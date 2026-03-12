package tech.ydb.topic.impl;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.config.Configurator;
import org.apache.logging.log4j.spi.ExtendedLogger;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tech.ydb.test.junit4.GrpcTransportRule;
import tech.ydb.topic.TopicClient;
import tech.ydb.topic.description.Consumer;
import tech.ydb.topic.read.AsyncReader;
import tech.ydb.topic.read.Message;
import tech.ydb.topic.read.events.DataReceivedEvent;
import tech.ydb.topic.read.events.ReadEventHandler;
import tech.ydb.topic.read.events.StartPartitionSessionEvent;
import tech.ydb.topic.read.impl.ReadPartitionSession;
import tech.ydb.topic.settings.CreateTopicSettings;
import tech.ydb.topic.settings.ReadEventHandlersSettings;
import tech.ydb.topic.settings.ReaderSettings;
import tech.ydb.topic.settings.StartPartitionSessionSettings;
import tech.ydb.topic.settings.TopicReadSettings;
import tech.ydb.topic.settings.WriterSettings;
import tech.ydb.topic.write.SyncWriter;

/**
 *
 * @author Aleksandr Gorshenin
 */
public class TopicReadersIntegrationTest {
    private static final Logger logger = LoggerFactory.getLogger(YdbTopicsIntegrationTest.class);

    @ClassRule
    public final static GrpcTransportRule ydbTransport = new GrpcTransportRule();

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
    public void singleThreadExecutorTest() throws Exception {
        ExtendedLogger silenceLogger = LogManager.getContext(true).getLogger(ReadPartitionSession.class);
        Level level = silenceLogger.getLevel();

        ReaderSettings readerSettings = ReaderSettings.newBuilder()
                .addTopic(TopicReadSettings.newBuilder()
                        .setPath(TEST_TOPIC)
                        .build())
                .setConsumerName(TEST_CONSUMER1)
                .build();

        try {
            // temporary disable logging
            Configurator.setLevel(silenceLogger, Level.OFF);

            Semaphore messageCount = new Semaphore(0);
            CompletableFuture<Boolean> processing = new CompletableFuture<>();

            ExecutorService executor = Executors.newSingleThreadExecutor((r) -> new Thread(r, "test-executor"));
            AsyncReader reader = client.createAsyncReader(readerSettings, ReadEventHandlersSettings.newBuilder()
                    .setExecutor(executor)
                    .setEventHandler((event) -> {
                        messageCount.release();
                        processing.join();
                    }).build()
            );

            reader.init().join();

            // wait for message committing
            messageCount.acquireUninterruptibly();

            // stop reader
            CompletableFuture<Void> f = reader.shutdown();
            processing.completeExceptionally(new RuntimeException("shutdown"));
            f.get(5, TimeUnit.SECONDS);

            executor.shutdownNow();
        } finally {
            Configurator.setLevel(silenceLogger, level);
        }
    }

    @Test
    public void readAllTest() {
        ReaderSettings readerSettings = ReaderSettings.newBuilder()
                .addTopic(TopicReadSettings.newBuilder().setPath(TEST_TOPIC).build())
                .setConsumerName(TEST_CONSUMER1)
                .build();

        AtomicLong counter = new AtomicLong();
        CompletableFuture<Long> result = new CompletableFuture<>();
        AsyncReader reader = client.createAsyncReader(readerSettings, ReadEventHandlersSettings.newBuilder()
                .setEventHandler((DataReceivedEvent event) -> {
            for (Message msg: event.getMessages()) {
                Assert.assertEquals(msg.getOffset(), counter.get());
                long read = counter.incrementAndGet();
                if (new String(msg.getData()).equals("stop")) {
                    result.complete(read);
                }
            }
        }).build());

        reader.init().join();
        Assert.assertEquals(Long.valueOf(6), result.join());
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
}
