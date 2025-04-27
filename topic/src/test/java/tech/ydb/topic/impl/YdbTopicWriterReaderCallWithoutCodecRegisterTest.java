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
import tech.ydb.topic.description.Consumer;
import tech.ydb.topic.description.CustomTopicCodec;
import tech.ydb.topic.read.events.AbstractReadEventHandler;
import tech.ydb.topic.read.events.DataReceivedEvent;
import tech.ydb.topic.read.impl.AsyncReaderImpl;
import tech.ydb.topic.read.impl.SyncReaderImpl;
import tech.ydb.topic.settings.CreateTopicSettings;
import tech.ydb.topic.settings.ReadEventHandlersSettings;
import tech.ydb.topic.settings.ReaderSettings;
import tech.ydb.topic.settings.TopicReadSettings;
import tech.ydb.topic.settings.WriterSettings;
import tech.ydb.topic.write.AsyncWriter;
import tech.ydb.topic.write.Message;
import tech.ydb.topic.write.QueueOverflowException;
import tech.ydb.topic.write.SyncWriter;
import tech.ydb.topic.write.WriteAck;
import tech.ydb.topic.write.impl.AsyncWriterImpl;
import tech.ydb.topic.write.impl.SyncWriterImpl;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Specific test connected to deprecated constructor
 * We check that old constructor work with predefined codec and doesn't work with custom codec
 * even we specify custom codec
 */
public class YdbTopicWriterReaderCallWithoutCodecRegisterTest {

    private final static Logger logger = LoggerFactory.getLogger(YdbTopicWriterReaderCallWithoutCodecRegisterTest.class);

    @ClassRule
    public final static GrpcTransportRule ydbTransport = new GrpcTransportRule();

    private final static String TEST_TOPIC1 = "integration_test_custom_codec_without_topic1";
    private final static String TEST_CONSUMER1 = "consumer_old_rw";
    private final static String TEST_CONSUMER2 = "other_consumer_old_rw";

    private final List<String> topicToDelete = new ArrayList<>();
    private final List<TopicClient> clientToClose = new ArrayList<>();

    TopicClient client1;
    WriterSettings settings;
    ExecutorService executors;
    GrpcTopicRpc topicRpc;
    CustomTopicCodec codec1;
    ReaderSettings readerSettings;

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

        topicRpc = GrpcTopicRpc.useTransport(ydbTransport);
        client1 = createClient(topicRpc);
        createTopic(client1);

        settings = WriterSettings.newBuilder()
                .setTopicPath(TEST_TOPIC1)
                .setCodec(2)
                .build();

        executors = Executors.newFixedThreadPool(5);

        codec1 = new YdbTopicsCodecIntegrationTest.CustomCustomTopicCode(1);

        readerSettings = ReaderSettings.newBuilder()
                .addTopic(TopicReadSettings.newBuilder().setPath(TEST_TOPIC1).build())
                .setConsumerName(TEST_CONSUMER1)
                .build();

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

        executors.shutdown();
    }


    /**
     * Check that old constructor SyncWriterImpl can write predefined codec and can't use custom codec
     */
    @Test
    public void syncWriterImplWithUnModifiableRegistry() throws ExecutionException, InterruptedException, TimeoutException {
        SyncWriterImpl syncWriter = new SyncWriterImpl(topicRpc, settings, executors);
        writeDataSyncWriter(2, syncWriter);

        settings = WriterSettings.newBuilder()
                .setTopicPath(TEST_TOPIC1)
                .setCodec(10013)
                .build();

        client1.registerCodec(10113, codec1);
        SyncWriterImpl finalSyncWriter = new SyncWriterImpl(topicRpc, settings, executors);

        Assert.assertThrows(Exception.class, () -> writeDataSyncWriter(10113, finalSyncWriter));
    }

    /**
     * Check that old constructor AsyncWriterImpl can write predefined codec and can't use custom codec
     */
    @Test
    public void asyncWriterImplWithUnModifiableRegistry() throws QueueOverflowException {
        AsyncWriterImpl asyncWriter = new AsyncWriterImpl(topicRpc, settings, executors);
        writeDataAsyncWriter(2, asyncWriter);

        settings = WriterSettings.newBuilder()
                .setTopicPath(TEST_TOPIC1)
                .setCodec(10013)
                .build();

        AsyncWriterImpl finalAsyncWriter = new AsyncWriterImpl(topicRpc, settings, executors);

        Assert.assertThrows(Exception.class, () -> writeDataAsyncWriter(10113, finalAsyncWriter));
    }

    /**
     * Check that old constructor AsyncReaderImpl failed with custom codec
     */
    @Test
    public void asyncReaderImplWithUnModifiableRegistryWithCustomCodec() throws QueueOverflowException, ExecutionException, InterruptedException, TimeoutException {
        client1.registerCodec(10113, codec1);

        writeData(10113, client1);

        final CompletableFuture<Void> wait = new CompletableFuture<>();
        AsyncReaderImpl asyncReaderImpl = new AsyncReaderImpl(topicRpc, readerSettings, ReadEventHandlersSettings.newBuilder()
                .setEventHandler(new AbstractReadEventHandler() {
                    @Override
                    public void onMessages(DataReceivedEvent dre) {
                        // With custom codec we should fail and not pass this code
                        wait.complete(null);
                    }

                }).
                build());

        // wait for 3 seconds
        Assert.assertTrue(asyncReadData(asyncReaderImpl, wait));
        Assert.assertFalse(wait.isDone());
    }

    /**
     * Check that old constructor AsyncReaderImpl read with predefined codec
     */
    @Test
    public void asyncReaderImplWithUnModifiableRegistryWithStandartCodec() throws QueueOverflowException, ExecutionException, InterruptedException, TimeoutException {
        writeData(2, client1);

        final CompletableFuture<Void> wait = new CompletableFuture<>();
        AsyncReaderImpl asyncReaderImpl = new AsyncReaderImpl(topicRpc, readerSettings, ReadEventHandlersSettings.newBuilder()
                .setEventHandler(new AbstractReadEventHandler() {
                    @Override
                    public void onMessages(DataReceivedEvent dre) {
                        int count = 0;
                        byte[][] testMessages = queueOfMessages.get(TEST_TOPIC1).poll();
                        Assert.assertNotNull(testMessages);

                        for (tech.ydb.topic.read.Message msg : dre.getMessages()) {
                            Assert.assertNotNull(msg);
                            Assert.assertArrayEquals(testMessages[count], msg.getData());
                            count++;
                        }

                        if (count == testMessages.length) {
                            wait.complete(null);
                        }
                    }
                }).
                build());

        asyncReadData(asyncReaderImpl, wait);
    }

    /**
     * Check that old constructor SyncReaderImpl failed with custom codec
     */
    @Test
    public void syncReaderImplWithUnModifiableRegistryWithCustomCodec() throws ExecutionException, InterruptedException, TimeoutException {
        client1.registerCodec(10113, codec1);

        writeData(10113, client1);

        SyncReaderImpl syncReaderImpl = new SyncReaderImpl(topicRpc, readerSettings);

        syncReadDataFailed(syncReaderImpl);
    }

    /**
     * Check that old constructor SyncReaderImpl read with predefined codec
     */
    @Test
    public void syncReaderImplWithUnModifiableRegistryWithStandartCodec() throws ExecutionException, InterruptedException, TimeoutException {
        writeData(2, client1);

        SyncReaderImpl syncReaderImpl = new SyncReaderImpl(topicRpc, readerSettings);

        syncReadData(syncReaderImpl);
    }

    private void deleteTopic(String topicName) {
        logger.info("Drop test topic {} ...", topicName);
        Status dropStatus = client1.dropTopic(topicName).join();
        client1.close();
        dropStatus.expectSuccess("can't drop test topic");
    }

    TopicClient createClient(GrpcTopicRpc topicRpc) {
        client1 = TopicClientImpl.newClient(topicRpc).build();
        clientToClose.add(client1);
        return client1;
    }

    private void writeDataSyncWriter(int codecId, SyncWriter writer) throws ExecutionException, InterruptedException, TimeoutException {
        byte[][] testMessages = new byte[][]{
                (TEST_MESSAGES[0] + codecId).getBytes(),
                TEST_MESSAGES[1].getBytes(),
                TEST_MESSAGES[2].getBytes(),
                (TEST_MESSAGES[3] + codecId).getBytes(),
                (TEST_MESSAGES[4] + codecId).getBytes(),
        };

        writer.init();

        Deque<byte[][]> deque = queueOfMessages.computeIfAbsent(TEST_TOPIC1, k -> new ArrayDeque<>());
        deque.add(testMessages);

        for (byte[] testMessage : testMessages) {
            writer.send(Message.newBuilder().setData(testMessage).build());
        }

        writer.flush();
        writer.shutdown(1, TimeUnit.MINUTES);
    }

    private void writeDataAsyncWriter(int codecId, AsyncWriter writer) throws QueueOverflowException {
        byte[][] testMessages = new byte[][]{
                (TEST_MESSAGES[0] + codecId).getBytes(),
                TEST_MESSAGES[1].getBytes(),
                TEST_MESSAGES[2].getBytes(),
                (TEST_MESSAGES[3] + codecId).getBytes(),
                (TEST_MESSAGES[4] + codecId).getBytes(),
        };

        writer.init();

        Deque<byte[][]> deque = queueOfMessages.computeIfAbsent(YdbTopicWriterReaderCallWithoutCodecRegisterTest.TEST_TOPIC1, k -> new ArrayDeque<>());
        deque.add(testMessages);

        List<CompletableFuture<WriteAck>> futures = new ArrayList<>();
        for (byte[] testMessage : testMessages) {
            CompletableFuture<WriteAck> future = writer.send(Message.newBuilder().setData(testMessage).build());
            futures.add(future);
        }

        futures.forEach(CompletableFuture::join);

        writer.shutdown();
    }

    private void createTopic(TopicClient client) {
        logger.info("Create test topic {} ...", TEST_TOPIC1);

        client.createTopic(TEST_TOPIC1, CreateTopicSettings.newBuilder()
                .addConsumer(Consumer.newBuilder().setName(TEST_CONSUMER1).build())
                .addConsumer(Consumer.newBuilder().setName(TEST_CONSUMER2).build())
                .build()
        ).join().expectSuccess("can't create a new topic");

        topicToDelete.add(TEST_TOPIC1);
    }

    private void syncReadDataFailed(SyncReaderImpl reader) throws InterruptedException {
        reader.initAndWait();

        while (!queueOfMessages.get(TEST_TOPIC1).isEmpty()) {
            byte[][] testMessages = queueOfMessages.get(TEST_TOPIC1).poll();

            Assert.assertNotNull(testMessages);
            for (byte[] bytes : testMessages) {
                tech.ydb.topic.read.Message msg = reader.receive(1, TimeUnit.SECONDS);
                Assert.assertNull(msg);
            }
        }
        reader.shutdown();
    }

    private void syncReadData(SyncReaderImpl reader) throws InterruptedException {
        reader.initAndWait();

        while (!queueOfMessages.get(TEST_TOPIC1).isEmpty()) {
            byte[][] testMessages = queueOfMessages.get(TEST_TOPIC1).poll();

            Assert.assertNotNull(testMessages);
            for (byte[] bytes : testMessages) {
                tech.ydb.topic.read.Message msg = reader.receive(1, TimeUnit.SECONDS);
                Assert.assertNotNull(msg);
                Assert.assertArrayEquals(bytes, msg.getData());
            }
        }
        reader.shutdown();
    }

    private boolean asyncReadData(AsyncReaderImpl reader, CompletableFuture<Void> wait) throws InterruptedException {
        reader.init().join();
        boolean waitNotGetData = false;
        try {
            wait.get(3, TimeUnit.SECONDS);
        } catch (TimeoutException | ExecutionException e) {
            waitNotGetData = true;
        }

        reader.shutdown().join();
        return waitNotGetData;
    }

    private void writeData(int codecId, TopicClient client) throws ExecutionException, InterruptedException, TimeoutException {
        byte[][] testMessages = new byte[][]{
                (TEST_MESSAGES[0] + codecId).getBytes(),
                TEST_MESSAGES[1].getBytes(),
                TEST_MESSAGES[2].getBytes(),
                (TEST_MESSAGES[3] + codecId).getBytes(),
                (TEST_MESSAGES[4] + codecId).getBytes(),
        };

        writeData(codecId, client, testMessages);
    }

    private void writeData(int codecId, TopicClient client, byte[][] testMessages) throws ExecutionException, InterruptedException, TimeoutException {
        WriterSettings settings = WriterSettings.newBuilder()
                .setTopicPath(TEST_TOPIC1)
                .setCodec(codecId)
                .build();
        SyncWriter writer = client.createSyncWriter(settings);
        writeData(writer, testMessages);
    }

    private void writeData(SyncWriter writer, byte[][] testMessages) throws ExecutionException, InterruptedException, TimeoutException {
        writer.init();

        Deque<byte[][]> deque = queueOfMessages.computeIfAbsent(TEST_TOPIC1, k -> new ArrayDeque<>());
        deque.add(testMessages);

        for (byte[] testMessage : testMessages) {
            writer.send(Message.newBuilder().setData(testMessage).build());
        }

        writer.flush();
        writer.shutdown(1, TimeUnit.MINUTES);
    }
}
