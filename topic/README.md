# Topic MultiWriter

`AsyncMultiWriter` routes accepted messages across topic partitions and preserves admission order for each key. It uses the existing public `AsyncWriter` for each partition, including its compression, transport and transient reconnect logic. `SyncMultiWriter` adapts the same implementation. `TransactionalMultiWriter` binds it to one already active Table or Query transaction.

## Ordinary writes

Create a writer with `TopicClient.createAsyncMultiWriter(settings)`, await `init()`, then submit `MultiMessage` values. `send()` admits the message synchronously and returns a future for its logical acknowledgement. Several messages may be in flight at once. `flush()` captures the current admission watermark; subsequent writes do not extend that wait. `shutdown()` stops admission, drains accepted messages within its deadline, and closes the writer's resources.

```java
MultiWriterSettings settings = MultiWriterSettings.newBuilder()
        .setTopicPath("/local/events")
        .setProducerPrefix("service-instance-1")
        .build();
AsyncMultiWriter writer = topicClient.createAsyncMultiWriter(settings);
writer.init().join();
writer.send(MultiMessage.of(firstPayload, "account-1"));
writer.send(MultiMessage.of(secondPayload, "account-2"));
writer.flush().join();
writer.shutdown().join();
```

Use `try/finally` or equivalent cleanup in application code; the runnable examples below include cleanup. `SyncMultiWriter.send()` also returns after admission, not after an ACK. Its buffer rejection is immediate; it does not wait for space as `SyncWriter.send()` does.

Sequential messages with the same key preserve their admission order within one writer. Concurrent calls are ordered by admission. This does not establish order across different writers, independent transactions, or a mixture of ordinary and transactional writes.

An SDK retry keeps the accepted message's sequence number, timestamp, key and payload. A new `send()` with identical key and bytes is a new message when automatic sequence numbers are used. The key selects a partition; it is not a business-operation idempotency key. Explicit sequence numbers must be positive and strictly increase within the writer, including across partitions.

`MultiMessage` copies bytes and metadata. The reserved metadata key `__partition_key` cannot be supplied by the application. In bound mode the writer adds the exact hash bytes used for routing. `MultiWriteResult` identifies its logical sequence number and whether confirmation came from an ACK or producer history. Partition and offset are nullable: a cumulative ACK's offset cannot be assigned to another message, and ancestor history does not identify a physical offset.

## Initialization, routing and split

Initialization describes the complete partition graph, obtains producer history with bounded concurrency, and confirms a consistent topology snapshot. The first automatic sequence number exceeds the maximum history of the producer family. Producer IDs are `prefix-partitionId`. Only one live writer may own a prefix for a topic. Reusing that prefix after restart recovers sequence history; it does not recover payload that existed only in process memory.

| Mode | Routing and topology contract |
| --- | --- |
| `AUTO` | Selects `BOUND` when key ranges are present, otherwise `HASH`; selection stays fixed for the writer's lifetime. |
| `BOUND` | Requires complete, non-overlapping key ranges. Uses UTF-8, Murmur2Hash64A with seed 0, and unsigned big-endian hash bytes, matching the Go reference. |
| `HASH` | Uses Kafka-compatible Murmur2Hash32 and sorted partition IDs. The active partition set must remain fixed; a changed set is an error. |
| `EXPLICIT` | Requires `MultiMessage.Builder.setPartitionId()`. An inactive or unknown partition is an error; the writer cannot infer which child should receive an explicit parent's messages. |

For keyed routing an empty key falls back to the producer prefix. Ordinary split recovery stops submissions to the old partition, confirms its inactive state, reads relevant producer histories and reroutes the unresolved tail with unchanged sequence numbers. That tail precedes newer messages of its lineage. Transient transport reconnect remains the lower writer's responsibility and does not trigger a competing upper-layer resend.

`OVERLOADED` alone does not prove a split. Incomplete topology is refreshed within the recovery deadline. Half-close and the completion of `AsyncWriter.shutdown()` do not by themselves prove a final server-side history boundary. Tests that suppress a returned ACK establish a lost-response scenario; they do not establish every possible race between server processing and a history probe. This implementation does not claim an unqualified end-to-end exactly-once guarantee across producer migration.

## Transactions

The transaction must already be active when passed to `createTransactionalMultiWriter(settings, tx)`. The facade captures its transaction/session IDs and status future, preventing a queued message from silently using a reused transaction object.

```java
TransactionalMultiWriter writer = topicClient.createTransactionalMultiWriter(settings, tx);
writer.init().join();
writer.send(MultiMessage.of(debit, "account-1"));
writer.send(MultiMessage.of(credit, "account-2"));
writer.prepareCommit().join();
tx.commit().join().expectSuccess();
writer.shutdown().join();
```

Awaiting a send future or `flush()` confirms staging, not commit or reader visibility. A successful transaction publishes its result atomically; rollback does not publish its messages. An identical same-transaction retry can receive `ALREADY_WRITTEN` instead of `WRITTEN_IN_TX`; neither response is a commit result.

`prepareCommit()` closes admission atomically and waits for staging of every accepted message. A racing send is either admitted before the barrier and included, or rejected. Repeated preparation has the same logical result. Calling the external transaction's `commit()` directly does **not** automatically wait for this writer. The caller must preserve the explicit `prepareCommit() → commit()` order.

For a split, ordinary producer history is not treated as proof of staging in the current transaction. Unresolved messages are retried within the same captured transaction after the parent becomes inactive; the server may still reject or abort the transaction. If the facade fails, the caller must roll back or resolve the transaction's outcome. Closing a writer is not rollback.

After a definitive transaction `ABORTED`, the application or an existing transaction retry mechanism can repeat the entire business operation with a new transaction and facade. The writer never transfers only an unresolved tail into a new transaction. A missing or failed commit response can leave the outcome unknown; it must not be interpreted as rollback or trigger an automatic business retry. Applications requiring T1 before T2 must await successful commit of T1 before committing T2. MultiWriter does not coordinate independent commits or deduplicate new business operations.

## Observed server scenarios

The live parity tests use YDB **25.1.4.7**, image digest
`sha256:c07a02b4018100835e535ee842e9a789566bafee12cc5e5d75c882607149be2c`, and the Go reference
`229af32f876ca3e96ab962436cd670cb2e174872`. Both SDKs produced these outcomes:

| Fault before transaction completion | Commit result | Visible A/B |
| --- | --- | --- |
| Lost staging ACK, without split | Success after same-identity retry | 1/1 |
| Split before A reached staging; B already staged in another partition | Success after recovery in the same transaction | 1/1 |
| Split after staging, with or without its ACK reaching the SDK | `ABORTED` | 0/0 |
| Lost successful commit response | Transport error observed by the caller | 1/1 |

Rollback produced 0/0 in each tested split/ACK variant. An application retry of the entire operation after a definitive
`ABORTED` produced 1/1. These are observations on the pinned server, not a claim that a staged transaction survives every
split. The tests do not control delayed server persistence of an already admitted request relative to a history probe.

## Limits and resource ownership

| Setting | Default | Meaning |
| --- | --- | --- |
| `bufferBytes`, `bufferMessages` | 64 MiB, 10,000 | Shared accepted work, including payload and conservative encoding reservations. |
| `maxMessageBytes` | 1 MiB | Maximum unencoded payload; its codec bound must also fit a partition buffer. |
| `writerBufferBytes`, `writerBufferMessages` | 8 MiB, 1,024 | Limits for each underlying writer; capacity is reserved before calling its public `send()`. |
| `maxWriters`, `maxConcurrentInits` | 32, 4 | Limits on live lower writers/probes and concurrent initialization. |
| `ioThreads` | 4 | Shared workers for lower-writer calls; no permanent thread per partition. |
| `initTimeout`, `recoveryTimeout`, `closeTimeout` | 30 seconds each | Initialization, recovery and close deadlines. A cascade does not restart an accepted message's recovery deadline. |
| `idleTimeout` | 60 seconds | Idle lower-writer eviction. Pending history probes may reclaim idle capacity earlier. |
| `deliveryTimeout` | Disabled (`Duration.ZERO`) | Optional deadline after admission. Expiration leaves server persistence unknown. |
| `codec`, `directWrite` | RAW, `false` | Registered codec and direct-write mode for lower writers. |

The shared budget uses a completed-prefix sweep: a late acknowledged message retains its budget reservation behind an earlier unfinished message, even when its payload can be released. Slow application callbacks also retain their admission reservation until they return. Capacity rejection occurs before admission and does not allocate a new sequence number for a retry of that rejected call.

The byte budget does not claim to bound every JVM, protobuf or gRPC allocation. The codec selected at writer creation is captured in its own registry snapshot. Custom codecs must supply a truthful conservative `getMaxEncodedSize()` bound; a codec that underestimates its output invalidates the corresponding memory estimate. Lower buffers stay enabled. An unexpected lower admission overflow is terminal because repeating the same sequence number on that lower writer is not safe.

Message and control futures complete outside the coordinator on separate ForkJoin pools. Accepted work and barrier limits bound outstanding completion jobs; managed blocking supports callbacks that join writer futures. Keep callbacks short and use an application executor for unrelated blocking work. This is not a fixed upper bound on compensation threads created for blocked callbacks.

Cancellation of a returned future cancels that wait, not an already accepted message. A delivery or close timeout does not prove that the server did not write it, or that a stuck lower call/compression task physically released all resources. The writer shuts down its own executors, writers and probes; it does not close the shared TopicClient, transport, Table/Query client, transaction or session.

## Runnable examples

Both examples are Java 8 source and create a uniquely named, two-partition topic with an example consumer. They use an unauthenticated local endpoint. Configure transport credentials and TLS separately for a secured deployment.

- [MultiWriterExample](src/test/java/tech/ydb/topic/examples/MultiWriterExample.java) pipelines two keys, waits for `flush()`, prints results and removes its topic.
- [TransactionalMultiWriterExample](src/test/java/tech/ydb/topic/examples/TransactionalMultiWriterExample.java) explicitly prepares and commits one operation, rolls back another, and retries the whole operation at most three times on definitive `ABORTED`. It retains the topic for inspection if an outcome is unresolved.

From the repository root, build/install reactor dependencies, then build the test classpath:

```sh
mvn -pl bom,topic -am -DskipTests install
mvn -f topic/pom.xml -DskipTests test-compile dependency:build-classpath \
  -DincludeScope=test -Dmdep.outputFile=target/example-classpath.txt
java -cp "topic/target/test-classes:topic/target/classes:$(cat topic/target/example-classpath.txt)" \
  tech.ydb.topic.examples.MultiWriterExample localhost:2136 /local
java -cp "topic/target/test-classes:topic/target/classes:$(cat topic/target/example-classpath.txt)" \
  tech.ydb.topic.examples.TransactionalMultiWriterExample localhost:2136 /local
```

`-DskipTests` here builds the examples without running the test suite; it is not a test result. The first command installs the parent, BOM and module dependencies required by the subsequent single-module command. On Windows use the platform's classpath separator and shell syntax.
