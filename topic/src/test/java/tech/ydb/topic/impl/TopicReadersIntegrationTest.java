package tech.ydb.topic.impl;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tech.ydb.core.Status;
import tech.ydb.test.junit4.GrpcTransportRule;
import tech.ydb.topic.TopicClient;
import tech.ydb.topic.description.Consumer;
import tech.ydb.topic.read.AsyncReader;
import tech.ydb.topic.settings.CreateTopicSettings;
import tech.ydb.topic.settings.ReadEventHandlersSettings;
import tech.ydb.topic.settings.ReaderSettings;
import tech.ydb.topic.settings.TopicReadSettings;
import tech.ydb.topic.settings.WriterSettings;
import tech.ydb.topic.write.Message;
import tech.ydb.topic.write.SyncWriter;

/**
 *
 * @author Aleksandr Gorshenin
 */
public class TopicReadersIntegrationTest {
    private final static Logger logger = LoggerFactory.getLogger(YdbTopicsIntegrationTest.class);

    @ClassRule
    public final static GrpcTransportRule ydbTransport = new GrpcTransportRule();

    private final static String TEST_TOPIC = "topic_readers_test";

    private final static String TEST_CONSUMER1 = "consumer";

    private static TopicClient client;

    @BeforeClass
    public static void initTopic() {
        logger.info("Create test table  {} ...", TEST_TOPIC);

        client = TopicClient.newClient(ydbTransport).build();
        client.createTopic(TEST_TOPIC, CreateTopicSettings.newBuilder()
                .addConsumer(Consumer.newBuilder().setName(TEST_CONSUMER1).build())
                .build()
        ).join().expectSuccess("can't create a new topic");
    }

    @AfterClass
    public static void dropTopic() {
        logger.info("Drop test topic {} ...", TEST_TOPIC);
        Status dropStatus = client.dropTopic(TEST_TOPIC).join();
        client.close();
        dropStatus.expectSuccess("can't drop test topic");
    }

    private void sendMessages(Message... messages) throws Exception {
        WriterSettings settings = WriterSettings.newBuilder()
                .setTopicPath(TEST_TOPIC)
                .setProducerId("helper")
                .build();
        SyncWriter writer = client.createSyncWriter(settings);
        writer.initAndWait();
        for (Message message : messages) {
            writer.send(message);
        }

        writer.flush();
        writer.shutdown(10, TimeUnit.SECONDS);
    }

    @Test
    public void singleThreadExecutorTest() throws Exception {
        ReaderSettings readerSettings = ReaderSettings.newBuilder()
                .addTopic(TopicReadSettings.newBuilder()
                        .setPath(TEST_TOPIC)
                        .build())
                .setConsumerName(TEST_CONSUMER1)
                .build();

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

        sendMessages(Message.of("test".getBytes()));

        // wait for message committing
        messageCount.acquireUninterruptibly();

        // stop reader
        CompletableFuture<Void> f = reader.shutdown();
        processing.completeExceptionally(new RuntimeException("shutdown"));
        f.get(5, TimeUnit.SECONDS);

        executor.shutdownNow();
    }
}
