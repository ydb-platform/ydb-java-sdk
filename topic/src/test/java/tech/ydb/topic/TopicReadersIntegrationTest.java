package tech.ydb.topic;

import java.util.Arrays;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.Timeout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tech.ydb.test.junit4.GrpcTransportRule;
import tech.ydb.topic.description.Consumer;
import tech.ydb.topic.read.AsyncReader;
import tech.ydb.topic.read.Message;
import tech.ydb.topic.read.events.DataReceivedEvent;
import tech.ydb.topic.read.events.ReadEventHandler;
import tech.ydb.topic.read.events.StartPartitionSessionEvent;
import tech.ydb.topic.read.impl.AsyncReaderImpl;
import tech.ydb.topic.read.impl.ReaderImpl;
import tech.ydb.topic.settings.CreateTopicSettings;
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
    public final Timeout timeout = new Timeout(10, TimeUnit.SECONDS);

    @Rule
    public final HideLoggersRule hideLogger = new HideLoggersRule();

    private final static String TEST_TOPIC = "topic_readers_test";

    private final static String TEST_CONSUMER1 = "consumer";

    private static TopicClient client;

    @BeforeClass
    public static void initClient() {
        client = TopicClient.newClient(ydbTransport).build();
    }

    @AfterClass
    public static void closeClient() {
        client.close();
    }

    @Before
    public void initTopic() throws Exception {
        logger.info("Create test topic  {} ...", TEST_TOPIC);
        client.createTopic(TEST_TOPIC, CreateTopicSettings.newBuilder()
                .addConsumer(Consumer.newBuilder().setName(TEST_CONSUMER1).build())
                .build()
        ).join().expectSuccess("can't create a new topic");

        // send 6 messages with offsets 0-5
        String[] messages = new String[] { "test1", "test2", "test3", "test4", "test5", "stop" };

        WriterSettings settings = WriterSettings.newBuilder()
                .setTopicPath(TEST_TOPIC)
                .setProducerId("helper")
                .build();
        SyncWriter writer = client.createSyncWriter(settings);
        writer.initAndWait();
        for (String message : messages) {
            writer.send(tech.ydb.topic.write.Message.of(message.getBytes()));
        }

        writer.flush();
        writer.shutdown(10, TimeUnit.SECONDS);
    }

    @After
    public void dropTopic() {
        logger.info("Drop test topic {} ...", TEST_TOPIC);
        client.dropTopic(TEST_TOPIC).join().expectSuccess("can't drop test topic");
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

        AtomicLong offset = new AtomicLong();
        CountDownLatch read = new CountDownLatch(6);
        AsyncReader reader = client.createAsyncReader(readerSettings, ReadEventHandlersSettings.newBuilder()
                .setEventHandler((DataReceivedEvent event) -> {
            for (Message msg: event.getMessages()) {
                Assert.assertEquals(offset.getAndIncrement(), msg.getOffset());
                read.countDown();
            }
        }).build());

        reader.init().join();
        Assert.assertTrue(read.await(5, TimeUnit.SECONDS));
        Assert.assertEquals(6, offset.get());
        reader.shutdown().join();
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
        CountDownLatch read = new CountDownLatch(6);
        AsyncReader reader = client.createAsyncReader(readerSettings, ReadEventHandlersSettings.newBuilder()
                .setEventHandler((DataReceivedEvent event) -> {
            for (Message msg: event.getMessages()) {
                Assert.assertEquals(offset.getAndIncrement(), msg.getOffset());
                read.countDown();
            }
        }).build());

        reader.init().join();
        Assert.assertTrue(read.await(5, TimeUnit.SECONDS));
        Assert.assertEquals(6, offset.get());
        reader.shutdown().join();
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
        CountDownLatch read = new CountDownLatch(6);
        AsyncReader reader = client.createAsyncReader(readerSettings, ReadEventHandlersSettings.newBuilder()
                .setEventHandler((DataReceivedEvent event) -> {
            for (Message msg: event.getMessages()) {
                Assert.assertEquals(offset.getAndIncrement(), msg.getOffset());
                read.countDown();
            }
        }).build());

        reader.init().join();
        Assert.assertTrue(read.await(5, TimeUnit.SECONDS));
        Assert.assertEquals(6, offset.get());
        reader.shutdown().join();
    }

    @Test
    public void readFromTest() throws Exception {
        ReaderSettings readerSettings = ReaderSettings.newBuilder()
                .addTopic(TopicReadSettings.newBuilder().setPath(TEST_TOPIC).build())
                .setConsumerName(TEST_CONSUMER1)
                .build();

        AtomicLong counter = new AtomicLong();
        CompletableFuture<Long> result = new CompletableFuture<>();
        AsyncReader reader = client.createAsyncReader(readerSettings, ReadEventHandlersSettings.newBuilder()
                .setEventHandler(new ReadEventHandler() {
                    @Override
                    public void onStartPartitionSession(StartPartitionSessionEvent event) {
                        Assert.assertEquals(0, event.getCommittedOffset());
                        Assert.assertEquals(0, event.getPartitionOffsets().getStart());
                        Assert.assertEquals(6, event.getPartitionOffsets().getEnd());

                        // read only from offset 2
                        event.confirm(StartPartitionSessionSettings.newBuilder().setReadOffset(2L).build());
                    }

                    @Override
                    public void onMessages(DataReceivedEvent event) {
                        for (Message msg : event.getMessages()) {
                            Assert.assertEquals(msg.getOffset(), 2 + counter.get());
                            long read = counter.incrementAndGet();
                            if (new String(msg.getData()).equals("stop")) {
                                result.complete(read);
                            }
                        }
                    }
                }).build());

        reader.init().join();
        Assert.assertEquals(Long.valueOf(4), result.join());
        reader.shutdown().join();
    }

    @Test
    public void readFromWithCommitTest() throws Exception {
        ReaderSettings readerSettings = ReaderSettings.newBuilder()
                .addTopic(TopicReadSettings.newBuilder().setPath(TEST_TOPIC).build())
                .setConsumerName(TEST_CONSUMER1)
                .build();

        AtomicLong counter = new AtomicLong();
        CompletableFuture<Long> result = new CompletableFuture<>();
        AsyncReader reader = client.createAsyncReader(readerSettings, ReadEventHandlersSettings.newBuilder()
                .setEventHandler(new ReadEventHandler() {
                    @Override
                    public void onStartPartitionSession(StartPartitionSessionEvent event) {
                        Assert.assertEquals(0, event.getCommittedOffset());
                        Assert.assertEquals(0, event.getPartitionOffsets().getStart());
                        Assert.assertEquals(6, event.getPartitionOffsets().getEnd());

                        // read only from offset 2
                        event.confirm(StartPartitionSessionSettings.newBuilder()
                                .setReadOffset(2L)
                                .setCommitOffset(2L)
                                .build());
                    }

                    @Override
                    public void onMessages(DataReceivedEvent event) {
                        event.commit().join();
                        for (Message msg : event.getMessages()) {
                            Assert.assertEquals(msg.getOffset(), 2 + counter.get());
                            long read = counter.incrementAndGet();
                            if (new String(msg.getData()).equals("stop")) {
                                result.complete(read);
                            }
                        }
                    }
                }).build());

        reader.init().join();
        Assert.assertEquals(Long.valueOf(4), result.join());
        reader.shutdown().join();
    }

    @Test
    public void readRetentionedTopicTest() throws Exception {
        ReaderSettings readerSettings = ReaderSettings.newBuilder()
                .addTopic(TopicReadSettings.newBuilder().setPath(TEST_TOPIC).build())
                .setConsumerName(TEST_CONSUMER1)
                .build();

        AtomicLong counter = new AtomicLong();
        CompletableFuture<Long> result = new CompletableFuture<>();
        AsyncReader reader = client.createAsyncReader(readerSettings, ReadEventHandlersSettings.newBuilder()
                .setEventHandler(new ReadEventHandler() {
                    @Override
                    public void onStartPartitionSession(StartPartitionSessionEvent event) {
                        Assert.assertEquals(0, event.getCommittedOffset());
                        Assert.assertEquals(0, event.getPartitionOffsets().getStart());
                        Assert.assertEquals(6, event.getPartitionOffsets().getEnd());

                        // emulate topic retention - skip first 2 message but don't commit them
                        event.confirm(StartPartitionSessionSettings.newBuilder()
                                .setReadOffset(2L)
                                .build());
                    }

                    @Override
                    public void onMessages(DataReceivedEvent event) {
                        event.commit().join();
                        for (Message msg : event.getMessages()) {
                            Assert.assertEquals(msg.getOffset(), 2 + counter.get());
                            long read = counter.incrementAndGet();
                            if (new String(msg.getData()).equals("stop")) {
                                result.complete(read);
                            }
                        }
                    }
                }).build());

        reader.init().join();
        Assert.assertEquals(Long.valueOf(4), result.join());
        reader.shutdown().join();
    }
}
