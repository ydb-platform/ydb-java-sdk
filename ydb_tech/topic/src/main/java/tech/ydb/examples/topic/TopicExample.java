package tech.ydb.examples.topic;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import tech.ydb.common.transaction.TxMode;
import tech.ydb.core.grpc.GrpcTransport;
import tech.ydb.table.Session;
import tech.ydb.table.TableClient;
import tech.ydb.table.transaction.TableTransaction;
import tech.ydb.topic.TopicClient;
import tech.ydb.topic.description.Codec;
import tech.ydb.topic.description.Consumer;
import tech.ydb.topic.description.MetadataItem;
import tech.ydb.topic.description.TopicDescription;
import tech.ydb.topic.description.SupportedCodecs;
import tech.ydb.topic.read.AsyncReader;
import tech.ydb.topic.read.SyncReader;
import tech.ydb.topic.read.events.AbstractReadEventHandler;
import tech.ydb.topic.read.events.DataReceivedEvent;
import tech.ydb.topic.read.events.PartitionSessionClosedEvent;
import tech.ydb.topic.read.events.StartPartitionSessionEvent;
import tech.ydb.topic.read.events.StopPartitionSessionEvent;
import tech.ydb.topic.settings.AlterTopicSettings;
import tech.ydb.topic.settings.CommitOffsetSettings;
import tech.ydb.topic.settings.CreateTopicSettings;
import tech.ydb.topic.settings.PartitioningSettings;
import tech.ydb.topic.settings.ReadEventHandlersSettings;
import tech.ydb.topic.settings.ReaderSettings;
import tech.ydb.topic.settings.ReceiveSettings;
import tech.ydb.topic.settings.SendSettings;
import tech.ydb.topic.settings.StartPartitionSessionSettings;
import tech.ydb.topic.settings.TopicReadSettings;
import tech.ydb.topic.settings.UpdateOffsetsInTransactionSettings;
import tech.ydb.topic.settings.WriterSettings;
import tech.ydb.topic.write.AsyncWriter;
import tech.ydb.topic.write.Message;
import tech.ydb.topic.write.SyncWriter;
import tech.ydb.topic.write.WriteAck;

/** Executable source of the Java topic snippets on ydb.tech. */
public final class TopicExample {
    private static final Set<String> EXPECTED = new HashSet<>(Arrays.asList(
            "sync-one", "sync-two", "async-one", "acknowledged", "metadata-one", "metadata-two", "compressed"));
    private static final String[] CONSUMERS = {
            "one", "commit_one", "batch", "commit_batch", "offset", "selectors", "outside"
    };

    private TopicExample() { }

    public static void main(String[] args) throws Exception {
        String connectionString = System.getenv("YDB_CONNECTION_STRING");
        if (connectionString == null) {
            connectionString = "grpc://localhost:2136/local";
        }
        String topicPath = "ydb_tech_" + UUID.randomUUID().toString().replace("-", "");
        // [BEGIN topic_init]
        try (GrpcTransport transport = GrpcTransport.forConnectionString(connectionString).build();
                TopicClient topicClient = TopicClient.newClient(transport).build();
                TableClient tableClient = TableClient.newClient(transport).build()) {
            // [END topic_init]
            createTopic(topicClient, topicPath);
            try {
                // [BEGIN topic_alter]
                topicClient.alterTopic(topicPath, AlterTopicSettings.newBuilder()
                        .addAddConsumer(Consumer.newBuilder().setName("another-consumer").build())
                        .build()).get(30, TimeUnit.SECONDS).expectSuccess();
                // [END topic_alter]
                // [BEGIN topic_describe]
                TopicDescription description = topicClient.describeTopic(topicPath)
                        .get(30, TimeUnit.SECONDS).getValue();
                System.out.println("Consumers: " + description.getConsumers().size());
                // [END topic_describe]
                require(description.getConsumers().size() == CONSUMERS.length + 1, "Unexpected consumers");

                write(topicClient, topicPath);
                readSync(topicClient, topicPath, "one", false);
                readSync(topicClient, topicPath, "commit_one", true);
                readAsync(topicClient, topicPath, "batch", false, false);
                readAsync(topicClient, topicPath, "commit_batch", true, false);
                readAsync(topicClient, topicPath, "offset", false, true);
                readWithoutConsumer(topicClient, topicPath);
                readSelectors(topicClient, topicPath);
                commitOutside(topicClient, topicPath);
                transactions(topicClient, tableClient, topicPath + "_tx");
            } finally {
                // [BEGIN topic_drop]
                topicClient.dropTopic(topicPath).get(30, TimeUnit.SECONDS).expectSuccess();
                // [END topic_drop]
            }
        }
        System.out.println("All topic scenarios completed");
    }

    private static void createTopic(TopicClient topicClient, String topicPath) throws Exception {
        // [BEGIN topic_create]
        CreateTopicSettings.Builder settings = CreateTopicSettings.newBuilder()
                .setPartitioningSettings(PartitioningSettings.newBuilder()
                        .setMinActivePartitions(3).setMaxActivePartitions(3).build())
                .setSupportedCodecs(SupportedCodecs.newBuilder()
                        .addCodec(Codec.RAW).addCodec(Codec.GZIP).addCodec(Codec.ZSTD).build());
        for (String name : CONSUMERS) {
            settings.addConsumer(Consumer.newBuilder().setName(name).build());
        }
        topicClient.createTopic(topicPath, settings.build()).get(30, TimeUnit.SECONDS).expectSuccess();
        // [END topic_create]
    }

    private static WriterSettings writerSettings(String topicPath, String producerId) {
        // [BEGIN topic_writer_settings]
        return WriterSettings.newBuilder().setTopicPath(topicPath)
                .setProducerId(producerId).setPartitionId(0).build();
        // [END topic_writer_settings]
    }

    private static void write(TopicClient topicClient, String topicPath) throws Exception {
        WriterSettings settings = writerSettings(topicPath, "ydb-tech-sync");
        // [BEGIN topic_sync_writer]
        SyncWriter writer = topicClient.createSyncWriter(settings);
        // [END topic_sync_writer]
        try {
            // [BEGIN topic_sync_writer_init]
            writer.init();
            // [END topic_sync_writer_init]
            // [BEGIN topic_write_sync]
            writer.send(Message.of(bytes("sync-one")));
            writer.send(Message.newBuilder().setData(bytes("sync-two"))
                    .setCreateTimestamp(Instant.now().minusSeconds(5)).build(), 30, TimeUnit.SECONDS);
            writer.flush();
            // [END topic_write_sync]
        } finally {
            writer.shutdown(30, TimeUnit.SECONDS);
        }

        // [BEGIN topic_async_writer]
        AsyncWriter asyncWriter = topicClient.createAsyncWriter(writerSettings(topicPath, "ydb-tech-async"));
        asyncWriter.init().get(30, TimeUnit.SECONDS);
        // [END topic_async_writer]
        try {
            // [BEGIN topic_write_async]
            CompletableFuture<WriteAck> pending = asyncWriter.send(Message.of(bytes("async-one")));
            // [END topic_write_async]
            pending.get(30, TimeUnit.SECONDS);
            // [BEGIN topic_write_ack]
            asyncWriter.send(Message.of(bytes("acknowledged"))).thenAccept(ack -> {
                require(ack.getState() == WriteAck.State.WRITTEN, "The message was not written");
                require(ack.getDetails() != null, "Written acknowledgment has no offset");
                System.out.println("Acknowledged offset: " + ack.getDetails().getOffset());
            }).get(30, TimeUnit.SECONDS);
            // [END topic_write_ack]
            // [BEGIN topic_write_metadata]
            asyncWriter.send(Message.newBuilder().setData(bytes("metadata-one"))
                    .setMetadataItems(Arrays.asList(new MetadataItem("meta-key", bytes("meta-value"))))
                    .build()).get(30, TimeUnit.SECONDS);
            // [END topic_write_metadata]
            // [BEGIN topic_write_metadata_add]
            asyncWriter.send(Message.newBuilder().setData(bytes("metadata-two"))
                    .addMetadataItem(new MetadataItem("meta-key", bytes("meta-value")))
                    .build()).get(30, TimeUnit.SECONDS);
            // [END topic_write_metadata_add]
        } finally {
            asyncWriter.shutdown().get(30, TimeUnit.SECONDS);
        }

        // [BEGIN topic_codec]
        WriterSettings compressedSettings = WriterSettings.newBuilder().setTopicPath(topicPath)
                .setProducerId("ydb-tech-compressed").setPartitionId(0).setCodec(Codec.ZSTD).build();
        // [END topic_codec]
        AsyncWriter compressedWriter = topicClient.createAsyncWriter(compressedSettings);
        try {
            compressedWriter.init().get(30, TimeUnit.SECONDS);
            compressedWriter.send(Message.of(bytes("compressed"))).get(30, TimeUnit.SECONDS);
        } finally {
            compressedWriter.shutdown().get(30, TimeUnit.SECONDS);
        }
    }

    private static ReaderSettings readerSettings(String topicPath, String consumerName) {
        // [BEGIN topic_reader_settings]
        return ReaderSettings.newBuilder().setConsumerName(consumerName)
                .addTopic(TopicReadSettings.newBuilder().setPath(topicPath)
                        .setReadFrom(Instant.EPOCH).setMaxLag(Duration.ofHours(1)).build()).build();
        // [END topic_reader_settings]
    }

    private static void readSync(TopicClient topicClient, String topicPath, String consumer, boolean commit)
            throws Exception {
        ReaderSettings settings = readerSettings(topicPath, consumer);
        // [BEGIN topic_sync_reader]
        SyncReader reader = topicClient.createSyncReader(settings);
        // [END topic_sync_reader]
        try {
            // [BEGIN topic_sync_reader_init]
            reader.initAndWait();
            // [END topic_sync_reader_init]
            Set<String> received = new HashSet<>();
            while (received.size() < EXPECTED.size()) {
                // [BEGIN topic_read_one]
                tech.ydb.topic.read.Message message = reader.receive(30, TimeUnit.SECONDS);
                received.add(checkMessage(message));
                // [END topic_read_one]
                if (commit) {
                    // [BEGIN topic_read_commit]
                    message.commit().get(30, TimeUnit.SECONDS);
                    // [END topic_read_commit]
                }
            }
            require(received.equals(EXPECTED), "Unexpected topic payloads");
        } finally {
            reader.shutdown();
        }
    }

    private static void readAsync(TopicClient topicClient, String topicPath, String consumer,
            boolean commit, boolean ownOffsets) throws Exception {
        Handler handler = new Handler(commit, ownOffsets, EXPECTED);
        // [BEGIN topic_handler_settings]
        ReadEventHandlersSettings handlers = ReadEventHandlersSettings.newBuilder()
                .setEventHandler(handler).build();
        // [END topic_handler_settings]
        // [BEGIN topic_async_reader]
        AsyncReader reader = topicClient.createAsyncReader(readerSettings(topicPath, consumer), handlers);
        reader.init().get(30, TimeUnit.SECONDS);
        // [END topic_async_reader]
        try {
            handler.awaitMessages();
        } finally {
            reader.shutdown().get(30, TimeUnit.SECONDS);
        }
    }

    private static void readSelectors(TopicClient topicClient, String topicPath) throws Exception {
        String anotherTopic = topicPath + "_another";
        topicClient.createTopic(anotherTopic, CreateTopicSettings.newBuilder()
                .addConsumer(Consumer.newBuilder().setName("selectors").build()).build())
                .get(30, TimeUnit.SECONDS).expectSuccess();
        // [BEGIN topic_reader_selectors]
        ReaderSettings settings = ReaderSettings.newBuilder().setConsumerName("selectors")
                .addTopic(TopicReadSettings.newBuilder().setPath(topicPath).build())
                .addTopic(TopicReadSettings.newBuilder().setPath(anotherTopic)
                        .setReadFrom(Instant.EPOCH).setMaxLag(Duration.ofHours(1)).build()).build();
        // [END topic_reader_selectors]
        SyncReader reader = topicClient.createSyncReader(settings);
        try {
            // [BEGIN topic_sync_reader_background]
            reader.init();
            // [END topic_sync_reader_background]
            checkMessage(reader.receive(30, TimeUnit.SECONDS));
        } finally {
            reader.shutdown();
            topicClient.dropTopic(anotherTopic).get(30, TimeUnit.SECONDS).expectSuccess();
        }
    }

    private static void readWithoutConsumer(TopicClient topicClient, String topicPath) throws Exception {
        Handler handler = new Handler(false, true, EXPECTED) {
            // [BEGIN topic_no_consumer_offset]
            @Override
            public void onStartPartitionSession(StartPartitionSessionEvent event) {
                event.confirm(StartPartitionSessionSettings.newBuilder().setReadOffset(0L).build());
            }
            // [END topic_no_consumer_offset]
        };
        // [BEGIN topic_no_consumer]
        ReaderSettings settings = ReaderSettings.newBuilder().withoutConsumer()
                .addTopic(TopicReadSettings.newBuilder().setPath(topicPath)
                        .setPartitionIds(Arrays.asList(0L, 1L, 2L)).build())
                .setErrorsHandler((status, error) -> handler.fail(error != null ? error
                        : new IllegalStateException("Reader failed: " + status))).build();
        // [END topic_no_consumer]
        AsyncReader reader = topicClient.createAsyncReader(settings,
                ReadEventHandlersSettings.newBuilder().setEventHandler(handler).build());
        try {
            reader.init().get(30, TimeUnit.SECONDS);
            handler.awaitMessages();
        } finally {
            reader.shutdown().get(30, TimeUnit.SECONDS);
        }
    }

    private static void commitOutside(TopicClient topicClient, String topicPath) throws Exception {
        SyncReader reader = topicClient.createSyncReader(readerSettings(topicPath, "outside"));
        try {
            reader.initAndWait();
            tech.ydb.topic.read.Message message = reader.receive(30, TimeUnit.SECONDS);
            checkMessage(message);
            // [BEGIN topic_commit_outside]
            topicClient.commitOffset(topicPath, CommitOffsetSettings.newBuilder()
                    .setReadSessionId(reader.getSessionId()).setPartitionId(0)
                    .setConsumer("outside").setOffset(message.getOffset() + 1).build())
                    .get(30, TimeUnit.SECONDS).expectSuccess();
            // [END topic_commit_outside]
        } finally {
            reader.shutdown();
        }
    }

    private static void transactions(TopicClient topicClient, TableClient tableClient, String topicPath)
            throws Exception {
        topicClient.createTopic(topicPath, CreateTopicSettings.newBuilder()
                .addConsumer(Consumer.newBuilder().setName("sync").build())
                .addConsumer(Consumer.newBuilder().setName("async").build()).build())
                .get(30, TimeUnit.SECONDS).expectSuccess();
        try {
            SyncWriter writer = topicClient.createSyncWriter(writerSettings(topicPath, "ydb-tech-tx-sync"));
            try (Session session = tableClient.createSession(Duration.ofSeconds(30)).get(30, TimeUnit.SECONDS).getValue()) {
                writer.init();
                // [BEGIN topic_write_tx_sync]
                TableTransaction transaction = session.createNewTransaction(TxMode.SERIALIZABLE_RW);
                transaction.executeDataQuery("SELECT 1").get(30, TimeUnit.SECONDS).getValue();
                writer.send(Message.of(bytes("tx-sync")), SendSettings.newBuilder().setTransaction(transaction).build());
                writer.flush();
                transaction.commit().get(30, TimeUnit.SECONDS).expectSuccess();
                // [END topic_write_tx_sync]
            } finally {
                writer.shutdown(30, TimeUnit.SECONDS);
            }

            AsyncWriter asyncWriter = topicClient.createAsyncWriter(writerSettings(topicPath, "ydb-tech-tx-async"));
            try (Session session = tableClient.createSession(Duration.ofSeconds(30)).get(30, TimeUnit.SECONDS).getValue()) {
                asyncWriter.init().get(30, TimeUnit.SECONDS);
                // [BEGIN topic_write_tx_async]
                TableTransaction transaction = session.createNewTransaction(TxMode.SERIALIZABLE_RW);
                transaction.executeDataQuery("SELECT 1").get(30, TimeUnit.SECONDS).getValue();
                asyncWriter.send(Message.of(bytes("tx-async")), SendSettings.newBuilder().setTransaction(transaction).build())
                        .get(30, TimeUnit.SECONDS);
                transaction.commit().get(30, TimeUnit.SECONDS).expectSuccess();
                // [END topic_write_tx_async]
            } finally {
                asyncWriter.shutdown().get(30, TimeUnit.SECONDS);
            }

            SyncReader reader = topicClient.createSyncReader(readerSettings(topicPath, "sync"));
            Set<String> received = new HashSet<>();
            try {
                reader.initAndWait();
                for (int index = 0; index < 2; index++) {
                    try (Session session = tableClient.createSession(Duration.ofSeconds(30)).get(30, TimeUnit.SECONDS).getValue()) {
                        TableTransaction transaction = session.createNewTransaction(TxMode.SERIALIZABLE_RW);
                        transaction.executeDataQuery("SELECT 1").get(30, TimeUnit.SECONDS).getValue();
                        // [BEGIN topic_read_tx_sync]
                        tech.ydb.topic.read.Message message = reader.receive(ReceiveSettings.newBuilder()
                                .setTransaction(transaction).setTimeout(30, TimeUnit.SECONDS).build());
                        received.add(new String(message.getData(), StandardCharsets.UTF_8));
                        transaction.commit().get(30, TimeUnit.SECONDS).expectSuccess();
                        // [END topic_read_tx_sync]
                    }
                }
                require(received.equals(new HashSet<>(Arrays.asList("tx-sync", "tx-async"))), "Unexpected transactional messages");
            } finally {
                reader.shutdown();
            }

            TransactionHandler handler = new TransactionHandler(tableClient);
            AsyncReader asyncReader = topicClient.createAsyncReader(readerSettings(topicPath, "async"),
                    ReadEventHandlersSettings.newBuilder().setEventHandler(handler).build());
            handler.reader = asyncReader;
            try {
                asyncReader.init().get(30, TimeUnit.SECONDS);
                require(handler.done.await(30, TimeUnit.SECONDS), "Timed out waiting for transactional messages");
                if (handler.failure.get() != null) {
                    throw new IllegalStateException("Transactional handler failed", handler.failure.get());
                }
            } finally {
                asyncReader.shutdown().get(30, TimeUnit.SECONDS);
            }
        } finally {
            topicClient.dropTopic(topicPath).get(30, TimeUnit.SECONDS).expectSuccess();
        }
    }

    private static String checkMessage(tech.ydb.topic.read.Message message) {
        require(message != null, "Timed out waiting for a message");
        String payload = new String(message.getData(), StandardCharsets.UTF_8);
        require(EXPECTED.contains(payload), "Unexpected payload: " + payload);
        if (payload.startsWith("metadata")) {
            // [BEGIN topic_read_metadata]
            for (MetadataItem item : message.getMetadataItems()) {
                System.out.println(item.getKey() + ": " + new String(item.getValue(), StandardCharsets.UTF_8));
            }
            // [END topic_read_metadata]
            require(message.getMetadataItems().stream().anyMatch(item -> item.getKey().equals("meta-key")
                    && Arrays.equals(item.getValue(), bytes("meta-value"))), "Unexpected metadata");
        }
        return payload;
    }

    private static byte[] bytes(String value) {
        return value.getBytes(StandardCharsets.UTF_8);
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new IllegalStateException(message);
        }
    }

    private static class Handler extends AbstractReadEventHandler {
        private final boolean commit;
        private final boolean ownOffsets;
        private final Set<String> expected;
        private final Set<String> received = ConcurrentHashMap.newKeySet();
        private final ConcurrentMap<Long, Long> offsets = new ConcurrentHashMap<>();
        private final AtomicReference<Throwable> failure = new AtomicReference<>();
        private final CountDownLatch done;

        private Handler(boolean commit, boolean ownOffsets, Set<String> expected) {
            this.commit = commit;
            this.ownOffsets = ownOffsets;
            this.expected = expected;
            this.done = new CountDownLatch(expected.size());
        }

        // [BEGIN topic_read_batch]
        @Override
        public void onMessages(DataReceivedEvent event) {
            try {
                for (tech.ydb.topic.read.Message message : event.getMessages()) {
                    String payload = checkMessage(message);
                    offsets.put(event.getPartitionSession().getPartitionId(), message.getOffset() + 1);
                    received.add(payload);
                    if (!commit) {
                        done.countDown();
                    }
                }
                if (commit) {
                    commitBatch(event);
                }
            } catch (Exception error) {
                failure.set(error);
                while (done.getCount() > 0) {
                    done.countDown();
                }
            }
        }
        // [END topic_read_batch]

        // [BEGIN topic_read_batch_commit]
        private void commitBatch(DataReceivedEvent event) throws Exception {
            event.commit().get(30, TimeUnit.SECONDS);
            for (int index = 0; index < event.getMessages().size(); index++) {
                done.countDown();
            }
        }
        // [END topic_read_batch_commit]

        // [BEGIN topic_client_offset]
        @Override
        public void onStartPartitionSession(StartPartitionSessionEvent event) {
            if (ownOffsets) {
                long offset = offsets.getOrDefault(event.getPartitionSession().getPartitionId(), 0L);
                event.confirm(StartPartitionSessionSettings.newBuilder()
                        .setReadOffset(offset).setCommitOffset(offset).build());
            } else {
                event.confirm();
            }
        }
        // [END topic_client_offset]

        // [BEGIN topic_soft_stop]
        @Override
        public void onStopPartitionSession(StopPartitionSessionEvent event) {
            System.out.println("Stopped partition session: " + event.getPartitionSessionId());
            event.confirm();
        }
        // [END topic_soft_stop]

        // [BEGIN topic_hard_stop]
        @Override
        public void onPartitionSessionClosed(PartitionSessionClosedEvent event) {
            System.out.println("Closed partition: " + event.getPartitionSession().getPartitionId());
        }
        // [END topic_hard_stop]

        private void awaitMessages() throws Exception {
            require(done.await(30, TimeUnit.SECONDS), "Timed out waiting for asynchronous messages");
            if (failure.get() != null) {
                throw new IllegalStateException("Reader handler failed", failure.get());
            }
            require(received.equals(expected), "Unexpected asynchronous payloads");
        }

        private void fail(Throwable error) {
            failure.set(error);
            while (done.getCount() > 0) {
                done.countDown();
            }
        }
    }

    private static final class TransactionHandler extends AbstractReadEventHandler {
        private final TableClient tableClient;
        private final CountDownLatch done = new CountDownLatch(2);
        private final AtomicReference<Throwable> failure = new AtomicReference<>();
        private AsyncReader reader;

        private TransactionHandler(TableClient tableClient) {
            this.tableClient = tableClient;
        }

        // [BEGIN topic_read_tx_async]
        @Override
        public void onMessages(DataReceivedEvent event) {
            try {
                for (tech.ydb.topic.read.Message message : event.getMessages()) {
                    try (Session session = tableClient.createSession(Duration.ofSeconds(30))
                            .get(30, TimeUnit.SECONDS).getValue()) {
                        TableTransaction transaction = session.createNewTransaction(TxMode.SERIALIZABLE_RW);
                        transaction.executeDataQuery("SELECT 1").get(30, TimeUnit.SECONDS).getValue();
                        reader.updateOffsetsInTransaction(transaction, message.getPartitionOffsets(),
                                UpdateOffsetsInTransactionSettings.newBuilder().build())
                                .get(30, TimeUnit.SECONDS).expectSuccess();
                        transaction.commit().get(30, TimeUnit.SECONDS).expectSuccess();
                        require(new String(message.getData(), StandardCharsets.UTF_8).startsWith("tx-"),
                                "Unexpected transactional payload");
                        done.countDown();
                    }
                }
            } catch (Exception error) {
                failure.set(error);
                while (done.getCount() > 0) {
                    done.countDown();
                }
            }
        }
        // [END topic_read_tx_async]
    }
}
