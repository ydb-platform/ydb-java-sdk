package tech.ydb.topic.impl;

import java.util.Arrays;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tech.ydb.core.Status;
import tech.ydb.core.utils.FutureTools;
import tech.ydb.test.junit4.GrpcTransportRule;
import tech.ydb.topic.TopicClient;
import tech.ydb.topic.settings.CreateTopicSettings;
import tech.ydb.topic.settings.WriterSettings;
import tech.ydb.topic.write.AsyncWriter;
import tech.ydb.topic.write.Message;
import tech.ydb.topic.write.QueueOverflowException;
import tech.ydb.topic.write.SyncWriter;
import tech.ydb.topic.write.WriteAck;

/**
 *
 * @author Aleksandr Gorshenin
 */
public class TopicWritersIntegrationTest {
    private final static Logger logger = LoggerFactory.getLogger(TopicWritersIntegrationTest.class);

    @ClassRule
    public final static GrpcTransportRule ydbTransport = new GrpcTransportRule();

    private final static String TEST_TOPIC = "topic_writers_test";

    private final static String TEST_PRODUCER1 = "producer";

    private static TopicClient client;

    @BeforeClass
    public static void initTopic() {
        logger.info("Create test table  {} ...", TEST_TOPIC);

        client = TopicClient.newClient(ydbTransport).build();
        client.createTopic(TEST_TOPIC, CreateTopicSettings.newBuilder().build())
                .join().expectSuccess("can't create a new topic");
    }

    @AfterClass
    public static void dropTopic() {
        logger.info("Drop test topic {} ...", TEST_TOPIC);
        Status dropStatus = client.dropTopic(TEST_TOPIC).join();
        client.close();
        dropStatus.expectSuccess("can't drop test topic");
    }

    @Test
    public void messageBufferOverflowTest() throws Exception {
        WriterSettings settings = WriterSettings.newBuilder()
                .setTopicPath(TEST_TOPIC)
                .setProducerId(TEST_PRODUCER1)
                .setMaxSendBufferMemorySize(1000)
                .build();

        SyncWriter writer = client.createSyncWriter(settings);
        writer.initAndWait();

        byte[] msg1 = new byte[1000];
        byte[] msg2 = new byte[1001];
        Arrays.fill(msg1, (byte) 0x10);
        Arrays.fill(msg2, (byte) 0x11);

        writer.send(Message.of(msg1));
        writer.send(Message.of(msg1));
        writer.send(Message.of(msg1));
        writer.send(Message.of(msg2)); // this message is more than buffer limit
        writer.send(Message.of(msg1));
        writer.send(Message.of(msg1));
        writer.send(Message.of(msg1));

        writer.flush();
        writer.shutdown(10, TimeUnit.SECONDS);
    }

    @Test
    public void lazyInitTest() throws Exception {
        WriterSettings settings = WriterSettings.newBuilder()
                .setTopicPath(TEST_TOPIC)
                .setProducerId(TEST_PRODUCER1)
                .build();

        AsyncWriter writer = client.createAsyncWriter(settings);

        CountDownLatch latch = new CountDownLatch(1);
        CompletableFuture<WriteAck> lastMessage = CompletableFuture.supplyAsync(() -> {
            ThreadLocalRandom rnd = ThreadLocalRandom.current();
            try {
                CompletableFuture<WriteAck> ack = FutureTools.failedFuture(new RuntimeException("not started"));
                for (int idx = 0; idx < 100; idx++) {
                    byte[] msg = new byte[1000];
                    rnd.nextBytes(msg);
                    ack = writer.send(Message.of(msg));
                }
                latch.countDown();
                return ack.join();
            } catch (QueueOverflowException ex) {
                latch.countDown();
                throw new RuntimeException(ex);
            }
        });

        latch.await(10, TimeUnit.SECONDS);
        writer.init();

        WriteAck ack = lastMessage.get(10, TimeUnit.SECONDS);
        Assert.assertEquals(WriteAck.State.WRITTEN, ack.getState());

        writer.shutdown().join();
    }
}
