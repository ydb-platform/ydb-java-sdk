package tech.ydb.topic.examples;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.UUID;

import tech.ydb.common.transaction.TxMode;
import tech.ydb.core.Status;
import tech.ydb.core.StatusCode;
import tech.ydb.core.UnexpectedResultException;
import tech.ydb.core.grpc.GrpcTransport;
import tech.ydb.table.Session;
import tech.ydb.table.TableClient;
import tech.ydb.table.transaction.TableTransaction;
import tech.ydb.topic.TopicClient;
import tech.ydb.topic.description.Consumer;
import tech.ydb.topic.settings.CreateTopicSettings;
import tech.ydb.topic.settings.MultiWriterSettings;
import tech.ydb.topic.settings.PartitioningSettings;
import tech.ydb.topic.write.MultiMessage;
import tech.ydb.topic.write.TransactionalMultiWriter;

/** Run against a local, unauthenticated YDB: TransactionalMultiWriterExample localhost:2136 /local. */
public final class TransactionalMultiWriterExample {
    private static final int MAX_ATTEMPTS = 3;

    private TransactionalMultiWriterExample() { }

    public static void main(String[] args) throws Exception {
        if (args.length != 2) {
            throw new IllegalArgumentException("Usage: TransactionalMultiWriterExample <host:port> <database>");
        }
        String name = "multiwriter-tx-example-" + UUID.randomUUID();
        String path = args[1] + (args[1].endsWith("/") ? "" : "/") + name;
        try (GrpcTransport transport = GrpcTransport.forEndpoint(args[0], args[1]).build();
                TopicClient topics = TopicClient.newClient(transport).build();
                TableClient tables = TableClient.newClient(transport).build()) {
            topics.createTopic(path, CreateTopicSettings.newBuilder()
                    .addConsumer(Consumer.newBuilder().setName("example-consumer").build())
                    .setPartitioningSettings(PartitioningSettings.newBuilder()
                            .setMinActivePartitions(2).setMaxActivePartitions(2).build())
                    .build()).join().expectSuccess("Cannot create the example topic");
            boolean completed = false;
            try {
                commitWithRetry(topics, tables, path, name);
                writeAttempt(topics, tables, path, name + "-rollback", "rolled-back-operation", false)
                        .expectSuccess("Rollback request failed");
                System.out.println("Committed one whole operation; rolled back a second operation: " + path);
                completed = true;
            } finally {
                if (completed) {
                    topics.dropTopic(path).join().expectSuccess("Cannot drop the example topic");
                } else {
                    // Preserve evidence if a commit response is missing or any outcome needs investigation.
                    System.err.println("Example topic retained for outcome inspection: " + path);
                }
            }
        }
    }

    private static void commitWithRetry(TopicClient topics, TableClient tables, String path, String prefix)
            throws Exception {
        String businessId = UUID.randomUUID().toString();
        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            // A fresh transaction and facade repeat both messages of the business operation.
            Status status = writeAttempt(topics, tables, path, prefix + "-attempt-" + attempt, businessId, true);
            if (status.isSuccess()) {
                return;
            }
            // Retry only a definitive transaction ABORTED, never a lost/unknown commit response.
            if (status.getCode() != StatusCode.ABORTED || attempt == MAX_ATTEMPTS) {
                status.expectSuccess("Transaction did not commit");
            }
            System.out.println("Transaction ABORTED; retrying the entire operation, attempt " + (attempt + 1));
        }
    }

    private static Status writeAttempt(TopicClient topics, TableClient tables, String path,
            String prefix, String businessId, boolean commit) throws Exception {
        try (Session session = tables.createSession(Duration.ofSeconds(5)).join().getValue()) {
            TableTransaction tx = session.beginTransaction(TxMode.SERIALIZABLE_RW).join().getValue();
            TransactionalMultiWriter writer = topics.createTransactionalMultiWriter(MultiWriterSettings.newBuilder()
                    .setTopicPath(path).setProducerPrefix(prefix).build(), tx);
            Exception primary = null;
            boolean commitStarted = false;
            boolean abortedOrRolledBack = false;
            try {
                writer.init().join();
                // A real business operation would also execute its table queries in this same tx.
                writer.send(message("account-1", businessId + ":debit"));
                writer.send(message("account-2", businessId + ":credit"));
                writer.prepareCommit().join(); // Staged, not yet published. Admission is now closed.
                if (!commit) {
                    Status rollback = tx.rollback().join();
                    rollback.expectSuccess("Cannot roll back the transaction");
                    abortedOrRolledBack = true;
                    return rollback;
                }
                commitStarted = true;
                Status status = tx.commit().join();
                abortedOrRolledBack = status.getCode() == StatusCode.ABORTED;
                if (!status.isSuccess() && !abortedOrRolledBack) {
                    status.expectSuccess("Commit failed; resolve its outcome before any business retry");
                }
                return status;
            } catch (Exception error) {
                primary = error;
                if (!commitStarted) {
                    try {
                        tx.rollback().join().expectSuccess("Cannot roll back after staging failure");
                        abortedOrRolledBack = true;
                    } catch (RuntimeException rollbackError) {
                        error.addSuppressed(rollbackError);
                    }
                }
                // Once commit started, this code neither assumes rollback nor starts a new business attempt.
                throw error;
            } finally {
                closeWriter(writer, primary, abortedOrRolledBack);
            }
        }
    }

    private static void closeWriter(TransactionalMultiWriter writer, Exception primary, boolean aborted) {
        try {
            writer.shutdown().join();
        } catch (RuntimeException cleanupError) {
            if (aborted && hasAbortedStatus(cleanupError)) {
                return; // A transaction facade reports the known rollback/ABORTED as its terminal outcome.
            }
            if (primary != null) {
                primary.addSuppressed(cleanupError);
            } else {
                throw cleanupError;
            }
        }
    }

    private static boolean hasAbortedStatus(Throwable error) {
        for (Throwable cause = error; cause != null; cause = cause.getCause()) {
            if (cause instanceof UnexpectedResultException
                    && ((UnexpectedResultException) cause).getStatus().getCode() == StatusCode.ABORTED) {
                return true;
            }
        }
        return false;
    }

    private static MultiMessage message(String key, String payload) {
        return MultiMessage.of(payload.getBytes(StandardCharsets.UTF_8), key);
    }
}
