package tech.ydb.topic.impl;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tech.ydb.core.Status;
import tech.ydb.test.junit4.GrpcTransportRule;
import tech.ydb.topic.TopicClient;
import tech.ydb.topic.settings.CreateTopicSettings;
import tech.ydb.topic.settings.WriterSettings;
import tech.ydb.topic.write.Message;
import tech.ydb.topic.write.SyncWriter;

/**
 *
 * @author Aleksandr Gorshenin
 */
public class TopicWritersIntegrationTest {
    private final static Logger logger = LoggerFactory.getLogger(YdbTopicsIntegrationTest.class);

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
        writer.flush();

        IllegalArgumentException ex = Assert.assertThrows(IllegalArgumentException.class,
                () -> writer.send(Message.of(msg2))
        );
        Assert.assertEquals("Rejecting a message of 1001 bytes: not enough space in message queue. "
                + "The maximum size of buffer is 1000 bytes", ex.getMessage());

        writer.send(Message.of(msg1));
        writer.send(Message.of(msg1));
        writer.send(Message.of(msg1));
        writer.flush();
        writer.shutdown(10, TimeUnit.SECONDS);
    }
}
