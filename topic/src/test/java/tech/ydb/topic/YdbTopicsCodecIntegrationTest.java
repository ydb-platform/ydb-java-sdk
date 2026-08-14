package tech.ydb.topic;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.Timeout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tech.ydb.test.junit4.GrpcTransportRule;
import tech.ydb.topic.description.Codec;
import tech.ydb.topic.description.Consumer;
import tech.ydb.topic.read.DecompressionException;
import tech.ydb.topic.read.SyncReader;
import tech.ydb.topic.read.impl.MessageDecoder;
import tech.ydb.topic.settings.CreateTopicSettings;
import tech.ydb.topic.settings.ReaderSettings;
import tech.ydb.topic.settings.TopicReadSettings;
import tech.ydb.topic.settings.WriterSettings;
import tech.ydb.topic.utils.HideLoggers;
import tech.ydb.topic.utils.HideLoggersRule;
import tech.ydb.topic.write.Message;
import tech.ydb.topic.write.SyncWriter;


/**
 * Test connecting to read write using all available codec
 *
 *  @author Evgeny Kuvardin
 */
public class YdbTopicsCodecIntegrationTest {
    private final static Logger logger = LoggerFactory.getLogger(YdbTopicsCodecIntegrationTest.class);

    @ClassRule
    public final static GrpcTransportRule ydbTransport = new GrpcTransportRule();

    @Rule
    public final Timeout timeout = new Timeout(10, TimeUnit.SECONDS);

    @Rule
    public final HideLoggersRule hideLogger = new HideLoggersRule();

    private final static String TEST_TOPIC1 = "integration_test_custom_codec_topic1";
    private final static String TEST_TOPIC2 = "integration_test_custom_codec_topic2";
    private final static String TEST_CONSUMER1 = "consumer_codec";
    private final static String TEST_CONSUMER2 = "other_consumer_codec";

    private final List<String> topicToDelete = new ArrayList<>();

    private final static String[] TEST_MESSAGES = new String[]{
            "Test message",
            "",
            " ",
            "Other message",
            "Last message",
    };

    private TopicClient.Builder buildClient() {
        return TopicClient.newClient(ydbTransport).setCompressionExecutor(Runnable::run);
    }

    @Before
    public void beforeEachTest() {
        topicToDelete.clear();
    }

    @After
    public void afterEachTest() {
        try (TopicClient client = buildClient().build()) {
            for (String topicName : topicToDelete) {
                logger.info("Drop test topic {} ...", topicName);
                client.dropTopic(topicName).join();
            }
        }
    }

    private void createTopic(TopicClient client, String topicName) {
        logger.info("Create test topic {} ...", topicName);

        client.createTopic(topicName, CreateTopicSettings.newBuilder()
                .addConsumer(Consumer.newBuilder().setName(TEST_CONSUMER1).build())
                .addConsumer(Consumer.newBuilder().setName(TEST_CONSUMER2).build())
                .build()
        ).join().expectSuccess("can't create a new topic");

        topicToDelete.add(topicName);
    }

    private void writeData(TopicClient client, String topicName, int codecId, String[] data) throws Exception {
        WriterSettings settings = WriterSettings.newBuilder()
                .setTopicPath(topicName)
                .setCodec(codecId)
                .build();
        SyncWriter writer = client.createSyncWriter(settings);
        writer.initAndWait();

        try {
            for (String msg : data) {
                writer.send(Message.newBuilder().setData(msg.getBytes()).build());
            }
            writer.flush();
        } finally {
            writer.shutdown(5, TimeUnit.SECONDS);
        }
    }

    private void readDataOK(TopicClient client, String topicName, Collection<String> expected) throws Exception {
        ReaderSettings readerSettings = ReaderSettings.newBuilder()
                .addTopic(TopicReadSettings.newBuilder().setPath(topicName).build())
                .setConsumerName(TEST_CONSUMER1)
                .build();

        SyncReader reader = client.createSyncReader(readerSettings);
        reader.initAndWait();

        try {
            for (String next: expected) {
                tech.ydb.topic.read.Message msg = reader.receive(1, TimeUnit.SECONDS);
                Assert.assertNotNull(msg);
                Assert.assertArrayEquals(next.getBytes(), msg.getData());
            }
        } finally {
            reader.shutdown();
        }
    }

    private void readDataFail(TopicClient client, String topicName, Collection<String> expected) throws Exception {
        ReaderSettings readerSettings = ReaderSettings.newBuilder()
                .addTopic(TopicReadSettings.newBuilder().setPath(topicName).build())
                .setConsumerName(TEST_CONSUMER1)
                .build();

        SyncReader reader = client.createSyncReader(readerSettings);
        reader.initAndWait();

        try {
            for (String next: expected) {
                tech.ydb.topic.read.Message msg = reader.receive(1, TimeUnit.SECONDS);
                Assert.assertNotNull(msg);
                // empty messages are always read successfully
                if (!next.isEmpty()) {
                    Assert.assertFalse(Arrays.equals(next.getBytes(), msg.getData()));
                }
            }
        } finally {
            reader.shutdown();
        }
    }

    private void readDataError(TopicClient client, String topicName, Collection<String> expected) throws Exception {
        ReaderSettings readerSettings = ReaderSettings.newBuilder()
                .addTopic(TopicReadSettings.newBuilder().setPath(topicName).build())
                .setConsumerName(TEST_CONSUMER1)
                .build();

        SyncReader reader = client.createSyncReader(readerSettings);
        reader.initAndWait();

        try {
            for (String next: expected) {
                tech.ydb.topic.read.Message msg = reader.receive(1, TimeUnit.SECONDS);
                Assert.assertNotNull(msg);
                // empty messages are always read successfully
                if (!next.isEmpty()) {
                    Exception th = Assert.assertThrows(DecompressionException.class, msg::getData);
                    Assert.assertEquals("Error occurred while decoding a message", th.getMessage());
                }
            }
        } finally {
            reader.shutdown();
        }
    }

    @Test
    public void writeAndReadWithCustomCodecTest() throws Exception {
        Codec c1 = new XorCodec(0xFF, 10113);
        Codec c2 = new XorCodec(0x12, 10114);

        try (TopicClient client = TopicClient.newClient(ydbTransport).registerCodec(c1).registerCodec(c2).build()) {
            createTopic(client, TEST_TOPIC1);

            writeData(client, TEST_TOPIC1, Codec.LZOP, TEST_MESSAGES);
            writeData(client, TEST_TOPIC1, Codec.GZIP, TEST_MESSAGES);
            writeData(client, TEST_TOPIC1, Codec.RAW, TEST_MESSAGES);
            writeData(client, TEST_TOPIC1, Codec.ZSTD, TEST_MESSAGES);
            writeData(client, TEST_TOPIC1, 10113, TEST_MESSAGES);
            writeData(client, TEST_TOPIC1, 10114, TEST_MESSAGES);

            List<String> expected = new ArrayList<>();
            expected.addAll(Arrays.asList(TEST_MESSAGES));
            expected.addAll(Arrays.asList(TEST_MESSAGES));
            expected.addAll(Arrays.asList(TEST_MESSAGES));
            expected.addAll(Arrays.asList(TEST_MESSAGES));
            expected.addAll(Arrays.asList(TEST_MESSAGES));
            expected.addAll(Arrays.asList(TEST_MESSAGES));

            readDataOK(client, TEST_TOPIC1, expected);
        }
    }

    @Test
    public void differentClientUsesDifferentRegistryTest() throws Exception {
        Codec c1 = new XorCodec(0xFF, 10113);
        Codec c2 = new XorCodec(0x12, 10113);

        try (TopicClient client = buildClient().registerCodec(c1).build()) {
            createTopic(client, TEST_TOPIC1);
            writeData(client, TEST_TOPIC1, 10113, TEST_MESSAGES);
        }

        try (TopicClient client = buildClient().registerCodec(c2).build()) {
            createTopic(client, TEST_TOPIC2);
            writeData(client, TEST_TOPIC2, 10113, TEST_MESSAGES);

            readDataFail(client, TEST_TOPIC1, Arrays.asList(TEST_MESSAGES));
        }

        try (TopicClient client = buildClient().registerCodec(c1).build()) {
            readDataFail(client, TEST_TOPIC2, Arrays.asList(TEST_MESSAGES));
        }
    }


    @Test
    @HideLoggers({ MessageDecoder.class })
    public void readShouldFailIfWithNotRegisteredCodec() throws Exception {
        Codec c1 = new XorCodec(0xFF, 10113);

        try (TopicClient client = buildClient().registerCodec(c1).build()) {
            createTopic(client, TEST_TOPIC1);
            writeData(client, TEST_TOPIC1, 10113, TEST_MESSAGES);
        }

        try (TopicClient client = buildClient().build()) {
            readDataError(client, TEST_TOPIC1, Arrays.asList(TEST_MESSAGES));
        }
    }

    @Test
    public void rewriteRawCodecIsDisabledTest() throws Exception {
        try (TopicClient client = buildClient().registerCodec(new XorCodec(0x44, Codec.RAW)).build()) {
            createTopic(client, TEST_TOPIC1);
            writeData(client, TEST_TOPIC1, Codec.RAW, TEST_MESSAGES);
            readDataOK(client, TEST_TOPIC1, Arrays.asList(TEST_MESSAGES));
        }

        try (TopicClient client = buildClient().build()) {
            readDataOK(client, TEST_TOPIC1, Arrays.asList(TEST_MESSAGES));
        }
    }

    @Test
    @HideLoggers({ MessageDecoder.class })
    public void rewriteZstdCodecTest() throws Exception {
        try (TopicClient client = buildClient().registerCodec(new XorCodec(0x44, Codec.ZSTD)).build()) {
            createTopic(client, TEST_TOPIC1);
            writeData(client, TEST_TOPIC1, Codec.ZSTD, TEST_MESSAGES);
            readDataOK(client, TEST_TOPIC1, Arrays.asList(TEST_MESSAGES));
        }

        try (TopicClient client = buildClient().build()) {
            readDataError(client, TEST_TOPIC1, Arrays.asList(TEST_MESSAGES));
        }
    }

    static class XorCodec implements Codec {
        final int code;
        final int codecId;

        public XorCodec(int code, int codecId) {
            this.code = code;
            this.codecId = codecId;
        }

        @Override
        public int getId() {
            return codecId;
        }

        @Override
        public InputStream decode(InputStream inputStream) throws IOException {
            return new InputStream() {
                @Override
                public int read() throws IOException {
                    int next = inputStream.read();
                    return next >= 0 ? (next ^ code) : -1;
                }
            };
        }

        @Override
        public OutputStream encode(OutputStream byteArrayOutputStream) throws IOException {
            return new OutputStream() {
                @Override
                public void write(int b) throws IOException {
                    byteArrayOutputStream.write(b ^ code);
                }
            };
        }
    }
}
