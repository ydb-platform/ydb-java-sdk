package tech.ydb.topic.impl;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.FixMethodOrder;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.ydb.core.Status;
import tech.ydb.test.junit4.GrpcTransportRule;
import tech.ydb.topic.TopicClient;
import tech.ydb.topic.description.Consumer;
import tech.ydb.topic.description.TopicCodec;
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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Aleksandr Gorshenin
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class YdbTopicsCustomCodecIntegrationTest {
    private final static Logger logger = LoggerFactory.getLogger(YdbTopicsCustomCodecIntegrationTest.class);

    @ClassRule
    public final static GrpcTransportRule ydbTransport = new GrpcTransportRule();

    private final static String TEST_TOPIC = "integration_test_custom_codec_topic";
    private final static String TEST_CONSUMER1 = "consumer";
    private final static String TEST_CONSUMER2 = "other_consumer";

    private static TopicClient client;
    private static TopicCodec codec;

    private final static byte[][] TEST_MESSAGES = new byte[][]{
            "Test message".getBytes(),
            "".getBytes(),
            " ".getBytes(),
            "Other message".getBytes(),
            "Last message".getBytes(),
    };

    @Before
    public void initTopic() {
        logger.info("Create test topic {} ...", TEST_TOPIC);

        codec = new CustomTopicCode();
        client = TopicClient.newClient(ydbTransport).build();
        client.createTopic(TEST_TOPIC, CreateTopicSettings.newBuilder()
                .addConsumer(Consumer.newBuilder().setName(TEST_CONSUMER1).build())
                .addConsumer(Consumer.newBuilder().setName(TEST_CONSUMER2).build())
                .build()
        ).join().expectSuccess("can't create a new topic");
    }

    @After
    public void dropTopic() {
        logger.info("Drop test topic {} ...", TEST_TOPIC);
        Status dropStatus = client.dropTopic(TEST_TOPIC).join();
        client.close();
        dropStatus.expectSuccess("can't drop test topic");
    }

    @Test
    public void writeDataWithCustomCodec() throws InterruptedException, ExecutionException, TimeoutException {
        WriterSettings settings = WriterSettings.newBuilder()
                .setTopicPath(TEST_TOPIC)
                .setCodec(10113, codec)
                .build();
        SyncWriter writer = client.createSyncWriter(settings);
        writer.init();

        for (byte[] testMessage : TEST_MESSAGES) {
            writer.send(Message.newBuilder().setData(testMessage).build());
        }

        writer.flush();
        writer.shutdown(1, TimeUnit.MINUTES);
    }

    @Ignore
    @Test
    public void writeDataAndReadDataWithCustomCodec() throws InterruptedException, ExecutionException, TimeoutException {
        WriterSettings settings = WriterSettings.newBuilder()
                .setTopicPath(TEST_TOPIC)
                .setCodec(10113, codec)
                .build();
        SyncWriter writer = client.createSyncWriter(settings);
        writer.init();

        for (byte[] testMessage : TEST_MESSAGES) {
            writer.send(Message.newBuilder().setData(testMessage).build());
        }

        writer.flush();
        writer.shutdown(1, TimeUnit.MINUTES);

        ReaderSettings readerSettings = ReaderSettings.newBuilder()
                .addTopic(TopicReadSettings.newBuilder().setPath(TEST_TOPIC).build())
                .setConsumerName(TEST_CONSUMER1)
                .setCodec(10013, codec)
                .build();

        SyncReader reader = client.createSyncReader(readerSettings);
        reader.initAndWait();

        for (byte[] bytes : TEST_MESSAGES) {
            tech.ydb.topic.read.Message msg = reader.receive();
            Assert.assertArrayEquals(bytes, msg.getData());
        }

        reader.shutdown();
    }

    static class CustomTopicCode implements TopicCodec {

        @Override
        public InputStream decode(ByteArrayInputStream byteArrayOutputStream) throws IOException {
            return byteArrayOutputStream;
        }

        @Override
        public OutputStream encode(ByteArrayOutputStream byteArrayOutputStream) throws IOException {
            return byteArrayOutputStream;
        }

    }
}
