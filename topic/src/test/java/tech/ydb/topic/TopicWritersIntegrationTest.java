package tech.ydb.topic;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tech.ydb.common.transaction.TxMode;
import tech.ydb.core.Status;
import tech.ydb.core.StatusCode;
import tech.ydb.core.UnexpectedResultException;
import tech.ydb.core.utils.FutureTools;
import tech.ydb.table.Session;
import tech.ydb.table.TableClient;
import tech.ydb.table.transaction.TableTransaction;
import tech.ydb.test.junit4.GrpcTransportRule;
import tech.ydb.topic.description.Consumer;
import tech.ydb.topic.read.SyncReader;
import tech.ydb.topic.settings.AlterPartitioningSettings;
import tech.ydb.topic.settings.AlterTopicSettings;
import tech.ydb.topic.settings.AutoPartitioningStrategy;
import tech.ydb.topic.settings.CreateTopicSettings;
import tech.ydb.topic.settings.PartitioningSettings;
import tech.ydb.topic.settings.ReaderSettings;
import tech.ydb.topic.settings.SendSettings;
import tech.ydb.topic.settings.TopicReadSettings;
import tech.ydb.topic.settings.TopicRetryConfig;
import tech.ydb.topic.settings.WriterSettings;
import tech.ydb.topic.utils.ErrorsHandler;
import tech.ydb.topic.write.AsyncWriter;
import tech.ydb.topic.write.InitResult;
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

    private final static FailableWriterInterceptor PROXY = new FailableWriterInterceptor();

    @ClassRule
    public final static GrpcTransportRule ydbTransport = new GrpcTransportRule()
            .withGrpcTransportCustomizer(b -> b.addChannelInitializer(PROXY));

    private final static String ONE_PART_TOPIC = "writers_test_topic_one";
    private final static String AUTO_PART_TOPIC = "writers_test_topic_auto_parts";

    private final static String TEST_PRODUCER = "producer";
    private final static String TEST_CONSUMER = "consumer";

    private static TopicClient client;

    private final List<String> topicsToDrop = new ArrayList<>();

    @BeforeClass
    public static void initClient() {
        client = TopicClient.newClient(ydbTransport).build();
    }

    @AfterClass
    public static void closeClient() {
        client.close();
    }

    @Before
    public void resetProxy() {
        PROXY.reset();
    }

    @After
    public void dropTopics() {
        Iterator<String> it = topicsToDrop.iterator();
        while (it.hasNext()) {
            String topicPath = it.next();
            logger.info("Drop test topic {} ...", topicPath);
            client.dropTopic(topicPath).join();
            it.remove();
        }
    }

    private void createTopicWithOnePartition() {
        logger.info("Create test topic  {} ...", ONE_PART_TOPIC);
        client.createTopic(ONE_PART_TOPIC, CreateTopicSettings.newBuilder()
                .addConsumer(Consumer.newBuilder().setName(TEST_CONSUMER).build())
                .setPartitioningSettings(PartitioningSettings.newBuilder()
                        .setMaxActivePartitions(1)
                        .setMinActivePartitions(1)
                        .build())
                .build())
                .join().expectSuccess("can't create a new topic");
        topicsToDrop.add(ONE_PART_TOPIC);
    }

    private void createAutoPartitionedTopic() {
        client.createTopic(AUTO_PART_TOPIC, CreateTopicSettings.newBuilder()
                .addConsumer(Consumer.newBuilder().setName(TEST_CONSUMER).build())
                .setPartitioningSettings(PartitioningSettings.newBuilder()
                        .setAutoPartitioningStrategy(AutoPartitioningStrategy.PAUSED)
                        .setMaxActivePartitions(1)
                        .setMinActivePartitions(1)
                        .build())
                .build())
                .join().expectSuccess("can't create an auto partitioned topic");
        topicsToDrop.add(AUTO_PART_TOPIC);
    }

    private void splitAutoPartitionedTopic() {
        client.alterTopic(AUTO_PART_TOPIC, AlterTopicSettings.newBuilder()
                .setAlterPartitioningSettings(AlterPartitioningSettings.newBuilder()
                        .setMaxActivePartitions(2)
                        .setMinActivePartitions(2)
                        .build())
                .build())
                .join().expectSuccess("can't alter the auto partitioned topic");
    }

    private void assertTopicContent(String topicPath, List<byte[]> messages) {
        try {
            SyncReader reader = client.createSyncReader(ReaderSettings.newBuilder().addTopic(
                    TopicReadSettings.newBuilder().setPath(topicPath).build()
            ).setConsumerName(TEST_CONSUMER).build());

            reader.init();
            int idx = 0;
            for (byte[] expected: messages) {
                tech.ydb.topic.read.Message next = reader.receive(1, TimeUnit.SECONDS);
                Assert.assertNotNull("Expected message " + idx, next);
                Assert.assertArrayEquals("Unexpected content for message " + idx, expected, next.getData());
                idx++;

                next.commit();
            }

            reader.shutdown();
        } catch (InterruptedException ex) {
            throw new AssertionError("Unexpected exception", ex);
        }
    }

    @Test
    public void messageBufferOverflowTest() throws Exception {
        createTopicWithOnePartition();

        WriterSettings settings = WriterSettings.newBuilder()
                .setTopicPath(ONE_PART_TOPIC)
                .setProducerId(TEST_PRODUCER)
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
        writer.send(Message.of(msg2)); // this message is more than buffer limit
        writer.send(Message.of(msg2)); // this message is more than buffer limit
        writer.send(Message.of(msg2)); // this message is more than buffer limit
        writer.send(Message.of(msg1));
        writer.send(Message.of(msg1));

        writer.flush();
        writer.shutdown(10, TimeUnit.SECONDS);

        assertTopicContent(ONE_PART_TOPIC, Arrays.asList(msg1, msg1, msg1, msg2, msg1, msg2, msg2, msg2, msg1, msg1));
    }

    @Test
    public void lazyInitTest() throws Exception {
        createTopicWithOnePartition();

        WriterSettings settings = WriterSettings.newBuilder()
                .setTopicPath(ONE_PART_TOPIC)
                .setProducerId(TEST_PRODUCER)
                .build();

        AsyncWriter writer = client.createAsyncWriter(settings);

        CountDownLatch latch = new CountDownLatch(1);
        List<byte[]> written = new ArrayList<>();
        CompletableFuture<WriteAck> lastMessage = CompletableFuture.supplyAsync(() -> {
            ThreadLocalRandom rnd = ThreadLocalRandom.current();
            try {
                CompletableFuture<WriteAck> ack = FutureTools.failedFuture(new RuntimeException("not started"));
                for (int idx = 0; idx < 100; idx++) {
                    byte[] msg = new byte[1000];
                    rnd.nextBytes(msg);
                    ack = writer.send(Message.of(msg));
                    written.add(msg);
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

        assertTopicContent(ONE_PART_TOPIC, written);
    }

    @Test
    public void doubleInitTest() throws Exception {
        createTopicWithOnePartition();

        WriterSettings settings = WriterSettings.newBuilder()
                .setTopicPath(ONE_PART_TOPIC)
                .setProducerId(TEST_PRODUCER)
                .build();

        AsyncWriter writer = client.createAsyncWriter(settings);

        writer.init();
        writer.init();

        byte[] msg = "hello".getBytes();
        writer.send(Message.of(msg)).join();

        writer.shutdown().join();

        assertTopicContent(ONE_PART_TOPIC, Collections.singletonList(msg));
    }

    @Test
    public void defaultRetryPolicyWriter() throws Exception {
        createTopicWithOnePartition();

        // errors pattern in order of processing
        PROXY.unavailableOnAckWithSeqNo(15);
        PROXY.badRequestOnInit(2);
        PROXY.badSessionOnSendMsgWithSeqNo(35);
        PROXY.unavailableOnInit(4);
        PROXY.unavailableOnInit(5);
        PROXY.unavailableOnInit(6);
        PROXY.badRequestOnAckWithSeqNo(60);
        PROXY.unavailableOnAckWithSeqNo(90);

        StatusCode[] expectedErrors = new StatusCode[] {
                StatusCode.TRANSPORT_UNAVAILABLE,
                StatusCode.BAD_REQUEST,
                StatusCode.BAD_SESSION,
                StatusCode.TRANSPORT_UNAVAILABLE,
                StatusCode.TRANSPORT_UNAVAILABLE,
                StatusCode.TRANSPORT_UNAVAILABLE,
                StatusCode.BAD_REQUEST,
                StatusCode.TRANSPORT_UNAVAILABLE
        };

        ErrorsHandler errorsHolder = new ErrorsHandler();
        WriterSettings settings = WriterSettings.newBuilder()
                .setTopicPath(ONE_PART_TOPIC)
                .setProducerId(TEST_PRODUCER)
                .setErrorsHandler(errorsHolder)
                .build();

        SyncWriter writer = client.createSyncWriter(settings);
        writer.initAndWait();

        List<byte[]> written = new ArrayList<>();
        for (int batch = 0; batch < 10; batch++) {
            for (int idx = 0; idx < 10; idx++) {
                byte[] msg = new byte[1000];
                Arrays.fill(msg, (byte) (batch * 10 + idx));
                writer.send(Message.of(msg), 1, TimeUnit.MINUTES);
                written.add(msg);
            }
            writer.flush();
        }

        writer.shutdown(10, TimeUnit.SECONDS);

        errorsHolder.assertCodes(expectedErrors);
        assertTopicContent(ONE_PART_TOPIC, written);
    }

    @Test
    public void sameProducerConflictTest() throws Exception {
        createTopicWithOnePartition();

        CountDownLatch closed = new CountDownLatch(1);
        List<Status> errors = new ArrayList<>();

        WriterSettings settings = WriterSettings.newBuilder()
                .setTopicPath(ONE_PART_TOPIC)
                .setProducerId(TEST_PRODUCER)
                .setRetryConfig(TopicRetryConfig.STANDARD)
                .setErrorsHandler((status, th) -> {
                    errors.add(status);
                    closed.countDown();
                })
                .build();

        SyncWriter writer1 = client.createSyncWriter(settings);
        writer1.initAndWait();

        byte[] msg1 = new byte[1000];
        byte[] msg2 = new byte[1001];
        Arrays.fill(msg1, (byte) 0x10);
        Arrays.fill(msg2, (byte) 0x11);

        writer1.send(Message.of(msg1));
        writer1.send(Message.of(msg2));
        writer1.flush();

        SyncWriter writer2 = client.createSyncWriter(settings);
        writer2.initAndWait();

        writer2.send(Message.of(msg1));
        writer2.send(Message.of(msg2));
        writer2.flush();

        Assert.assertTrue(closed.await(1, TimeUnit.MINUTES)); // wait to retry writer1

        Assert.assertFalse(errors.isEmpty());
        Assert.assertEquals(StatusCode.SESSION_EXPIRED, errors.get(0).getCode());

        writer1.flush(); // no IllegalStateException
        writer1.shutdown(10, TimeUnit.SECONDS);  // no IllegalStateException
        writer2.shutdown(10, TimeUnit.SECONDS);
    }

    @Test
    public void writeWithSplitsTest() throws Exception {
        createAutoPartitionedTopic();

        WriterSettings settings = WriterSettings.newBuilder()
                .setTopicPath(AUTO_PART_TOPIC)
                .setProducerId(TEST_PRODUCER)
                .setRetryConfig(TopicRetryConfig.STANDARD)
                .build();

        SyncWriter writer = client.createSyncWriter(settings);
        writer.init();

        byte[] msg1 = new byte[1000];
        byte[] msg2 = new byte[1001];
        byte[] msg3 = new byte[1002];
        byte[] msg4 = new byte[1003];
        Arrays.fill(msg1, (byte) 0x10);
        Arrays.fill(msg2, (byte) 0x11);
        Arrays.fill(msg3, (byte) 0x12);
        Arrays.fill(msg4, (byte) 0x13);

        writer.send(Message.of(msg1));
        writer.send(Message.of(msg2));
        writer.flush();

        splitAutoPartitionedTopic();

        writer.send(Message.of(msg3));
        writer.send(Message.of(msg4));
        writer.flush();

        writer.shutdown(10, TimeUnit.SECONDS);

        assertTopicContent(AUTO_PART_TOPIC, Arrays.asList(msg1, msg2, msg3, msg4));
    }

    @Test
    public void idempotentWriterTest() throws Exception {
        createTopicWithOnePartition();

        WriterSettings settings = WriterSettings.newBuilder()
                .setTopicPath(ONE_PART_TOPIC)
                .setProducerId(TEST_PRODUCER)
                .build();

        AsyncWriter writer1 = client.createAsyncWriter(settings);
        writer1.init().join();

        byte[] msg1 = new byte[1000];
        byte[] msg2 = new byte[1001];
        Arrays.fill(msg1, (byte) 0x10);
        Arrays.fill(msg2, (byte) 0x11);

        List<Long> order1 = new ArrayList<>();
        CompletableFuture<WriteAck> ack1 = writer1.send(Message.newBuilder().setData(msg1).setSeqNo(10).build())
                .whenComplete((ack, th) -> order1.add(ack.getSeqNo()));
        CompletableFuture<WriteAck> ack2 = writer1.send(Message.newBuilder().setData(msg2).setSeqNo(50).build())
                .whenComplete((ack, th) -> order1.add(ack.getSeqNo()));
        Exception ex1 = Assert.assertThrows(IllegalArgumentException.class,
                () -> writer1.send(Message.newBuilder().setData(msg2).setSeqNo(40).build())
        );
        Assert.assertEquals("SeqNo provided for a message is less or equal than SeqNo provided for previous message."
                + " SeqNo must be strictly growing.", ex1.getMessage());

        Assert.assertEquals(WriteAck.State.WRITTEN, ack1.join().getState());
        Assert.assertEquals(WriteAck.State.WRITTEN, ack2.join().getState());
        Assert.assertEquals(10, ack1.join().getSeqNo());
        Assert.assertEquals(50, ack2.join().getSeqNo());

        Assert.assertEquals(Arrays.asList(10L, 50L), order1);

        writer1.shutdown().join();

        AsyncWriter writer2 = client.createAsyncWriter(settings);

        List<Long> order2 = new ArrayList<>();
        CompletableFuture<WriteAck> ack4 = writer2.send(Message.newBuilder().setData(msg1).setSeqNo(10).build())
                .whenComplete((ack, th) -> order2.add(ack.getSeqNo()));
        CompletableFuture<WriteAck> ack5 = writer2.send(Message.newBuilder().setData(msg2).setSeqNo(20).build())
                .whenComplete((ack, th) -> order2.add(ack.getSeqNo()));
        writer2.init().join();
        CompletableFuture<WriteAck> ack6 = writer2.send(Message.newBuilder().setData(msg2).setSeqNo(40).build())
                .whenComplete((ack, th) -> order2.add(ack.getSeqNo()));
        Exception ex2 = Assert.assertThrows(IllegalArgumentException.class,
                () -> writer2.send(Message.newBuilder().setData(msg2).setSeqNo(30).build())
        );
        Assert.assertEquals("SeqNo provided for a message is less or equal than SeqNo provided for previous message."
                + " SeqNo must be strictly growing.", ex2.getMessage());
        CompletableFuture<WriteAck> ack7 = writer2.send(Message.newBuilder().setData(msg1).setSeqNo(60).build())
                .whenComplete((ack, th) -> order2.add(ack.getSeqNo()));

        Assert.assertEquals(WriteAck.State.ALREADY_WRITTEN, ack4.join().getState());
        Assert.assertEquals(WriteAck.State.ALREADY_WRITTEN, ack5.join().getState());
        Assert.assertEquals(WriteAck.State.ALREADY_WRITTEN, ack6.join().getState());
        Assert.assertEquals(WriteAck.State.WRITTEN, ack7.join().getState());
        Assert.assertEquals(10, ack4.join().getSeqNo());
        Assert.assertEquals(20, ack5.join().getSeqNo());
        Assert.assertEquals(40, ack6.join().getSeqNo());
        Assert.assertEquals(60, ack7.join().getSeqNo());

        Assert.assertEquals(Arrays.asList(10L, 20L, 40L, 60L), order2);

        writer2.shutdown().join();
    }

    @Test
    public void wrongDirectWriteTest() throws Exception {
        createTopicWithOnePartition();

        CountDownLatch closed = new CountDownLatch(1);

        WriterSettings settings = WriterSettings.newBuilder()
                .setTopicPath(ONE_PART_TOPIC)
                .setDirectWrite(true)
                .setPartitionId(1) // Invalid partition
                .setRetryConfig(TopicRetryConfig.STANDARD)
                .setErrorsHandler((t, u) -> { closed.countDown(); })
                .build();

        AsyncWriter writer = client.createAsyncWriter(settings);
        CompletableFuture<WriteAck> f1 = writer.send(Message.of(new byte[] { 0x00 }));
        CompletableFuture<InitResult> f2 = writer.init();

        Assert.assertTrue(closed.await(5, TimeUnit.SECONDS));

        CompletableFuture<Void> f3 = writer.shutdown();

        Assert.assertTrue(f1.isCompletedExceptionally());
        Assert.assertTrue(f2.isCompletedExceptionally());
        Assert.assertFalse(f3.isCompletedExceptionally());

        Exception ex1 = Assert.assertThrows(CompletionException.class, f1::join);
        Exception ex2 = Assert.assertThrows(CompletionException.class, f2::join);

        Assert.assertTrue(ex1.getCause() instanceof RuntimeException);
        Assert.assertTrue(ex2.getCause() instanceof UnexpectedResultException);

        String reason = "Cannot find partition 1 (S_ERROR)";
        Assert.assertEquals(
                "Message sending was cancelled with Status{code = BAD_REQUEST(code=400010), issues = [" + reason + "]}",
                ex1.getCause().getMessage()
        );
        Assert.assertEquals(
                "Cannot init write session, code: BAD_REQUEST, issues: [" + reason + "]",
                ex2.getCause().getMessage()
        );
    }

    @Test
    public void txWriteTest() throws Exception {
        createTopicWithOnePartition();

        WriterSettings settings = WriterSettings.newBuilder()
                .setTopicPath(ONE_PART_TOPIC)
                .setProducerId(TEST_PRODUCER)
                .setRetryConfig(TopicRetryConfig.STANDARD)
                .build();

        byte[] msg1 = new byte[1000];
        byte[] msg2 = new byte[1001];
        byte[] msg3 = new byte[1002];
        byte[] msg4 = new byte[1003];
        Arrays.fill(msg1, (byte) 0x10);
        Arrays.fill(msg2, (byte) 0x11);
        Arrays.fill(msg3, (byte) 0x50);
        Arrays.fill(msg4, (byte) 0x60);

        SyncWriter writer = client.createSyncWriter(settings);
        writer.initAndWait();

        try (TableClient table = TableClient.newClient(ydbTransport).build();
                Session s1 = table.createSession(Duration.ofSeconds(5)).join().getValue();
                Session s2 = table.createSession(Duration.ofSeconds(5)).join().getValue()) {
            TableTransaction tx1 = s1.beginTransaction(TxMode.SERIALIZABLE_RW).join().getValue();
            TableTransaction tx2 = s2.beginTransaction(TxMode.SERIALIZABLE_RW).join().getValue();

            SendSettings ss1 = SendSettings.newBuilder().setTransaction(tx1).build();
            SendSettings ss2 = SendSettings.newBuilder().setTransaction(tx2).build();

            writer.send(Message.newBuilder().setData(msg1).setSeqNo(1).build(), ss1);
            writer.send(Message.newBuilder().setData(msg2).setSeqNo(2).build(), ss2);
            writer.send(Message.newBuilder().setData(msg3).setSeqNo(3).build(), ss1);
            writer.send(Message.newBuilder().setData(msg4).setSeqNo(4).build(), ss2);

            writer.flush();

            tx2.commit().join().expectSuccess();
            Assert.assertEquals(StatusCode.ABORTED, tx1.commit().join().getCode());

            writer.send(Message.newBuilder().setData(msg1).setSeqNo(5).build());
            writer.send(Message.newBuilder().setData(msg2).setSeqNo(6).build());

            writer.flush();

            TableTransaction tx3 = s2.beginTransaction(TxMode.SERIALIZABLE_RW).join().getValue();
            SendSettings ss3 = SendSettings.newBuilder().setTransaction(tx3).build();

            writer.send(Message.newBuilder().setData(msg3).setSeqNo(7).build(), ss3);
            writer.send(Message.newBuilder().setData(msg4).setSeqNo(8).build(), ss3);

            writer.flush();

            tx3.commit().join().expectSuccess();
            writer.shutdown(1, TimeUnit.SECONDS);
        }

        assertTopicContent(ONE_PART_TOPIC, Arrays.asList(msg2, msg4, msg1, msg2, msg3, msg4));
    }

    @Test
    public void txWriteWithSplitsTest() throws Exception {
        createAutoPartitionedTopic();

        WriterSettings settings = WriterSettings.newBuilder()
                .setTopicPath(AUTO_PART_TOPIC)
                .setProducerId(TEST_PRODUCER)
                .setRetryConfig(TopicRetryConfig.STANDARD)
                .build();

        byte[] msg1 = new byte[1000];
        byte[] msg2 = new byte[1001];
        byte[] msg3 = new byte[1002];
        byte[] msg4 = new byte[1003];
        Arrays.fill(msg1, (byte) 0x10);
        Arrays.fill(msg2, (byte) 0x11);
        Arrays.fill(msg3, (byte) 0x12);
        Arrays.fill(msg4, (byte) 0x13);

        SyncWriter writer = client.createSyncWriter(settings);
        writer.init();

        try (TableClient table = TableClient.newClient(ydbTransport).build();
                Session s1 = table.createSession(Duration.ofSeconds(5)).join().getValue()) {

            TableTransaction tx1 = s1.beginTransaction(TxMode.SERIALIZABLE_RW).join().getValue();
            SendSettings ss1 = SendSettings.newBuilder().setTransaction(tx1).build();
            writer.send(Message.of(msg1), ss1);
            writer.send(Message.of(msg2), ss1);
            writer.flush();
            tx1.commit().join().expectSuccess();

            splitAutoPartitionedTopic();

            TableTransaction tx2 = s1.beginTransaction(TxMode.SERIALIZABLE_RW).join().getValue();
            SendSettings ss2 = SendSettings.newBuilder().setTransaction(tx2).build();
            writer.send(Message.of(msg3), ss2);
            writer.send(Message.of(msg4), ss2);
            writer.flush();
            Assert.assertEquals(StatusCode.ABORTED, tx2.commit().join().getCode());

            TableTransaction tx3 = s1.beginTransaction(TxMode.SERIALIZABLE_RW).join().getValue();
            SendSettings ss3 = SendSettings.newBuilder().setTransaction(tx3).build();

            writer.send(Message.of(msg4), ss3);
            writer.send(Message.of(msg1), ss3);
            writer.flush();
            tx3.commit().join().expectSuccess();
        } finally {
            writer.shutdown(10, TimeUnit.SECONDS);
        }

        assertTopicContent(AUTO_PART_TOPIC, Arrays.asList(msg1, msg2, msg4, msg1));
    }

    @Test
    public void invalidTxWriteTest() throws Exception {
        createTopicWithOnePartition();

        ErrorsHandler errorsHolder = new ErrorsHandler();
        WriterSettings settings = WriterSettings.newBuilder()
                .setTopicPath(ONE_PART_TOPIC)
                .setProducerId(TEST_PRODUCER)
                .setRetryConfig(TopicRetryConfig.STANDARD)
                .setErrorsHandler(errorsHolder)
                .build();

        byte[] msg1 = new byte[1000];
        byte[] msg2 = new byte[1001];
        byte[] msg3 = new byte[1002];
        byte[] msg4 = new byte[1003];
        Arrays.fill(msg1, (byte) 0x10);
        Arrays.fill(msg2, (byte) 0x11);
        Arrays.fill(msg3, (byte) 0x50);
        Arrays.fill(msg4, (byte) 0x60);

        SyncWriter writer = client.createSyncWriter(settings);
        writer.initAndWait();

        try (TableClient table = TableClient.newClient(ydbTransport).build();
                Session s1 = table.createSession(Duration.ofSeconds(5)).join().getValue();
                Session s2 = table.createSession(Duration.ofSeconds(5)).join().getValue()) {

            TableTransaction tx1 = s1.beginTransaction(TxMode.SERIALIZABLE_RW).join().getValue();
            TableTransaction tx2 = s2.beginTransaction(TxMode.SERIALIZABLE_RW).join().getValue();

            SendSettings ss1 = SendSettings.newBuilder().setTransaction(tx1).build();
            SendSettings ss2 = SendSettings.newBuilder().setTransaction(tx2).build();

            writer.send(Message.newBuilder().setData(msg1).setSeqNo(1).build(), ss1);
            writer.send(Message.newBuilder().setData(msg2).setSeqNo(2).build(), ss1);

            tx2.rollback().join();

            writer.send(Message.newBuilder().setData(msg3).setSeqNo(3).build(), ss2);
            writer.send(Message.newBuilder().setData(msg4).setSeqNo(4).build(), ss2);

            writer.flush();
            writer.shutdown(1, TimeUnit.SECONDS);

            tx1.commit().join().expectSuccess();
            errorsHolder.assertCodes(StatusCode.NOT_FOUND);
        }

        assertTopicContent(ONE_PART_TOPIC, Arrays.asList(msg1, msg2));
    }

    @Test
    public void txRetryWriteTest() throws Exception {
        createTopicWithOnePartition();

        PROXY.unavailableOnAckWithSeqNo(2);

        ErrorsHandler errorsHolder = new ErrorsHandler();
        WriterSettings settings = WriterSettings.newBuilder()
                .setTopicPath(ONE_PART_TOPIC)
                .setProducerId(TEST_PRODUCER)
                .setRetryConfig(TopicRetryConfig.STANDARD)
                .setErrorsHandler(errorsHolder)
                .build();

        byte[] msg1 = new byte[1000];
        byte[] msg2 = new byte[1001];
        Arrays.fill(msg1, (byte) 0x10);
        Arrays.fill(msg2, (byte) 0x11);

        AsyncWriter writer = client.createAsyncWriter(settings);
        writer.init().join();

        try (
                TableClient table = TableClient.newClient(ydbTransport).build();
                Session s1 = table.createSession(Duration.ofSeconds(5)).join().getValue()) {

            TableTransaction tx1 = s1.beginTransaction(TxMode.SERIALIZABLE_RW).join().getValue();
            SendSettings ss1 = SendSettings.newBuilder().setTransaction(tx1).build();

            writer.send(Message.newBuilder().setData(msg1).setSeqNo(1).build(), ss1);
            CompletableFuture<WriteAck> ack2 = writer.send(Message.newBuilder().setData(msg2).setSeqNo(2).build(), ss1);

            Assert.assertEquals(WriteAck.State.ALREADY_WRITTEN, ack2.join().getState());

            tx1.commit().join().expectSuccess();
            errorsHolder.assertCodes(StatusCode.TRANSPORT_UNAVAILABLE);

            writer.shutdown().join();
        }

        assertTopicContent(ONE_PART_TOPIC, Arrays.asList(msg1, msg2));
    }
}
