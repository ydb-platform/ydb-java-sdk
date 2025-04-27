package tech.ydb.topic.impl;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tech.ydb.core.Status;
import tech.ydb.test.junit4.GrpcTransportRule;
import tech.ydb.topic.TopicClient;
import tech.ydb.topic.description.Codec;
import tech.ydb.topic.description.Consumer;
import tech.ydb.topic.description.CustomTopicCodec;
import tech.ydb.topic.read.DecompressionException;
import tech.ydb.topic.read.SyncReader;
import tech.ydb.topic.settings.CreateTopicSettings;
import tech.ydb.topic.settings.ReaderSettings;
import tech.ydb.topic.settings.TopicReadSettings;
import tech.ydb.topic.settings.WriterSettings;
import tech.ydb.topic.write.Message;
import tech.ydb.topic.write.SyncWriter;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;


/**
 * Test connecting to read write using all available codec
 *
 *  @author Evgeny Kuvardin
 */
public class YdbTopicsCodecIntegrationTest {
    private final static Logger logger = LoggerFactory.getLogger(YdbTopicsCodecIntegrationTest.class);

    @ClassRule
    public final static GrpcTransportRule ydbTransport = new GrpcTransportRule();

    private final static String TEST_TOPIC1 = "integration_test_custom_codec_topic1";
    private final static String TEST_TOPIC2 = "integration_test_custom_codec_topic2";
    private final static String TEST_CONSUMER1 = "consumer_codec";
    private final static String TEST_CONSUMER2 = "other_consumer_codec";

    private final List<String> topicToDelete = new ArrayList<>();
    private final List<TopicClient> clientToClose = new ArrayList<>();

    TopicClient client1;

    private final static String[] TEST_MESSAGES = new String[]{
            "Test message",
            "",
            " ",
            "Other message",
            "Last message",
    };

    Map<String, Deque<byte[][]>> queueOfMessages = new HashMap<>();

    @Before
    public void beforeEachTest() {
        topicToDelete.clear();
        clientToClose.clear();
    }

    @After
    public void afterEachTest() {
        for (String s : topicToDelete) {
            deleteTopic(s);
        }

        for (TopicClient topicClient : clientToClose) {
            topicClient.close();
        }

        queueOfMessages.clear();
    }

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
        client1 = createClient();
        createTopic(client1, TEST_TOPIC1);

        CustomTopicCodec codec = new CustomCustomTopicCode(1);

        client1.registerCodec(10113, codec);

        writeData(10113, TEST_TOPIC1, client1);

        readData(TEST_TOPIC1, client1);
    }

    /**
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
        client1 = createClient();

        createTopic(client1, TEST_TOPIC1);
        createTopic(client1, TEST_TOPIC2);

        CustomTopicCodec codec1 = new CustomCustomTopicCode(1);
        CustomTopicCodec codec2 = new CustomCustomTopicCode(7);

        client1.registerCodec(10113, codec1);
        client1.registerCodec(10114, codec2);

        writeData(10113, TEST_TOPIC1, client1);
        writeData(10114, TEST_TOPIC2, client1);

        readData(TEST_TOPIC1, client1);
        readData(TEST_TOPIC2, client1);
    }

    /**
     * This test checks that different client don't exchange CodecRegistry with each other
     * <p>
     * STEPS
     * 1.1. Create client1
     * 1.2. Create client2
     * 2.1. Create topic TEST_TOPIC1 in client1
     * 2.2. Create topic TEST_TOPIC2 in client2
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
        client1 = createClient();
        TopicClient client2 = createClient();

        createTopic(client1, TEST_TOPIC1);
        createTopic(client2, TEST_TOPIC2);

        CustomTopicCodec codec1 = new CustomCustomTopicCode(1);
        CustomTopicCodec codec2 = new CustomCustomTopicCode(7);

        client1.registerCodec(10113, codec1);
        client2.registerCodec(10113, codec2);

        writeData(10113, TEST_TOPIC1, client1);
        writeData(10113, TEST_TOPIC2, client2);

        readData(TEST_TOPIC1, client1);
        readData(TEST_TOPIC2, client2);

        client2.close();
    }

    /**
     * This test checks that overwrite existing codec with not backward compatibility will give an error
     * <p>
     * STEPS
     * 1. Create client1
     * 2. Create topic TEST_TOPIC1 in client1
     * 3.1. Create custom codec1
     * 3.2. Create custom codec2
     * 4. Register codec with id = 10113 and codec1
     * 5. Write data to TEST_TOPIC1 with codec = 10113
     * 6. Register codec with id = 10113 and codec2
     * 7. Read data from TEST_TOPIC1 with errors
     *
     */
    @Test
    public void readUsingWrongCodec() throws ExecutionException, InterruptedException, TimeoutException {
        client1 = createClient();
        createTopic(client1, TEST_TOPIC1);

        CustomTopicCodec codec1 = new CustomCustomTopicCode(1);
        CustomTopicCodec codec2 = new CustomCustomTopicCode(7);

        client1.registerCodec(10113, codec1);

        writeData(10113, TEST_TOPIC1, client1);

        client1.registerCodec(10113, codec2);

        readDataFail(TEST_TOPIC1, client1);
    }


    /**
     * Test checks that we can write in one TopicUsing differentCodecs
     * <p>
     * STEPS
     * 1. Create client1
     * 2. Create topic TEST_TOPIC1 in client1
     * 3.1. Create custom codec1
     * 3.2. Create custom codec2
     * 4.1. Register codec with id = 10113 and codec1
     * 4.2. Register codec with id = 10113 and codec2
     * 7. Write data with codec 1, 10014, 2, 4, 10113, 3
     * 8. Read data without errors
     *
     */
    @Test
    public void writeInOneTopicWithDifferentCodec() throws ExecutionException, InterruptedException, TimeoutException {
        client1 = createClient();
        createTopic(client1, TEST_TOPIC1);

        CustomTopicCodec codec1 = new CustomCustomTopicCode(1);
        CustomTopicCodec codec2 = new CustomCustomTopicCode(7);

        client1.registerCodec(10113, codec1);
        client1.registerCodec(10114, codec2);

        writeData(Codec.RAW, TEST_TOPIC1, client1);
        writeData(10114, TEST_TOPIC1, client1);
        writeData(Codec.GZIP, TEST_TOPIC1, client1);
        writeData(Codec.ZSTD, TEST_TOPIC1, client1);
        writeData(10113, TEST_TOPIC1, client1);
        writeData(Codec.LZOP, TEST_TOPIC1, client1);

        readData(TEST_TOPIC1, client1);
    }

    /**
     * In this test we verify that decode failed when code not found but after specify correct codec
     * Messages reads again and will be decoded
     * <p>
     * 1. Create client1
     * 2. Create topic TEST_TOPIC1 in client1
     * 3. Create custom codec1
     * 4. Register codec with id = 10113 and codec1
     * 5. Write data with codec 10113
     * 6. Unregister code
     * 7. Read data with errors
     * 8. Once again register codec with id = 10113 and codec1
     * 9. Read data without errors
     *
     */
    @Test
    public void readShouldFailIfWithNotRegisteredCodec() throws ExecutionException, InterruptedException, TimeoutException {
        client1 = createClient();
        createTopic(client1, TEST_TOPIC1);

        CustomTopicCodec codec1 = new CustomCustomTopicCode(1);

        client1.registerCodec(10113, codec1);
        writeData(10113, TEST_TOPIC1, client1);

        client1.unregisterCodec(10113);

        readDataWithError(TEST_TOPIC1, client1);

        client1.registerCodec(10113, codec1);
        readData(TEST_TOPIC1, client1);
    }

    /**
     * Test checks that we can't write into topic with unknown codec
     * <p>
     * 1. Create client1
     * 2. Create topic TEST_TOPIC1 in client1
     * 3. Try to write with reserved codec 7 -> get error
     * 4. Try to write with reserved codec 10000 -> get error
     * 5. Try to write with custom unregister codec 20000 -> get error
     */
    @Test
    public void writeWithReservedNotExistedCodec() {
        client1 = createClient();
        createTopic(client1, TEST_TOPIC1);

        Exception e = Assert.assertThrows(RuntimeException.class, () -> writeData(7, TEST_TOPIC1, client1));
        Assert.assertEquals("Cannot convert codec to proto. Unknown codec value: " + 7, e.getMessage());
    }

    @Test
    public void writeWithCustomCodec10000() {
        client1 = createClient();
        createTopic(client1, TEST_TOPIC1);

        Exception e = Assert.assertThrows(Exception.class, () -> writeData(10000, TEST_TOPIC1, client1));
        Assert.assertEquals("Unsupported codec: " + 10000, e.getCause().getMessage());
    }

    /**
     * Test checks that we can write and read using RAW Codec
     * <p>
     * 1. Create client1
     * 2. Create topic TEST_TOPIC1 in client1
     * 3. Try to write
     * 4. Read data
     */
    @Test
    public void readWriteRawCodec() throws ExecutionException, InterruptedException, TimeoutException {
        client1 = createClient();
        createTopic(client1, TEST_TOPIC1);

        writeData(Codec.RAW, TEST_TOPIC1, client1);

        readData(TEST_TOPIC1, client1);
    }

    /**
     * Test checks that we can write and read using GZIP Codec
     * <p>
     * 1. Create client1
     * 2. Create topic TEST_TOPIC1 in client1
     * 3. Try to write
     * 4. Read data
     */
    @Test
    public void readWriteGzipCodec() throws ExecutionException, InterruptedException, TimeoutException {
        client1 = createClient();
        createTopic(client1, TEST_TOPIC1);

        writeData(Codec.GZIP, TEST_TOPIC1, client1);

        readData(TEST_TOPIC1, client1);
    }

    /**
     * Test checks that we can write and read using Lzop Codec
     * <p>
     * 1. Create client1
     * 2. Create topic TEST_TOPIC1 in client1
     * 3. Try to write
     * 4. Read data
     */
    @Test
    public void readWriteLzopCodec() throws ExecutionException, InterruptedException, TimeoutException {
        client1 = createClient();
        createTopic(client1, TEST_TOPIC1);

        writeData(Codec.LZOP, TEST_TOPIC1, client1);

        readData(TEST_TOPIC1, client1);
    }

    /**
     * Test checks that we can write and read using Zstd Codec
     * <p>
     * 1. Create client1
     * 2. Create topic TEST_TOPIC1 in client1
     * 3. Try to write
     * 4. Read data
     */
    @Test
    public void readWriteZstdCodec() throws ExecutionException, InterruptedException, TimeoutException {
        client1 = createClient();
        createTopic(client1, TEST_TOPIC1);

        writeData(Codec.ZSTD, TEST_TOPIC1, client1);

        readData(TEST_TOPIC1, client1);
    }

    private TopicClient createClient() {
        TopicClient topicClient = TopicClient.newClient(ydbTransport).build();
        clientToClose.add(topicClient);
        return topicClient;
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

    private void deleteTopic(String topicName) {
        logger.info("Drop test topic {} ...", topicName);
        Status dropStatus = client1.dropTopic(topicName).join();
        client1.close();
        dropStatus.expectSuccess("can't drop test topic");
    }

    private void writeData(int codecId, String topicName, TopicClient client) throws ExecutionException, InterruptedException, TimeoutException {
        byte[][] testMessages = new byte[][]{
                (TEST_MESSAGES[0] + codecId).getBytes(),
                TEST_MESSAGES[1].getBytes(),
                TEST_MESSAGES[2].getBytes(),
                (TEST_MESSAGES[3] + codecId).getBytes(),
                (TEST_MESSAGES[4] + codecId).getBytes(),
        };

        writeData(codecId, topicName, client, testMessages);
    }

    private void writeData(int codecId, String topicName, TopicClient client, byte[][] testMessages) throws ExecutionException, InterruptedException, TimeoutException {
        WriterSettings settings = WriterSettings.newBuilder()
                .setTopicPath(topicName)
                .setCodec(codecId)
                .build();
        SyncWriter writer = client.createSyncWriter(settings);
        writeData(writer, topicName, testMessages);
    }

    private void writeData(SyncWriter writer, String topicName, byte[][] testMessages) throws ExecutionException, InterruptedException, TimeoutException {
        writer.init();

        Deque<byte[][]> deque = queueOfMessages.computeIfAbsent(topicName, k -> new ArrayDeque<>());
        deque.add(testMessages);

        for (byte[] testMessage : testMessages) {
            writer.send(Message.newBuilder().setData(testMessage).build());
        }

        writer.flush();
        writer.shutdown(1, TimeUnit.MINUTES);
    }

    private void readData(String topicName, TopicClient client) throws InterruptedException {
        ReaderSettings readerSettings = ReaderSettings.newBuilder()
                .addTopic(TopicReadSettings.newBuilder().setPath(topicName).build())
                .setConsumerName(TEST_CONSUMER1)
                .build();

        SyncReader reader = client.createSyncReader(readerSettings);
        reader.initAndWait();

        while (!queueOfMessages.get(topicName).isEmpty()) {
            byte[][] testMessages = queueOfMessages.get(topicName).poll();

            Assert.assertNotNull(testMessages);
            for (byte[] bytes : testMessages) {
                tech.ydb.topic.read.Message msg = reader.receive(1, TimeUnit.SECONDS);
                Assert.assertNotNull(msg);
                Assert.assertArrayEquals(bytes, msg.getData());
            }
        }
        reader.shutdown();
    }

    private void readDataFail(String topicName, TopicClient client) throws InterruptedException {
        ReaderSettings readerSettings = ReaderSettings.newBuilder()
                .addTopic(TopicReadSettings.newBuilder().setPath(topicName).build())
                .setConsumerName(TEST_CONSUMER1)
                .build();

        SyncReader reader = client.createSyncReader(readerSettings);
        reader.initAndWait();

        while (!queueOfMessages.get(topicName).isEmpty()) {
            byte[][] testMessages = queueOfMessages.get(topicName).poll();
            Assert.assertNotNull(testMessages);
            for (byte[] bytes : testMessages) {
                tech.ydb.topic.read.Message msg = reader.receive(1, TimeUnit.SECONDS);
                if (bytes.length != 0 && // nothing to decode
                        msg != null) // uncatch error has happened and that is what we want
                {
                    Assert.assertFalse(java.util.Arrays.equals(bytes, msg.getData()));
                }
            }
        }

        reader.shutdown();
    }

    private void readDataWithError(String topicName, TopicClient client) throws InterruptedException {
        ReaderSettings readerSettings = ReaderSettings.newBuilder()
                .addTopic(TopicReadSettings.newBuilder().setPath(topicName).build())
                .setConsumerName(TEST_CONSUMER1)
                .build();

        SyncReader reader = client.createSyncReader(readerSettings);
        reader.initAndWait();

        while (!queueOfMessages.get(topicName).isEmpty()) {
            byte[][] testMessages = queueOfMessages.get(topicName).poll();
            Assert.assertNotNull(testMessages);
            for (byte[] bytes : testMessages) {
                tech.ydb.topic.read.Message msg = reader.receive(1, TimeUnit.SECONDS);
                if (bytes.length != 0 && // nothing to decode
                        msg != null) // uncatch error has happened and that is what we want
                {
                    Assert.assertThrows(DecompressionException.class, msg::getData);
                }
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
        public InputStream decode(ByteArrayInputStream byteArrayOutputStream) {
            final ByteArrayInputStream outputStream = byteArrayOutputStream;
            return new InputStream() {
                @Override
                public int read() {
                    for (int i = 0; i < stub; i++) {
                        outputStream.read();
                    }

                    return outputStream.read();
                }
            };
        }

        @Override
        public OutputStream encode(ByteArrayOutputStream byteArrayOutputStream) {
            return new OutputStream() {
                @Override
                public void write(int b) {
                    for (int i = 0; i < stub; i++) {
                        byteArrayOutputStream.write(stub);
                    }
                    byteArrayOutputStream.write(b);
                }
            };
        }
    }
}
