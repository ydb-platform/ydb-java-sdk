package tech.ydb.topic.impl;

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
import tech.ydb.topic.description.RawCodec;
import tech.ydb.topic.description.SupportedCodecs;
import tech.ydb.topic.description.TopicCodec;
import tech.ydb.topic.description.TopicDescription;
import tech.ydb.topic.description.ZctdCodec;
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
public class YdbTopicsIntegrationTest2 {
    private final static Logger logger = LoggerFactory.getLogger(YdbTopicsIntegrationTest2.class);

    @ClassRule
    public final static GrpcTransportRule ydbTransport = new GrpcTransportRule();

    private final static String TEST_TOPIC = "integration_test_topic";
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

    @BeforeClass
    public static void initTopic() {
        logger.info("Create test topic {} ...", TEST_TOPIC);

        codec = new CustomTopicCode();
        client = TopicClient.newClient(ydbTransport).build();
        client.createTopic(TEST_TOPIC, CreateTopicSettings.newBuilder()
                .addConsumer(Consumer.newBuilder().setName(TEST_CONSUMER1).build())
                .addConsumer(Consumer.newBuilder().setName(TEST_CONSUMER2).build())
                .setSupportedCodecs(SupportedCodecs.newBuilder().addCodec(new ZctdCodec()).build())
                .build()
        ).join().expectSuccess("can't create a new topic");

        client.registerCodec(10001, codec);
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
    }

    @Test
    public void step02_readHalfWithoutCommit() throws InterruptedException {
        ReaderSettings settings = ReaderSettings.newBuilder()
                .addTopic(TopicReadSettings.newBuilder().setPath(TEST_TOPIC).build())
                .setConsumerName(TEST_CONSUMER1)
                .build();

        SyncReader reader = client.createSyncReader(settings);
        reader.initAndWait();

        for (byte[] bytes : TEST_MESSAGES) {
            tech.ydb.topic.read.Message msg = reader.receive();
            Assert.assertArrayEquals(bytes, msg.getData());
        }

        reader.shutdown();
    }

    @Test
    public void step03_readHalfWithCommit() throws InterruptedException {
        ReaderSettings settings = ReaderSettings.newBuilder()
                .addTopic(TopicReadSettings.newBuilder().setPath(TEST_TOPIC).build())
                .setConsumerName(TEST_CONSUMER1)
                .build();

        SyncReader reader = client.createSyncReader(settings);
        reader.initAndWait();

        for (byte[] bytes : TEST_MESSAGES) {
            tech.ydb.topic.read.Message msg = reader.receive();
            Assert.assertArrayEquals(bytes, msg.getData());
            msg.commit();
        }

        reader.shutdown();
    }

    @Test
    public void step03_readNextHalfWithoutCommit() throws InterruptedException {
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
    }

    @Test
    public void step04_readNextHalfWithCommit() throws InterruptedException {
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
    }

    @Test
    public void step05_describeTopic() throws InterruptedException {
        TopicDescription description = client.describeTopic(TEST_TOPIC).join().getValue();

        Assert.assertNull(description.getTopicStats());
        List<Consumer> consumers = description.getConsumers();
        Assert.assertEquals(2, consumers.size());

        Assert.assertEquals(TEST_CONSUMER1, consumers.get(0).getName());
        Assert.assertEquals(TEST_CONSUMER2, consumers.get(1).getName());
    }

    @Test
    public void step06_readAllByAsyncReader() throws InterruptedException {
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
                        for (tech.ydb.topic.read.Message msg : dre.getMessages()) {
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

    static class CustomTopicCode implements TopicCodec {

        @Override
        public OutputStream decode(ByteArrayOutputStream byteArrayOutputStream) throws IOException {
            return null;
        }

        @Override
        public InputStream encode(ByteArrayInputStream byteArrayInputStream) throws IOException {
            return null;
        }

        @Override
        public int getId() {
            return 0;
        }
    }
}
