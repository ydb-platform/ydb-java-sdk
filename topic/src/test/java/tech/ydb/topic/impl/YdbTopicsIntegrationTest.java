package tech.ydb.topic.impl;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tech.ydb.core.Status;
import tech.ydb.test.junit4.GrpcTransportRule;
import tech.ydb.topic.TopicClient;
import tech.ydb.topic.description.Consumer;
import tech.ydb.topic.description.TopicDescription;
import tech.ydb.topic.read.AsyncReader;
import tech.ydb.topic.read.DeferredCommitter;
import tech.ydb.topic.read.SyncReader;
import tech.ydb.topic.read.events.AbstractReadEventHandler;
import tech.ydb.topic.read.events.DataReceivedEvent;
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
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class YdbTopicsIntegrationTest {
    private final static Logger logger = LoggerFactory.getLogger(YdbTopicsIntegrationTest.class);

    @ClassRule
    public final static GrpcTransportRule ydbTransport = new GrpcTransportRule();

    private final static String TEST_TOPIC = "integration_test_topic";
    private final static String TEST_CONSUMER1 = "consumer";
    private final static String TEST_CONSUMER2 = "other_consumer";

    private static TopicClient client;
    final static CountDownLatch latch1 = new CountDownLatch(1);
    final static CountDownLatch latch2 = new CountDownLatch(1);
    final static CountDownLatch latch3WithCommit = new CountDownLatch(1);
    final static CountDownLatch latch3WithoutCommit = new CountDownLatch(1);
    final static CountDownLatch latch4 = new CountDownLatch(1);
    final static CountDownLatch latch5 = new CountDownLatch(1);

    private final static byte[][] TEST_MESSAGES = new byte[][] {
        "Test message".getBytes(),
        "".getBytes(),
        " ".getBytes(),
        "Other message".getBytes(),
        "Last message".getBytes(),
    };

    @BeforeClass
    public static void initTopic() {
        logger.info("Create test topic {} ...", TEST_TOPIC);

        client = TopicClient.newClient(ydbTransport).build();
        client.createTopic(TEST_TOPIC, CreateTopicSettings.newBuilder()
                .addConsumer(Consumer.newBuilder().setName(TEST_CONSUMER1).build())
                .addConsumer(Consumer.newBuilder().setName(TEST_CONSUMER2).build())
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

    @Test
    public void step01_writeWithoutDeduplication() throws InterruptedException, ExecutionException, TimeoutException {
        WriterSettings settings = WriterSettings.newBuilder()
                .setTopicPath(TEST_TOPIC)
                .build();
        SyncWriter writer = client.createSyncWriter(settings);
        writer.init();

        for (int idx = 0; idx < TEST_MESSAGES.length; idx += 1) {
            writer.send(Message.newBuilder().setData(TEST_MESSAGES[idx]).build());
        }

        for (int idx = TEST_MESSAGES.length - 1; idx >= 0; idx -= 1) {
            writer.send(Message.newBuilder().setData(TEST_MESSAGES[idx]).build());
        }

        writer.flush();
        writer.shutdown(1, TimeUnit.MINUTES);
        latch1.countDown();
    }

    @Test
    public void step02_readHalfWithoutCommit() throws InterruptedException {
        latch1.await(5, TimeUnit.SECONDS);
        ReaderSettings settings = ReaderSettings.newBuilder()
                .addTopic(TopicReadSettings.newBuilder().setPath(TEST_TOPIC).build())
                .setConsumerName(TEST_CONSUMER1)
                .build();

        SyncReader reader = client.createSyncReader(settings);
        reader.initAndWait();

        for (byte[] bytes: TEST_MESSAGES) {
            tech.ydb.topic.read.Message msg = reader.receive();
            Assert.assertArrayEquals(bytes, msg.getData());
        }

        reader.shutdown();
        latch2.countDown();
    }

    @Test
    public void step03_readHalfWithCommit() throws InterruptedException {
        latch2.await(5, TimeUnit.SECONDS);
        ReaderSettings settings = ReaderSettings.newBuilder()
                .addTopic(TopicReadSettings.newBuilder().setPath(TEST_TOPIC).build())
                .setConsumerName(TEST_CONSUMER1)
                .build();

        SyncReader reader = client.createSyncReader(settings);
        reader.initAndWait();

        for (byte[] bytes: TEST_MESSAGES) {
            tech.ydb.topic.read.Message msg = reader.receive();
            Assert.assertArrayEquals(bytes, msg.getData());
            msg.commit();
        }

        reader.shutdown();
        latch3WithCommit.countDown();
    }

    @Test
    public void step03_readNextHalfWithoutCommit() throws InterruptedException {
        latch3WithCommit.await(5, TimeUnit.SECONDS);
        ReaderSettings settings = ReaderSettings.newBuilder()
                .addTopic(TopicReadSettings.newBuilder().setPath(TEST_TOPIC).build())
                .setConsumerName(TEST_CONSUMER1)
                .build();

        SyncReader reader = client.createSyncReader(settings);
        reader.initAndWait();

        for (int idx = TEST_MESSAGES.length - 1; idx >= 0; idx -= 1) {
            tech.ydb.topic.read.Message msg = reader.receive();
            Assert.assertArrayEquals(TEST_MESSAGES[idx], msg.getData());
        }

        reader.shutdown();
        latch3WithoutCommit.countDown();
    }

    @Test
    public void step04_readNextHalfWithCommit() throws InterruptedException {
        latch3WithoutCommit.await(5, TimeUnit.SECONDS);
        ReaderSettings settings = ReaderSettings.newBuilder()
                .addTopic(TopicReadSettings.newBuilder().setPath(TEST_TOPIC).build())
                .setConsumerName(TEST_CONSUMER1)
                .build();

        SyncReader reader = client.createSyncReader(settings);
        reader.initAndWait();

        DeferredCommitter committer = DeferredCommitter.newInstance();
        for (int idx = TEST_MESSAGES.length - 1; idx >= 0; idx -= 1) {
            tech.ydb.topic.read.Message msg = reader.receive();
            Assert.assertArrayEquals(TEST_MESSAGES[idx], msg.getData());
            committer.add(msg);
        }

        committer.commit();
        reader.shutdown();
        latch4.countDown();
    }

    @Test
    public void step05_describeTopic() throws InterruptedException {
        latch4.await(5, TimeUnit.SECONDS);
        TopicDescription description = client.describeTopic(TEST_TOPIC).join().getValue();

        Assert.assertNull(description.getTopicStats());
        List<Consumer> consumers = description.getConsumers();
        Assert.assertEquals(2, consumers.size());

        Assert.assertEquals(TEST_CONSUMER1, consumers.get(0).getName());
        Assert.assertEquals(TEST_CONSUMER2, consumers.get(1).getName());
        latch5.countDown();
    }

    @Test
    public void step06_readAllByAsyncReader() throws InterruptedException {
        latch5.await(5, TimeUnit.SECONDS);
        ReaderSettings settings = ReaderSettings.newBuilder()
                .addTopic(TopicReadSettings.newBuilder().setPath(TEST_TOPIC).build())
                .setConsumerName(TEST_CONSUMER2)
                .build();

        final AtomicInteger atomicIdx = new AtomicInteger(0);
        final CompletableFuture<Void> wait = new CompletableFuture<>();
        final byte[][] results = new byte[TEST_MESSAGES.length * 2][];

        AsyncReader reader = client.createAsyncReader(settings, ReadEventHandlersSettings.newBuilder()
                .setEventHandler(new AbstractReadEventHandler() {
                    @Override
                    public void onMessages(DataReceivedEvent dre) {
                        for (tech.ydb.topic.read.Message msg: dre.getMessages()) {
                            int idx = atomicIdx.getAndIncrement();
                            if (idx < results.length) {
                                results[idx] = msg.getData();
                            }
                            if (idx >= results.length - 1) {
                                wait.complete(null);
                                return;
                            }
                        }
                    }
                })
                .build());

        reader.init();
        wait.join();

        reader.shutdown().join();

        for (int idx = 0; idx < TEST_MESSAGES.length; idx += 1) {
            Assert.assertArrayEquals(TEST_MESSAGES[idx], results[idx]);
            Assert.assertArrayEquals(TEST_MESSAGES[idx], results[results.length - idx - 1]);
        }
    }
}
