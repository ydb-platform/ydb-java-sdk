package tech.ydb.topic.impl;

import com.sun.security.ntlm.Client;
import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tech.ydb.core.Status;
import tech.ydb.test.junit4.GrpcTransportRule;
import tech.ydb.topic.TopicClient;
import tech.ydb.topic.description.Consumer;
import tech.ydb.topic.description.CustomTopicCodec;
import tech.ydb.topic.read.SyncReader;
import tech.ydb.topic.settings.CreateTopicSettings;
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
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;


/**
 * Test connecting to custom codec
 */
public class YdbTopicsCustomCodecIntegrationTest {
    private final static Logger logger = LoggerFactory.getLogger(YdbTopicsCustomCodecIntegrationTest.class);

    @ClassRule
    public final static GrpcTransportRule ydbTransport = new GrpcTransportRule();

    private final static String TEST_TOPIC1 = "integration_test_custom_codec_topic1";
    private final static String TEST_TOPIC2 = "integration_test_custom_codec_topic2";
    private final static String TEST_CONSUMER1 = "consumer";
    private final static String TEST_CONSUMER2 = "other_consumer";

    private static TopicClient client1;
    private static TopicClient client2;

    private final static byte[][] TEST_MESSAGES = new byte[][]{
            "Test message".getBytes(),
            "".getBytes(),
            " ".getBytes(),
            "Other message".getBytes(),
            "Last message".getBytes(),
    };

    /**
     * Ability to use custom codec with write and read
     * This positive test checks that we can read and write in one topic
     * <p>
     * STEPS
     * 1. Create client
     * 2. Create topic TEST_TOPIC1
     * 3. Create custom codec
     * 4. Register codec with id = 10113 and CustomTopicCodec
     * 5. Write data to topic with codec = 10113
     * 6. Read data from topic without errors
     *
     */
    @Test
    public void writeDataAndReadDataWithCustomCodec() throws InterruptedException, ExecutionException, TimeoutException {
        try {
            client1 = createClient();
            createTopic(client1, TEST_TOPIC1);

            CustomTopicCodec codec = new CustomCustomTopicCode(1);

            client1.registerCodec(10113, codec);

            writeData(10113, TEST_TOPIC1);

            readData(TEST_TOPIC1);
        } finally {
            deleteTopic(TEST_TOPIC1);
        }
    }

    /**
     * Ability to write to different topic in different codecs.
     * This test checks that in one client we can make arbitrary codecs which don't disturb each other
     * <p>
     * STEPS
     * 1. Create client
     * 2.1. Create topic TEST_TOPIC1
     * 2.2. Create topic TEST_TOPIC2
     * 3.1. Create custom codec1
     * 3.2. Create custom codec2
     * 4.1. Register codec with id = 10113 and codec1
     * 4.2. Register codec with id = 10114 and codec2
     * 5.1. Write data to TEST_TOPIC1 with codec = 10113
     * 5.1. Write data to TEST_TOPIC2 with codec = 10114
     * 6.1. Read data from TEST_TOPIC1 without errors
     * 6.1. Read data from TEST_TOPIC2 without errors
     *
     */
    @Test
    public void writeInTwoTopicsInOneClientWithDifferentCustomCodec() throws ExecutionException, InterruptedException, TimeoutException {
        try {
            client1 = createClient();

            createTopic(client1, TEST_TOPIC1);
            createTopic(client1, TEST_TOPIC2);

            CustomTopicCodec codec1 = new CustomCustomTopicCode(1);
            CustomTopicCodec codec2 = new CustomCustomTopicCode(7);

            client1.registerCodec(10113, codec1);
            client1.registerCodec(10114, codec2);

            writeData(10113, TEST_TOPIC1);
            writeData(10114, TEST_TOPIC2);


            readData(TEST_TOPIC1);
            readData(TEST_TOPIC2);
        } finally {
            deleteTopic(TEST_TOPIC1);
        }
    }

    /**
     * Ability to write to different topic in different clients with same id
     * This test checks that different client don't exchange codecs CodecRegistry with each other
     * <p>
     * STEPS
     * 1.1. Create client1
     * 1.2. Create client2
     * 2.1. Create topic TEST_TOPIC1 in client1
     * 2.2. Create topic TEST_TOPIC2 in client1
     * 3.1. Create custom codec1
     * 3.2. Create custom codec2
     * 4.1. Register codec with id = 10113 and codec1
     * 4.2. Register codec with id = 10113 and codec2
     * 5.1. Write data to TEST_TOPIC1 with codec = 10113
     * 5.1. Write data to TEST_TOPIC2 with codec = 10113
     * 6.1. Read data from TEST_TOPIC1 without errors
     * 6.1. Read data from TEST_TOPIC2 without errors
     *
     */
    @Test
    public void writeInTwoTopicWithDifferentCodecWithOneIdShouldNotFailed() throws ExecutionException, InterruptedException, TimeoutException {
        try {
            createTopic(client1, TEST_TOPIC1);
            createTopic(client1, TEST_TOPIC2);

            CustomTopicCodec codec1 = new CustomCustomTopicCode(1);
            CustomTopicCodec codec2 = new CustomCustomTopicCode(7);

            client1.registerCodec(10113, codec1);
            client2.registerCodec(10113, codec2);

            writeData(10113, TEST_TOPIC1);
            writeData(10113, TEST_TOPIC2);

            readData(TEST_TOPIC1);
            readData(TEST_TOPIC2);
        } finally {
            deleteTopic(TEST_TOPIC1);
        }
    }

    /**
     * Ability to write to different topic in different clients with same id
     * This test checks that different client don't exchange codecs CodecRegistry with each other
     * <p>
     * STEPS
     * 1.1. Create client1
     * 1.2. Create client2
     * 2.1. Create topic TEST_TOPIC1 in client1
     * 2.2. Create topic TEST_TOPIC2 in client1
     * 3.1. Create custom codec1
     * 3.2. Create custom codec2
     * 4.1. Register codec with id = 10113 and codec1
     * 4.2. Register codec with id = 10113 and codec2
     * 5.1. Write data to TEST_TOPIC1 with codec = 10113
     * 5.1. Write data to TEST_TOPIC2 with codec = 10113
     * 6.1. Read data from TEST_TOPIC1 without errors
     * 6.1. Read data from TEST_TOPIC2 without errors
     *
     */
    @Test
    public void readUsingWrongCodec() throws ExecutionException, InterruptedException, TimeoutException {
        try {
            createTopic(client1, TEST_TOPIC1);

            CustomTopicCodec codec1 = new CustomCustomTopicCode(1);
            CustomTopicCodec codec2 = new CustomCustomTopicCode(7);

            client1.registerCodec(10113, codec1);

            writeData(10113, TEST_TOPIC1);

            client1.registerCodec(10113, codec2);

            readDataFail(TEST_TOPIC1);
        } finally {
            deleteTopic(TEST_TOPIC1);
        }
    }

    /**
     * This test checks that read with codec changed with write failed
     * <p>
     * STEPS
     * 1 Create client1
     * 2. Create topic TEST_TOPIC1 in client1
     * 3. Create custom codec1
     * 4. Register codec with id = 10113 and codec1
     * 5. Write data to TEST_TOPIC1 with codec = 10113
     * 6. Register codec with id = 10113 and codec2
     * 7. Read data from TEST_TOPIC1 with errors
     *
     */
    @Test
    public void readUsingWrongCodecIdentifierShouldNotPass() throws ExecutionException, InterruptedException, TimeoutException {
        try {
            createTopic(client1, TEST_TOPIC1);

            CustomTopicCodec codec1 = new CustomCustomTopicCode(1);
            CustomTopicCodec codec2 = new CustomCustomTopicCode(2);
            client1.registerCodec(10113, codec1);

            writeData(10113, TEST_TOPIC1);

            client1.registerCodec(10113, codec2);

            readData(TEST_TOPIC1);
        } finally {
            deleteTopic(TEST_TOPIC1);
        }
    }


    private TopicClient createClient() {
        return TopicClient.newClient(ydbTransport).build();
    }

    private void createTopic(TopicClient client, String topicName) {
        logger.info("Create test topic {} ...", topicName);

        client.createTopic(topicName, CreateTopicSettings.newBuilder()
                .addConsumer(Consumer.newBuilder().setName(TEST_CONSUMER1).build())
                .addConsumer(Consumer.newBuilder().setName(TEST_CONSUMER2).build())
                .build()
        ).join().expectSuccess("can't create a new topic");
    }

    private void deleteTopic(String topicName) {
        logger.info("Drop test topic {} ...", topicName);
        Status dropStatus = client1.dropTopic(topicName).join();
        client1.close();
        dropStatus.expectSuccess("can't drop test topic");
    }

    private void writeData(int codecId, String topicName) throws ExecutionException, InterruptedException, TimeoutException {
        WriterSettings settings = WriterSettings.newBuilder()
                .setTopicPath(topicName)
                .setCodec(codecId)
                .build();
        SyncWriter writer = client1.createSyncWriter(settings);
        writer.init();

        for (byte[] testMessage : TEST_MESSAGES) {
            writer.send(Message.newBuilder().setData(testMessage).build());
        }

        writer.flush();
        writer.shutdown(1, TimeUnit.MINUTES);
    }

    private void readData(String topicName) throws InterruptedException {
        ReaderSettings readerSettings = ReaderSettings.newBuilder()
                .addTopic(TopicReadSettings.newBuilder().setPath(topicName).build())
                .setConsumerName(TEST_CONSUMER1)
                .build();

        SyncReader reader = client1.createSyncReader(readerSettings);
        reader.initAndWait();

        for (byte[] bytes : TEST_MESSAGES) {
            tech.ydb.topic.read.Message msg = reader.receive(1, TimeUnit.SECONDS);
            Assert.assertArrayEquals(bytes, msg.getData());
        }

        reader.shutdown();
    }

    private void readDataFail(String topicName) throws InterruptedException {
        ReaderSettings readerSettings = ReaderSettings.newBuilder()
                .addTopic(TopicReadSettings.newBuilder().setPath(topicName).build())
                .setConsumerName(TEST_CONSUMER1)
                .build();

        SyncReader reader = client1.createSyncReader(readerSettings);
        reader.initAndWait();

        for (byte[] bytes : TEST_MESSAGES) {
            tech.ydb.topic.read.Message msg = reader.receive(1, TimeUnit.SECONDS);
            if (bytes.length != 0) {
                Assert.assertFalse(java.util.Arrays.equals(bytes, msg.getData()));
            }
        }

        reader.shutdown();
    }

    static class CustomCustomTopicCode implements CustomTopicCodec {

        final int stub;

        public CustomCustomTopicCode(int stub) {
            this.stub = stub;
        }


        @Override
        public InputStream decode(ByteArrayInputStream byteArrayOutputStream) throws IOException {
            final ByteArrayInputStream outputStream = byteArrayOutputStream;
            return new InputStream() {
                @Override
                public int read() throws IOException {
                    for (int i = 0; i < stub; i++) {
                        int stub = outputStream.read();
                    }

                    return outputStream.read();
                }
            };
        }

        @Override
        public OutputStream encode(ByteArrayOutputStream byteArrayOutputStream) throws IOException {
            return new OutputStream() {
                @Override
                public void write(int b) throws IOException {
                    for (int i = 0; i < stub; i++) {
                        byteArrayOutputStream.write(stub);
                    }

                    byteArrayOutputStream.write(b);
                }
            };
        }
    }
}
