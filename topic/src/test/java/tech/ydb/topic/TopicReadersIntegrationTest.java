package tech.ydb.topic;

import java.util.Arrays;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tech.ydb.core.Status;
import tech.ydb.test.junit4.GrpcTransportRule;
import tech.ydb.topic.description.Consumer;
import tech.ydb.topic.read.AsyncReader;
import tech.ydb.topic.read.Message;
import tech.ydb.topic.read.events.DataReceivedEvent;
import tech.ydb.topic.read.events.ReadEventHandler;
import tech.ydb.topic.read.events.StartPartitionSessionEvent;
import tech.ydb.topic.read.impl.AsyncReaderImpl;
import tech.ydb.topic.read.impl.ReaderImpl;
import tech.ydb.topic.settings.CommitOffsetSettings;
import tech.ydb.topic.settings.CreateTopicSettings;
import tech.ydb.topic.settings.PartitioningSettings;
import tech.ydb.topic.settings.ReadEventHandlersSettings;
import tech.ydb.topic.settings.ReaderSettings;
import tech.ydb.topic.settings.StartPartitionSessionSettings;
import tech.ydb.topic.settings.TopicReadSettings;
import tech.ydb.topic.settings.WriterSettings;
import tech.ydb.topic.utils.HideLoggers;
import tech.ydb.topic.utils.HideLoggersRule;
import tech.ydb.topic.write.SyncWriter;

/**
 *
 * @author Aleksandr Gorshenin
 */
public class TopicReadersIntegrationTest {
    private static final Logger logger = LoggerFactory.getLogger(YdbTopicsIntegrationTest.class);

    @ClassRule
    public final static GrpcTransportRule ydbTransport = new GrpcTransportRule();

    @Rule
    public final HideLoggersRule hideLogger = new HideLoggersRule();

    private final static String TEST_TOPIC = "topic_readers_test";

    private final static String TEST_CONSUMER1 = "consumer";

    private static TopicClient client;

    @BeforeClass
    public static void initClient() {
        client = TopicClient.newClient(ydbTransport).build();

        logger.info("Create test topic  {} ...", TEST_TOPIC);
        client.createTopic(TEST_TOPIC, CreateTopicSettings.newBuilder()
                .addConsumer(Consumer.newBuilder().setName(TEST_CONSUMER1).build())
                .setPartitioningSettings(PartitioningSettings.newBuilder()
                        .setMinActivePartitions(3)
                        .setMaxActivePartitions(3)
                        .build())
                .build()
        ).join().expectSuccess("can't create a new topic");

        CompletableFuture<Void> f1 = CompletableFuture.runAsync(() -> writeToTopic(0, 1000));
        CompletableFuture<Void> f2 = CompletableFuture.runAsync(() -> writeToTopic(1, 500));
        CompletableFuture<Void> f3 = CompletableFuture.runAsync(() -> writeToTopic(2, 2100));

        CompletableFuture.allOf(f1, f2, f3).join();
    }

    @AfterClass
    public static void closeClient() {
        logger.info("Drop test topic {} ...", TEST_TOPIC);
        client.dropTopic(TEST_TOPIC).join();
        client.close();
    }

    @Before
    public void resetConsumer() {
        CompletableFuture<Status> r1 = resetPartition(0);
        CompletableFuture<Status> r2 = resetPartition(1);
        CompletableFuture<Status> r3 = resetPartition(2);
        r1.join().expectSuccess();
        r2.join().expectSuccess();
        r3.join().expectSuccess();
    }

    private static CompletableFuture<Status> resetPartition(int partitionID) {
        return client.commitOffset(TEST_TOPIC, CommitOffsetSettings.newBuilder()
                .setConsumer(TEST_CONSUMER1)
                .setOffset(0)
                .setPartitionId(partitionID)
                .build());
    }

    private static void writeToTopic(int partitionID, int count) {
        WriterSettings settings = WriterSettings.newBuilder()
                .setTopicPath(TEST_TOPIC)
                .setProducerId("p" + partitionID)
                .setPartitionId(partitionID)
                .build();

        SyncWriter writer = client.createSyncWriter(settings);
        writer.initAndWait();
        for (int idx = 1; idx <= count; idx++) {
            byte[] msg = ("p" + partitionID + "_msg" + idx).getBytes();
            byte[] data = new byte[100];
            System.arraycopy(msg, 0, data, 0, msg.length);
            writer.send(tech.ydb.topic.write.Message.of(data));
        }

        try {
            writer.flush();
            writer.shutdown(10, TimeUnit.SECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException ex) {
            throw new AssertionError("cannot write", ex);
        }
    }

    @Test
    @HideLoggers({ ReaderImpl.class, AsyncReaderImpl.class })
    public void singleThreadExecutorTest() throws Exception {
        ReaderSettings readerSettings = ReaderSettings.newBuilder()
                .addTopic(TopicReadSettings.newBuilder()
                        .setPath(TEST_TOPIC)
                        .build())
                .setConsumerName(TEST_CONSUMER1)
                .build();

        CountDownLatch read = new CountDownLatch(1);
        CompletableFuture<Boolean> processing = new CompletableFuture<>();

        ExecutorService executor = Executors.newSingleThreadExecutor((r) -> new Thread(r, "test-executor"));
        AsyncReader reader = client.createAsyncReader(readerSettings, ReadEventHandlersSettings.newBuilder()
                .setExecutor(executor)
                .setEventHandler((event) -> {
                    read.countDown();
                    processing.join();
                }).build()
        );

        reader.init().join();

        // wait for message committing
        Assert.assertTrue(read.await(5, TimeUnit.SECONDS));

        // stop reader
        CompletableFuture<Void> shutdown = reader.shutdown();
        processing.completeExceptionally(new RuntimeException("shutdown"));
        shutdown.get(5, TimeUnit.SECONDS);

        executor.shutdownNow();
    }

    @Test
    public void readAllTest() throws InterruptedException {
        ReaderSettings readerSettings = ReaderSettings.newBuilder()
                .addTopic(TopicReadSettings.newBuilder().setPath(TEST_TOPIC).build())
                .setConsumerName(TEST_CONSUMER1)
                .build();

        AtomicLong[] offsets = new AtomicLong[] { new AtomicLong(), new AtomicLong(), new AtomicLong() };
        CountDownLatch read = new CountDownLatch(3600);
        AsyncReader reader = client.createAsyncReader(readerSettings, ReadEventHandlersSettings.newBuilder()
                .setEventHandler((DataReceivedEvent event) -> {
                    AtomicLong offset = offsets[(int) event.getPartitionSession().getPartitionId()];
                    for (Message msg : event.getMessages()) {
                        Assert.assertEquals(offset.getAndIncrement(), msg.getOffset());
                        read.countDown();
                    }
                }).build());

        reader.init().join();
        try {
            Assert.assertTrue(read.await(30, TimeUnit.SECONDS));
            Assert.assertEquals(1000, offsets[0].get());
            Assert.assertEquals(500, offsets[1].get());
            Assert.assertEquals(2100, offsets[2].get());
        } finally {
            reader.shutdown().join();
        }
    }

    @Test
    public void readAllByPartitionIdTest() throws InterruptedException {
        ReaderSettings readerSettings = ReaderSettings.newBuilder()
                .addTopic(TopicReadSettings.newBuilder()
                        .setPath(TEST_TOPIC)
                        .setPartitionIds(Arrays.asList(0L))
                        .build())
                .setConsumerName(TEST_CONSUMER1)
                .build();

        AtomicLong offset = new AtomicLong();
        CountDownLatch read = new CountDownLatch(1000);
        AsyncReader reader = client.createAsyncReader(readerSettings, ReadEventHandlersSettings.newBuilder()
                .setEventHandler((DataReceivedEvent event) -> {
            for (Message msg: event.getMessages()) {
                Assert.assertEquals(offset.getAndIncrement(), msg.getOffset());
                read.countDown();
            }
        }).build());

        reader.init().join();
        try {
            Assert.assertTrue(read.await(30, TimeUnit.SECONDS));
            Assert.assertEquals(1000, offset.get());
        } finally {
            reader.shutdown().join();
        }
    }

    @Test
    public void readAllWithoutConsumerTest() throws InterruptedException {
        ReaderSettings readerSettings = ReaderSettings.newBuilder()
                .addTopic(TopicReadSettings.newBuilder()
                        .setPath(TEST_TOPIC)
                        .setPartitionIds(Arrays.asList(0L))
                        .build())
                .withoutConsumer()
                .build();

        AtomicLong offset = new AtomicLong();
        CountDownLatch read = new CountDownLatch(1000);
        AsyncReader reader = client.createAsyncReader(readerSettings, ReadEventHandlersSettings.newBuilder()
                .setEventHandler((DataReceivedEvent event) -> {
            for (Message msg: event.getMessages()) {
                Assert.assertEquals(offset.getAndIncrement(), msg.getOffset());
                read.countDown();
            }
        }).build());


        reader.init().join();
        try {
            Assert.assertTrue(read.await(30, TimeUnit.SECONDS));
            Assert.assertEquals(1000, offset.get());
        } finally {
            reader.shutdown().join();
        }
    }

    @Test
    public void readFromTest() throws Exception {
        ReaderSettings readerSettings = ReaderSettings.newBuilder()
                .addTopic(TopicReadSettings.newBuilder()
                        .setPath(TEST_TOPIC)
                        .setPartitionIds(Arrays.asList(1L))
                        .build())
                .setConsumerName(TEST_CONSUMER1)
                .build();

        AtomicLong offset = new AtomicLong(123L);
        CountDownLatch read = new CountDownLatch(500-123);
        AsyncReader reader = client.createAsyncReader(readerSettings, ReadEventHandlersSettings.newBuilder()
                .setEventHandler(new ReadEventHandler() {
                    @Override
                    public void onStartPartitionSession(StartPartitionSessionEvent event) {
                        Assert.assertEquals(0, event.getCommittedOffset());
                        Assert.assertEquals(0, event.getPartitionOffsets().getStart());
                        Assert.assertEquals(500, event.getPartitionOffsets().getEnd());

                        // read only from offset 123
                        event.confirm(StartPartitionSessionSettings.newBuilder().setReadOffset(123L).build());
                    }

                    @Override
                    public void onMessages(DataReceivedEvent event) {
                        for (Message msg : event.getMessages()) {
                            Assert.assertEquals(offset.getAndIncrement(), msg.getOffset());
                            read.countDown();
                        }
                    }
                }).build());

        reader.init().join();
        try {
            Assert.assertTrue(read.await(30, TimeUnit.SECONDS));
            Assert.assertEquals(500, offset.get());
        } finally {
            reader.shutdown().join();
        }
    }

    @Test
    public void readFromWithCommitTest() throws Exception {
        ReaderSettings readerSettings = ReaderSettings.newBuilder()
                .addTopic(TopicReadSettings.newBuilder()
                        .setPath(TEST_TOPIC)
                        .setPartitionIds(Arrays.asList(1L))
                        .build())
                .setConsumerName(TEST_CONSUMER1)
                .build();

        AtomicLong offset = new AtomicLong(200L);
        CountDownLatch committed = new CountDownLatch(500-200);
        AsyncReader reader = client.createAsyncReader(readerSettings, ReadEventHandlersSettings.newBuilder()
                .setEventHandler(new ReadEventHandler() {
                    @Override
                    public void onStartPartitionSession(StartPartitionSessionEvent event) {
                        Assert.assertEquals(0, event.getCommittedOffset());
                        Assert.assertEquals(0, event.getPartitionOffsets().getStart());
                        Assert.assertEquals(500, event.getPartitionOffsets().getEnd());

                        // read only from offset 200
                        event.confirm(StartPartitionSessionSettings.newBuilder()
                                .setReadOffset(200L)
                                .setCommitOffset(200L)
                                .build());
                    }

                    @Override
                    public void onMessages(DataReceivedEvent event) {
                        for (Message msg: event.getMessages()) {
                            Assert.assertEquals(msg.getOffset(), offset.getAndIncrement());
                            msg.commit().whenComplete((r, th) -> {
                                Assert.assertNull(th);
                                committed.countDown();
                            });
                        }
                    }
                }).build());

        reader.init().join();
        try {
            Assert.assertTrue(committed.await(30, TimeUnit.SECONDS));
            Assert.assertEquals(500, offset.get());
        } finally {
            reader.shutdown().join();
        }
    }

    @Test
    public void readRetentionedTopicTest() throws Exception {
        ReaderSettings readerSettings = ReaderSettings.newBuilder()
                .addTopic(TopicReadSettings.newBuilder()
                        .setPath(TEST_TOPIC)
                        .setPartitionIds(Arrays.asList(1L))
                        .build())
                .setConsumerName(TEST_CONSUMER1)
                .build();

        AtomicLong offset = new AtomicLong(150L);
        CountDownLatch lastCommitted = new CountDownLatch(1);
        AsyncReader reader = client.createAsyncReader(readerSettings, ReadEventHandlersSettings.newBuilder()
                .setEventHandler(new ReadEventHandler() {
                    @Override
                    public void onStartPartitionSession(StartPartitionSessionEvent event) {
                        Assert.assertEquals(0, event.getCommittedOffset());
                        Assert.assertEquals(0, event.getPartitionOffsets().getStart());
                        Assert.assertEquals(500, event.getPartitionOffsets().getEnd());

                        // emulate topic retention - skip first 150 message but don't commit them
                        event.confirm(StartPartitionSessionSettings.newBuilder()
                                .setReadOffset(150L)
                                .build());
                    }

                    @Override
                    public void onMessages(DataReceivedEvent event) {
                        for (Message msg: event.getMessages()) {
                            Assert.assertEquals(msg.getOffset(), offset.getAndIncrement());
                        }

                        event.commit().whenComplete((r, th) -> {
                            Assert.assertNull(th);
                            if (event.getRangeToCommit().getEnd() >= 500) {
                                lastCommitted.countDown();
                            }
                        });
                    }
                }).build());

        reader.init().join();
        try {
            Assert.assertTrue(lastCommitted.await(30, TimeUnit.SECONDS));
            Assert.assertEquals(500, offset.get());
        } finally {
            reader.shutdown().join();
        }
    }

    @Test
    public void smallBufferTest() throws InterruptedException {
        ReaderSettings readerSettings = ReaderSettings.newBuilder()
                .addTopic(TopicReadSettings.newBuilder().setPath(TEST_TOPIC).build())
                .setConsumerName(TEST_CONSUMER1)
                .setMaxMemoryUsageBytes(1000)
                .build();

        AtomicLong[] offsets = new AtomicLong[] { new AtomicLong(), new AtomicLong(), new AtomicLong() };
        CountDownLatch read = new CountDownLatch(3600);
        AsyncReader reader = client.createAsyncReader(readerSettings, ReadEventHandlersSettings.newBuilder()
                .setEventHandler((DataReceivedEvent event) -> {
                    AtomicLong offset = offsets[(int) event.getPartitionSession().getPartitionId()];
                    for (Message msg : event.getMessages()) {
                        Assert.assertEquals(offset.getAndIncrement(), msg.getOffset());
                        read.countDown();
                    }
                }).build());

        reader.init().join();
        try {
            Assert.assertTrue(read.await(30, TimeUnit.SECONDS));
            Assert.assertEquals(1000, offsets[0].get());
            Assert.assertEquals(500, offsets[1].get());
            Assert.assertEquals(2100, offsets[2].get());
        } finally {
            reader.shutdown().join();
        }
    }

    @Test
    public void directDecompressorTest() throws InterruptedException {
        ReaderSettings readerSettings = ReaderSettings.newBuilder()
                .addTopic(TopicReadSettings.newBuilder().setPath(TEST_TOPIC).build())
                .setConsumerName(TEST_CONSUMER1)
                .setDecompressionExecutor(Runnable::run)
                .setMaxMemoryUsageBytes(1000)
                .build();

        AtomicLong[] offsets = new AtomicLong[] { new AtomicLong(), new AtomicLong(), new AtomicLong() };
        CountDownLatch read = new CountDownLatch(3600);
        AsyncReader reader = client.createAsyncReader(readerSettings, ReadEventHandlersSettings.newBuilder()
                .setExecutor(Runnable::run)
                .setEventHandler((DataReceivedEvent event) -> {
                    AtomicLong offset = offsets[(int) event.getPartitionSession().getPartitionId()];
                    for (Message msg : event.getMessages()) {
                        Assert.assertEquals(offset.getAndIncrement(), msg.getOffset());
                        read.countDown();
                    }
                }).build());

        reader.init().join();
        try {
            Assert.assertTrue(read.await(10, TimeUnit.MINUTES));
            Assert.assertEquals(1000, offsets[0].get());
            Assert.assertEquals(500, offsets[1].get());
            Assert.assertEquals(2100, offsets[2].get());
        } finally {
            reader.shutdown().join();
        }
    }
}
