package tech.ydb.topic;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tech.ydb.common.transaction.TxMode;
import tech.ydb.core.Status;
import tech.ydb.core.StatusCode;
import tech.ydb.table.SessionRetryContext;
import tech.ydb.table.TableClient;
import tech.ydb.table.transaction.TableTransaction;
import tech.ydb.test.junit4.GrpcTransportRule;
import tech.ydb.topic.description.Consumer;
import tech.ydb.topic.description.ConsumerDescription;
import tech.ydb.topic.description.ConsumerPartitionInfo;
import tech.ydb.topic.impl.SerialExecutor;
import tech.ydb.topic.read.AsyncReader;
import tech.ydb.topic.read.Message;
import tech.ydb.topic.read.SyncReader;
import tech.ydb.topic.read.events.DataReceivedEvent;
import tech.ydb.topic.read.events.ReadEventHandler;
import tech.ydb.topic.read.events.StartPartitionSessionEvent;
import tech.ydb.topic.read.impl.AsyncReaderImpl;
import tech.ydb.topic.settings.AlterPartitioningSettings;
import tech.ydb.topic.settings.AlterTopicSettings;
import tech.ydb.topic.settings.AutoPartitioningStrategy;
import tech.ydb.topic.settings.CommitOffsetSettings;
import tech.ydb.topic.settings.CreateTopicSettings;
import tech.ydb.topic.settings.DescribeConsumerSettings;
import tech.ydb.topic.settings.PartitioningSettings;
import tech.ydb.topic.settings.ReadEventHandlersSettings;
import tech.ydb.topic.settings.ReaderSettings;
import tech.ydb.topic.settings.StartPartitionSessionSettings;
import tech.ydb.topic.settings.TopicReadSettings;
import tech.ydb.topic.settings.UpdateOffsetsInTransactionSettings;
import tech.ydb.topic.settings.WriterSettings;
import tech.ydb.topic.utils.ErrorsHandler;
import tech.ydb.topic.utils.HideLoggers;
import tech.ydb.topic.utils.HideLoggersRule;
import tech.ydb.topic.write.SyncWriter;

/**
 *
 * @author Aleksandr Gorshenin
 */
public class TopicReadersIntegrationTest {
    private static final Logger logger = LoggerFactory.getLogger(YdbTopicsIntegrationTest.class);

    private static final FailableReaderInterceptor PROXY = new FailableReaderInterceptor();

    @ClassRule
    public final static GrpcTransportRule ydbTransport = new GrpcTransportRule()
            .withGrpcTransportCustomizer(b -> b.addChannelInitializer(PROXY));

    @Rule
    public final HideLoggersRule hideLogger = new HideLoggersRule();

    private final static String TEST_TOPIC = "topic_readers_test";
    private final static String SPLITTED_TOPIC = "topic_readers_splitted";

    private final static String TEST_CONSUMER1 = "consumer";

    private static TopicClient client;

    @BeforeClass
    public static void initClient() {
        client = TopicClient.newClient(ydbTransport).build();
        initTopips();
    }

    @AfterClass
    public static void closeClient() {
        dropTopics();
        client.close();
    }

    private static void initTopips() {
        logger.info("Create test topic  {} ...", TEST_TOPIC);
        client.createTopic(TEST_TOPIC, CreateTopicSettings.newBuilder()
                .addConsumer(Consumer.newBuilder().setName(TEST_CONSUMER1).build())
                .setPartitioningSettings(PartitioningSettings.newBuilder()
                        .setMinActivePartitions(3)
                        .setMaxActivePartitions(3)
                        .build())
                .build()
        ).join().expectSuccess("can't create a new topic");

        CompletableFuture<Void> f1 = CompletableFuture.runAsync(() -> writeToTopic(TEST_TOPIC, 0, 1000));
        CompletableFuture<Void> f2 = CompletableFuture.runAsync(() -> writeToTopic(TEST_TOPIC, 1, 500));
        CompletableFuture<Void> f3 = CompletableFuture.runAsync(() -> writeToTopic(TEST_TOPIC, 2, 2100));

        CompletableFuture.allOf(f1, f2, f3).join();

        client.createTopic(SPLITTED_TOPIC, CreateTopicSettings.newBuilder()
                .addConsumer(Consumer.newBuilder().setName(TEST_CONSUMER1).build())
                .setPartitioningSettings(PartitioningSettings.newBuilder()
                        .setAutoPartitioningStrategy(AutoPartitioningStrategy.PAUSED)
                        .setMinActivePartitions(2)
                        .setMaxActivePartitions(2)
                        .build())
                .build()
        ).join().expectSuccess("can't create a new topic");

        CompletableFuture<Void> f4 = CompletableFuture.runAsync(() -> writeToTopic(SPLITTED_TOPIC, 0, 860));
        CompletableFuture<Void> f5 = CompletableFuture.runAsync(() -> writeToTopic(SPLITTED_TOPIC, 1, 100));
        CompletableFuture.allOf(f4, f5).join();

        client.alterTopic(SPLITTED_TOPIC, AlterTopicSettings.newBuilder()
                .setAlterPartitioningSettings(AlterPartitioningSettings.newBuilder()
                        .setMinActivePartitions(4)
                        .setMaxActivePartitions(4)
                        .build())
                .build()
        ).join().expectSuccess("can't alter topic");

        CompletableFuture<Void> f6 = CompletableFuture.runAsync(() -> writeToTopic(SPLITTED_TOPIC, "p0", 860, 140));
        CompletableFuture<Void> f7 = CompletableFuture.runAsync(() -> writeToTopic(SPLITTED_TOPIC, "p1", 100, 400));
        CompletableFuture.allOf(f6, f7).join();
    }

    private static void dropTopics() {
        logger.info("Drop test topic {} ...", TEST_TOPIC);
        client.dropTopic(TEST_TOPIC).join();
        logger.info("Drop test topic {} ...", SPLITTED_TOPIC);
        client.dropTopic(SPLITTED_TOPIC).join();
    }

    @Before
    public void resetConsumer() {
        PROXY.reset();
        List<CompletableFuture<Status>> resets = new ArrayList<>();

        DescribeConsumerSettings dc = DescribeConsumerSettings.newBuilder().withIncludeStats(true).build();
        ConsumerDescription cd1 = client.describeConsumer(TEST_TOPIC, TEST_CONSUMER1, dc).join().getValue();
        ConsumerDescription cd2 = client.describeConsumer(SPLITTED_TOPIC, TEST_CONSUMER1, dc).join().getValue();

        for (ConsumerPartitionInfo p: cd1.getPartitions()) {
            if (p.getConsumerStats().getCommittedOffset() > 0) {
                resets.add(resetPartition(TEST_TOPIC, p.getPartitionId()));
            }
        }
        for (ConsumerPartitionInfo p: cd2.getPartitions()) {
            if (p.getConsumerStats().getCommittedOffset() > 0) {
                resets.add(resetPartition(SPLITTED_TOPIC, p.getPartitionId()));
            }
        }

        resets.forEach(f -> f.join().expectSuccess());
    }

    private static CompletableFuture<Status> resetPartition(String topicPath, long partitionID) {
        return client.commitOffset(topicPath, CommitOffsetSettings.newBuilder()
                .setConsumer(TEST_CONSUMER1)
                .setOffset(0)
                .setPartitionId(partitionID)
                .build());
    }

    private static void writeToTopic(String topicPath, int partitionID, int count) {
        writeToTopic(0, count, WriterSettings.newBuilder()
                .setTopicPath(topicPath)
                .setProducerId("p" + partitionID)
                .setPartitionId(partitionID)
                .build());
    }

    private static void writeToTopic(String topicPath, String producerId, int startFrom, int count) {
        writeToTopic(startFrom, count, WriterSettings.newBuilder()
                .setLogPrefix("writers-test-" + producerId)
                .setTopicPath(topicPath)
                .setProducerId(producerId)
                .build());
    }

    private static byte[] writedMsg(String producerID, int idx) {
        byte[] msg = ("p" + producerID + "_msg" + idx).getBytes();
        byte[] data = new byte[100];
        Arrays.fill(data, (byte) 0x20); // fill spaces
        System.arraycopy(msg, 0, data, 0, msg.length);
        return data;
    }

    private static void writeToTopic(int startFrom, int count, WriterSettings settings) {
        SyncWriter writer = client.createSyncWriter(settings);
        writer.initAndWait();
        for (int idx = 1; idx <= count; idx++) {
            writer.send(tech.ydb.topic.write.Message.of(writedMsg(settings.getProducerId(), startFrom + idx)));
        }

        try {
            writer.flush();
            writer.shutdown(10, TimeUnit.SECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException ex) {
            throw new AssertionError("cannot write", ex);
        }
    }

    @Test
    @HideLoggers({ SerialExecutor.class, AsyncReaderImpl.class })
    public void singleThreadExecutorTest() throws Exception {
        ReaderSettings readerSettings = ReaderSettings.newBuilder()
                .addTopic(TopicReadSettings.newBuilder()
                        .setPath(TEST_TOPIC)
                        .build())
                .setConsumerName(TEST_CONSUMER1)
                .build();

        CountDownLatch read = new CountDownLatch(1);
        CompletableFuture<Boolean> processing = new CompletableFuture<>();

        ExecutorService executor = Executors.newSingleThreadExecutor((r) -> new Thread(r, "test-executor"));
        AsyncReader reader = client.createAsyncReader(readerSettings, ReadEventHandlersSettings.newBuilder()
                .setExecutor(executor)
                .setEventHandler((event) -> {
                    read.countDown();
                    processing.join();
                }).build()
        );

        reader.init().join();

        // wait for message committing
        Assert.assertTrue(read.await(5, TimeUnit.SECONDS));

        // stop reader
        CompletableFuture<Void> shutdown = reader.shutdown();
        processing.completeExceptionally(new RuntimeException("shutdown"));
        shutdown.get(5, TimeUnit.SECONDS);

        executor.shutdown();
        executor.awaitTermination(5, TimeUnit.SECONDS);
    }

    @Test
    public void singleThreadReadAllTest() throws Exception {
        ReaderSettings readerSettings = ReaderSettings.newBuilder()
                .addTopic(TEST_TOPIC)
                .setConsumerName(TEST_CONSUMER1)
                .build();

        CountDownLatch read = new CountDownLatch(3600);

        ExecutorService executor = Executors.newSingleThreadExecutor((r) -> new Thread(r, "test-executor"));
        AsyncReader reader = client.createAsyncReader(readerSettings, ReadEventHandlersSettings.newBuilder()
                .setExecutor(executor)
                .setEventHandler((event) -> {
                    event.commit().join();
                    event.getMessages().forEach(m -> read.countDown());
                }).build()
        );

        reader.init();

        // wait for message committing
        Assert.assertTrue(read.await(5, TimeUnit.SECONDS));

        // stop reader
        reader.shutdown();

        executor.shutdown();
        executor.awaitTermination(5, TimeUnit.SECONDS);
    }

    @Test
    public void readAllTest() throws InterruptedException {
        ReaderSettings readerSettings = ReaderSettings.newBuilder()
                .addTopic(TopicReadSettings.newBuilder().setPath(TEST_TOPIC).build())
                .setConsumerName(TEST_CONSUMER1)
                .build();

        AtomicLong[] offsets = new AtomicLong[] { new AtomicLong(), new AtomicLong(), new AtomicLong() };
        CountDownLatch read = new CountDownLatch(3600);
        AsyncReader reader = client.createAsyncReader(readerSettings, ReadEventHandlersSettings.newBuilder()
                .setEventHandler((DataReceivedEvent event) -> {
                    AtomicLong offset = offsets[(int) event.getPartitionSession().getPartitionId()];
                    for (Message msg : event.getMessages()) {
                        Assert.assertEquals(offset.getAndIncrement(), msg.getOffset());
                        read.countDown();
                    }
                }).build());

        reader.init().join();
        try {
            Assert.assertTrue(read.await(30, TimeUnit.SECONDS));
            Assert.assertEquals(1000, offsets[0].get());
            Assert.assertEquals(500, offsets[1].get());
            Assert.assertEquals(2100, offsets[2].get());
        } finally {
            reader.shutdown().join();
        }
    }

    @Test
    public void readAllWithCommitsTest() throws Exception {
        ReaderSettings readerSettings = ReaderSettings.newBuilder()
                .addTopic(TopicReadSettings.newBuilder().setPath(TEST_TOPIC).build())
                .setConsumerName(TEST_CONSUMER1)
                .build();

        CountDownLatch read = new CountDownLatch(3600);
        ExecutorService executor = Executors.newSingleThreadExecutor((r) -> new Thread(r, "test-executor"));
        AsyncReader reader = client.createAsyncReader(readerSettings, ReadEventHandlersSettings.newBuilder()
                .setExecutor(executor)
                .setEventHandler((DataReceivedEvent event) -> {
                    event.commit().join();
                    event.getMessages().forEach(msg -> read.countDown());
                }).build());

        reader.init();
        try {
            Assert.assertTrue(read.await(30, TimeUnit.SECONDS));
        } finally {
            reader.shutdown().get(1, TimeUnit.SECONDS);
            executor.shutdown();
        }
    }

    @Test
    public void readAllSplittedTest() throws InterruptedException {
        ReaderSettings readerSettings = ReaderSettings.newBuilder()
                .addTopic(TopicReadSettings.newBuilder().setPath(SPLITTED_TOPIC).build())
                .setConsumerName(TEST_CONSUMER1)
                .setDecompressionExecutor(Runnable::run)
                .setMaxBatchSize(100)
                .build();

        BlockingQueue<Message> queue = new ArrayBlockingQueue<>(1);
        AsyncReader reader = client.createAsyncReader(readerSettings, ReadEventHandlersSettings.newBuilder()
                .setEventHandler((DataReceivedEvent event) -> {
                    try {
                        for (Message msg : event.getMessages()) {
                            Assert.assertTrue(queue.offer(msg, 60, TimeUnit.SECONDS));
                        }
                    } catch (InterruptedException ex) {
                        throw new AssertionError("cannot process event", ex);
                    }
        }).build());

        CountDownLatch recieved = new CountDownLatch(1500);
        PROXY.listenPartitionData(partitionData -> {
            partitionData.getBatchesList().forEach(batch -> {
                batch.getMessageDataList().forEach(msg -> recieved.countDown());
            });
        });

        reader.init();
        try {
            // wait to recieve all messages
            Assert.assertTrue(recieved.await(5, TimeUnit.SECONDS));

            // validate all messages
            int p0_idx = 0;
            int p1_idx = 0;
            while (p0_idx < 1000 || p1_idx < 500) {
                Message msg = queue.poll(1, TimeUnit.SECONDS);
                Assert.assertNotNull("cannot get msg " + (p0_idx + p1_idx), msg);

                int msg_idx = "p0".equals(msg.getProducerId()) ? ++p0_idx : ++p1_idx;
                byte[] expected = writedMsg(msg.getProducerId(), msg_idx);
                Assert.assertEquals(new String(expected), new String(msg.getData()));
            }
        } finally {
            reader.shutdown().join();
        }
    }

    @Test(timeout = 120000)
    public void syncReadAllWithDefaultRetryPolicyTest() throws Exception {
        // Fail initialization, reading, and both sides of committing, in this order.
        PROXY.unavailableOnInit(1);
        PROXY.badRequestOnInit(2);
        PROXY.unavailableOnReadResponse(1);
        PROXY.unavailableOnInit(4);
        PROXY.unavailableOnInit(5);
        PROXY.unavailableOnInit(6);
        PROXY.badRequestOnReadResponse(2);

        PROXY.badSessionOnCommitWithOffset(1, 60);
        PROXY.unavailableOnCommitAck(1, 200);

        AtomicLong[] committed = new AtomicLong[] { new AtomicLong(), new AtomicLong(), new AtomicLong() };
        CountDownLatch totalCommitted = new CountDownLatch(3600);

        ErrorsHandler errors = new ErrorsHandler();
        ReaderSettings settings = ReaderSettings.newBuilder()
                .addTopic(TopicReadSettings.newBuilder().setPath(TEST_TOPIC).build())
                .setConsumerName(TEST_CONSUMER1)
                .setErrorsHandler((st, th) -> { // restore partition states
                    DescribeConsumerSettings s = DescribeConsumerSettings.newBuilder()
                            .withIncludeStats(true).build();
                    ConsumerDescription desc = client.describeConsumer(TEST_TOPIC, TEST_CONSUMER1, s).join().getValue();
                    for (ConsumerPartitionInfo partition: desc.getPartitions()) {
                        int pid = (int) partition.getPartitionId();
                        long lastCommit = partition.getConsumerStats().getCommittedOffset();
                        long diff = lastCommit - committed[pid].getAndSet(lastCommit);
                        for (int idx = 0; idx < diff; idx++) {
                            totalCommitted.countDown();
                        }
                    }
                    errors.accept(st, th);
                })
                .build();

        Thread worker = new Thread(() -> {
            Semaphore commitInflyLimit = new Semaphore(100);
            SyncReader reader = client.createSyncReader(settings);
            reader.init();
            try {
                while (!Thread.interrupted() && totalCommitted.getCount() > 0) {
                    Message msg = reader.receive(10, TimeUnit.MILLISECONDS);
                    if (msg == null) {
                        continue;
                    }

                    int pid = (int) msg.getPartitionSession().getPartitionId();
                    AtomicLong lastCommit = committed[pid];
                    long messageCommit = msg.getOffset() + 1;
                    // limit commit infly to avoid last message committing before test errors
                    commitInflyLimit.acquire();
                    msg.commit().whenComplete((res, th) -> {
                        commitInflyLimit.release();
                        if (th == null) { // commit is successful
                            long diff = messageCommit - lastCommit.getAndSet(messageCommit);
                            for (int idx = 0; idx < diff; idx++) {
                                totalCommitted.countDown();
                            }
                        }
                    });
                }
            } catch (InterruptedException ex) {
                // nothing
            } finally {
                reader.shutdown();
            }
        });

        worker.start();
        try {
            Assert.assertTrue("All messages must be committed", totalCommitted.await(30, TimeUnit.SECONDS));
            Assert.assertEquals(1000, committed[0].get());
            Assert.assertEquals(500, committed[1].get());
            Assert.assertEquals(2100, committed[2].get());
        } finally {
            worker.interrupt();
            worker.join(1_000);
        }

        errors.assertCodes(
                StatusCode.TRANSPORT_UNAVAILABLE,
                StatusCode.BAD_REQUEST,
                StatusCode.TRANSPORT_UNAVAILABLE,
                StatusCode.TRANSPORT_UNAVAILABLE,
                StatusCode.TRANSPORT_UNAVAILABLE,
                StatusCode.TRANSPORT_UNAVAILABLE,
                StatusCode.BAD_REQUEST,
                StatusCode.BAD_SESSION,
                StatusCode.TRANSPORT_UNAVAILABLE
        );
    }

    @Test(timeout = 120000)
    public void asyncReadAllWithDefaultRetryPolicyTest() throws Exception {
        // Fail initialization, reading, and both sides of committing, in this order.
        PROXY.unavailableOnInit(1);
        PROXY.badRequestOnInit(2);
        PROXY.unavailableOnReadResponse(1);
        PROXY.unavailableOnInit(4);
        PROXY.unavailableOnInit(5);

        PROXY.badSessionOnCommitWithOffset(2, 100);
        PROXY.unavailableOnCommitAck(2, 500);

        ErrorsHandler errors = new ErrorsHandler();
        ReaderSettings settings = ReaderSettings.newBuilder()
                .addTopic(TopicReadSettings.newBuilder().setPath(TEST_TOPIC).build())
                .setReaderName("async-read-all-with-default-retry-policy")
                .setConsumerName(TEST_CONSUMER1)
                .setMaxBatchSize(100) // commits by 100 messages
                .setErrorsHandler(errors)
                .build();

        AtomicLong[] offsets = new AtomicLong[] { new AtomicLong(), new AtomicLong(), new AtomicLong() };
        AtomicLong[] committed = new AtomicLong[] { new AtomicLong(), new AtomicLong(), new AtomicLong() };
        CountDownLatch totalCommitted = new CountDownLatch(3600);

        AsyncReader reader = client.createAsyncReader(settings, ReadEventHandlersSettings.newBuilder()
                .setEventHandler(new ReadEventHandler() {
                    @Override
                    public void onStartPartitionSession(StartPartitionSessionEvent event) {
                        int pid = (int) event.getPartitionSession().getPartitionId();
                        // restore offset position
                        offsets[pid].set(event.getCommittedOffset());
                        // restore lost commits
                        long diff = event.getCommittedOffset() - committed[pid].getAndSet(event.getCommittedOffset());
                        for (int idx = 0; idx < diff; idx++) {
                            totalCommitted.countDown();
                        }
                        event.confirm();
                    }

                    @Override
                    public void onMessages(DataReceivedEvent event) {
                        int pid = (int) event.getPartitionSession().getPartitionId();
                        AtomicLong offset = offsets[pid];
                        AtomicLong lastCommit = committed[pid];
                        for (Message msg : event.getMessages()) {
                            Assert.assertEquals(offset.getAndIncrement(), msg.getOffset());
                        }

                        long eventCommit = event.getRangeToCommit().getEnd();
                        event.commit().thenRun(() -> {
                            long diff = eventCommit - lastCommit.getAndSet(eventCommit);
                            for (int idx = 0; idx < diff; idx++) {
                                totalCommitted.countDown();
                            }
                        });
                    }
                }).build());

        reader.init().join();
        try {
            Assert.assertTrue(totalCommitted.await(30, TimeUnit.SECONDS));
            Assert.assertEquals(1000, offsets[0].get());
            Assert.assertEquals(500, offsets[1].get());
            Assert.assertEquals(2100, offsets[2].get());
            Assert.assertEquals(1000, committed[0].get());
            Assert.assertEquals(500, committed[1].get());
            Assert.assertEquals(2100, committed[2].get());
        } finally {
            reader.shutdown().join();
        }

        errors.assertCodes(
                StatusCode.TRANSPORT_UNAVAILABLE,
                StatusCode.BAD_REQUEST,
                StatusCode.TRANSPORT_UNAVAILABLE,
                StatusCode.TRANSPORT_UNAVAILABLE,
                StatusCode.TRANSPORT_UNAVAILABLE,
                StatusCode.BAD_SESSION,
                StatusCode.TRANSPORT_UNAVAILABLE
        );
    }

    @Test
    public void readAllByPartitionIdTest() throws InterruptedException {
        ReaderSettings readerSettings = ReaderSettings.newBuilder()
                .addTopic(TopicReadSettings.newBuilder()
                        .setPath(TEST_TOPIC)
                        .setPartitionIds(Arrays.asList(0L))
                        .build())
                .setConsumerName(TEST_CONSUMER1)
                .build();

        AtomicLong offset = new AtomicLong();
        CountDownLatch read = new CountDownLatch(1000);
        AsyncReader reader = client.createAsyncReader(readerSettings, ReadEventHandlersSettings.newBuilder()
                .setEventHandler((DataReceivedEvent event) -> {
            for (Message msg: event.getMessages()) {
                Assert.assertEquals(offset.getAndIncrement(), msg.getOffset());
                read.countDown();
            }
        }).build());

        reader.init().join();
        try {
            Assert.assertTrue(read.await(30, TimeUnit.SECONDS));
            Assert.assertEquals(1000, offset.get());
        } finally {
            reader.shutdown().join();
        }
    }

    @Test
    public void readAllByWithRebalancingTest() throws Exception {
        ReaderSettings readerSettings = ReaderSettings.newBuilder()
                .addTopic(TEST_TOPIC)
                .setConsumerName(TEST_CONSUMER1)
                .build();

        AtomicLong[] offsets = new AtomicLong[] { new AtomicLong(), new AtomicLong(), new AtomicLong() };
        CountDownLatch[] read = new CountDownLatch[] {
            new CountDownLatch(1000),
            new CountDownLatch(500),
            new CountDownLatch(2100),
        };
        CountDownLatch partitions = new CountDownLatch(3);

        ReadEventHandler handler = new ReadEventHandler() {
            @Override
            public void onStartPartitionSession(StartPartitionSessionEvent event) {
                event.confirm();
                partitions.countDown();
            }

            @Override
            public void onMessages(DataReceivedEvent event) {
                int pid = (int) event.getPartitionSession().getPartitionId();
                for (Message msg : event.getMessages()) {
                    Assert.assertEquals(offsets[pid].getAndIncrement(), msg.getOffset());
                    read[pid].countDown();
                }
            }
        };

        AsyncReader reader1 = client.createAsyncReader(readerSettings, ReadEventHandlersSettings.newBuilder()
                .setEventHandler(handler).build());

        reader1.init();
        try {
            // wait for reader1 to get all partitions
            Assert.assertTrue(partitions.await(5, TimeUnit.SECONDS));

            CountDownLatch[] read2 = new CountDownLatch[] { read[0], read[1], read[2] };
            AtomicLong[] offsets2 = new AtomicLong[] { offsets[0], offsets[1], offsets[2] };
            CountDownLatch partitions2 = new CountDownLatch(2);
            ReadEventHandler handler2 = new ReadEventHandler() {
                @Override
                public void onStartPartitionSession(StartPartitionSessionEvent event) {
                    // reset read counter
                    int pid = (int) event.getPartitionSession().getPartitionId();
                    read2[pid] = new CountDownLatch(pid == 0 ? 1000 : (pid == 1 ? 500 : 2100));
                    offsets2[pid] = new AtomicLong();
                    partitions2.countDown();
                    event.confirm();
                }

                @Override
                public void onMessages(DataReceivedEvent event) {
                int pid = (int) event.getPartitionSession().getPartitionId();
                    for (Message msg : event.getMessages()) {
                        Assert.assertEquals(offsets2[pid].getAndIncrement(), msg.getOffset());
                        read2[pid].countDown();
                    }
                }
            };

            AsyncReader reader2 = client.createAsyncReader(readerSettings, ReadEventHandlersSettings.newBuilder()
                    .setEventHandler(handler2).build());
            AsyncReader reader3 = client.createAsyncReader(readerSettings, ReadEventHandlersSettings.newBuilder()
                    .setEventHandler(handler2).build());

            reader2.init();
            reader3.init();

            try {
                // wait for rebalancing - every reader must have one partition
                Assert.assertTrue(partitions2.await(5, TimeUnit.SECONDS));

                Assert.assertTrue(read2[0].await(5, TimeUnit.SECONDS));
                Assert.assertTrue(read2[1].await(5, TimeUnit.SECONDS));
                Assert.assertTrue(read2[2].await(5, TimeUnit.SECONDS));

                Assert.assertEquals(1000, offsets2[0].get());
                Assert.assertEquals(500, offsets2[1].get());
                Assert.assertEquals(2100, offsets2[2].get());
            } finally {
                reader2.shutdown().get(1, TimeUnit.SECONDS);
                reader3.shutdown().get(1, TimeUnit.SECONDS);
            }
        } finally {
            reader1.shutdown().get(1, TimeUnit.SECONDS);
        }
    }

    @Test
    public void readAllByWithLazyRebalancingTest() throws Exception {
        ReaderSettings readerSettings = ReaderSettings.newBuilder()
                .addTopic(TEST_TOPIC)
                .setConsumerName(TEST_CONSUMER1)
                .build();

        AtomicLong[] offsets = new AtomicLong[] { new AtomicLong(), new AtomicLong(), new AtomicLong() };
        CountDownLatch[] read = new CountDownLatch[] {
            new CountDownLatch(1000),
            new CountDownLatch(500),
            new CountDownLatch(2100),
        };
        CountDownLatch partitions = new CountDownLatch(3);
        Queue<StartPartitionSessionEvent> events = new ConcurrentLinkedQueue<>();

        ReadEventHandler handler = new ReadEventHandler() {
            @Override
            public void onStartPartitionSession(StartPartitionSessionEvent event) {
                events.add(event);
                partitions.countDown();
            }

            @Override
            public void onMessages(DataReceivedEvent event) {
                int pid = (int) event.getPartitionSession().getPartitionId();
                for (Message msg : event.getMessages()) {
                    Assert.assertEquals(offsets[pid].getAndIncrement(), msg.getOffset());
                    read[pid].countDown();
                }
            }
        };

        AsyncReader reader1 = client.createAsyncReader(readerSettings, ReadEventHandlersSettings.newBuilder()
                .setEventHandler(handler).build());

        reader1.init();
        try {
            // wait for reader1 to get all partitions
            Assert.assertTrue(partitions.await(5, TimeUnit.SECONDS));

            CountDownLatch partitions2 = new CountDownLatch(2);
            ReadEventHandler handler2 = new ReadEventHandler() {
                @Override
                public void onStartPartitionSession(StartPartitionSessionEvent event) {
                    events.add(event);
                    partitions2.countDown();
                }

                @Override
                public void onMessages(DataReceivedEvent event) {
                int pid = (int) event.getPartitionSession().getPartitionId();
                    for (Message msg : event.getMessages()) {
                        Assert.assertEquals(offsets[pid].getAndIncrement(), msg.getOffset());
                        read[pid].countDown();
                    }
                }
            };

            AsyncReader reader2 = client.createAsyncReader(readerSettings, ReadEventHandlersSettings.newBuilder()
                    .setEventHandler(handler2).build());
            AsyncReader reader3 = client.createAsyncReader(readerSettings, ReadEventHandlersSettings.newBuilder()
                    .setEventHandler(handler2).build());

            reader2.init();
            reader3.init();

            try {
                // wait for rebalancing - every reader must have one partition
                Assert.assertTrue(partitions2.await(5, TimeUnit.SECONDS));

                events.forEach(StartPartitionSessionEvent::confirm);

                Assert.assertTrue(read[0].await(5, TimeUnit.SECONDS));
                Assert.assertTrue(read[1].await(5, TimeUnit.SECONDS));
                Assert.assertTrue(read[2].await(5, TimeUnit.SECONDS));

                Assert.assertEquals(1000, offsets[0].get());
                Assert.assertEquals(500, offsets[1].get());
                Assert.assertEquals(2100, offsets[2].get());
            } finally {
                reader2.shutdown().get(1, TimeUnit.SECONDS);
                reader3.shutdown().get(1, TimeUnit.SECONDS);
            }
        } finally {
            reader1.shutdown().get(1, TimeUnit.SECONDS);
        }
    }

    @Test
    public void readAllWithoutConsumerTest() throws InterruptedException {
        ReaderSettings readerSettings = ReaderSettings.newBuilder()
                .addTopic(TopicReadSettings.newBuilder()
                        .setPath(TEST_TOPIC)
                        .setPartitionIds(Arrays.asList(0L))
                        .build())
                .withoutConsumer()
                .build();

        AtomicLong offset = new AtomicLong();
        CountDownLatch read = new CountDownLatch(1000);
        AsyncReader reader = client.createAsyncReader(readerSettings, ReadEventHandlersSettings.newBuilder()
                .setEventHandler((DataReceivedEvent event) -> {
            for (Message msg: event.getMessages()) {
                Assert.assertEquals(offset.getAndIncrement(), msg.getOffset());
                read.countDown();
            }
        }).build());


        reader.init().join();
        try {
            Assert.assertTrue(read.await(30, TimeUnit.SECONDS));
            Assert.assertEquals(1000, offset.get());
        } finally {
            reader.shutdown().join();
        }
    }

    @Test
    public void readFromTest() throws Exception {
        ReaderSettings readerSettings = ReaderSettings.newBuilder()
                .addTopic(TopicReadSettings.newBuilder()
                        .setPath(TEST_TOPIC)
                        .setPartitionIds(Arrays.asList(1L))
                        .build())
                .setConsumerName(TEST_CONSUMER1)
                .build();

        AtomicLong offset = new AtomicLong(123L);
        CountDownLatch read = new CountDownLatch(500-123);
        AsyncReader reader = client.createAsyncReader(readerSettings, ReadEventHandlersSettings.newBuilder()
                .setEventHandler(new ReadEventHandler() {
                    @Override
                    public void onStartPartitionSession(StartPartitionSessionEvent event) {
                        Assert.assertEquals(0, event.getCommittedOffset());
                        Assert.assertEquals(0, event.getPartitionOffsets().getStart());
                        Assert.assertEquals(500, event.getPartitionOffsets().getEnd());

                        // read only from offset 123
                        event.confirm(StartPartitionSessionSettings.newBuilder().setReadOffset(123L).build());
                    }

                    @Override
                    public void onMessages(DataReceivedEvent event) {
                        for (Message msg : event.getMessages()) {
                            Assert.assertEquals(offset.getAndIncrement(), msg.getOffset());
                            read.countDown();
                        }
                    }
                }).build());

        reader.init().join();
        try {
            Assert.assertTrue(read.await(30, TimeUnit.SECONDS));
            Assert.assertEquals(500, offset.get());
        } finally {
            reader.shutdown().join();
        }
    }

    @Test
    public void readFromWithCommitTest() throws Exception {
        ReaderSettings readerSettings = ReaderSettings.newBuilder()
                .addTopic(TopicReadSettings.newBuilder()
                        .setPath(TEST_TOPIC)
                        .setPartitionIds(Arrays.asList(1L))
                        .build())
                .setConsumerName(TEST_CONSUMER1)
                .build();

        AtomicLong offset = new AtomicLong(200L);
        CountDownLatch committed = new CountDownLatch(500-200);
        AsyncReader reader = client.createAsyncReader(readerSettings, ReadEventHandlersSettings.newBuilder()
                .setEventHandler(new ReadEventHandler() {
                    @Override
                    public void onStartPartitionSession(StartPartitionSessionEvent event) {
                        Assert.assertEquals(0, event.getCommittedOffset());
                        Assert.assertEquals(0, event.getPartitionOffsets().getStart());
                        Assert.assertEquals(500, event.getPartitionOffsets().getEnd());

                        // read only from offset 200
                        event.confirm(StartPartitionSessionSettings.newBuilder()
                                .setReadOffset(200L)
                                .setCommitOffset(200L)
                                .build());
                    }

                    @Override
                    public void onMessages(DataReceivedEvent event) {
                        for (Message msg: event.getMessages()) {
                            Assert.assertEquals(msg.getOffset(), offset.getAndIncrement());
                            msg.commit().whenComplete((r, th) -> {
                                Assert.assertNull(th);
                                committed.countDown();
                            });
                        }
                    }
                }).build());

        reader.init().join();
        try {
            Assert.assertTrue(committed.await(30, TimeUnit.SECONDS));
            Assert.assertEquals(500, offset.get());
        } finally {
            reader.shutdown().join();
        }
    }

    @Test
    public void readRetentionedTopicTest() throws Exception {
        ReaderSettings readerSettings = ReaderSettings.newBuilder()
                .addTopic(TopicReadSettings.newBuilder()
                        .setPath(TEST_TOPIC)
                        .setPartitionIds(Arrays.asList(1L))
                        .build())
                .setConsumerName(TEST_CONSUMER1)
                .build();

        AtomicLong offset = new AtomicLong(150L);
        CountDownLatch lastCommitted = new CountDownLatch(1);
        AsyncReader reader = client.createAsyncReader(readerSettings, ReadEventHandlersSettings.newBuilder()
                .setEventHandler(new ReadEventHandler() {
                    @Override
                    public void onStartPartitionSession(StartPartitionSessionEvent event) {
                        Assert.assertEquals(0, event.getCommittedOffset());
                        Assert.assertEquals(0, event.getPartitionOffsets().getStart());
                        Assert.assertEquals(500, event.getPartitionOffsets().getEnd());

                        // emulate topic retention - skip first 150 messages but don't commit them
                        event.confirm(StartPartitionSessionSettings.newBuilder()
                                .setReadOffset(150L)
                                .build());
                    }

                    @Override
                    public void onMessages(DataReceivedEvent event) {
                        for (Message msg: event.getMessages()) {
                            Assert.assertEquals(msg.getOffset(), offset.getAndIncrement());
                        }

                        event.commit().whenComplete((r, th) -> {
                            Assert.assertNull(th);
                            if (event.getRangeToCommit().getEnd() >= 500) {
                                lastCommitted.countDown();
                            }
                        });
                    }
                }).build());

        reader.init().join();
        try {
            Assert.assertTrue(lastCommitted.await(30, TimeUnit.SECONDS));
            Assert.assertEquals(500, offset.get());
        } finally {
            reader.shutdown().join();
        }
    }

    @Test
    public void smallBufferTest() throws InterruptedException {
        ReaderSettings readerSettings = ReaderSettings.newBuilder()
                .addTopic(TopicReadSettings.newBuilder().setPath(TEST_TOPIC).build())
                .setConsumerName(TEST_CONSUMER1)
                .setMaxMemoryUsageBytes(1000)
                .build();

        AtomicLong[] offsets = new AtomicLong[] { new AtomicLong(), new AtomicLong(), new AtomicLong() };
        CountDownLatch read = new CountDownLatch(3600);
        AsyncReader reader = client.createAsyncReader(readerSettings, ReadEventHandlersSettings.newBuilder()
                .setEventHandler((DataReceivedEvent event) -> {
                    AtomicLong offset = offsets[(int) event.getPartitionSession().getPartitionId()];
                    for (Message msg : event.getMessages()) {
                        Assert.assertEquals(offset.getAndIncrement(), msg.getOffset());
                        read.countDown();
                    }
                }).build());

        reader.init().join();
        try {
            Assert.assertTrue(read.await(30, TimeUnit.SECONDS));
            Assert.assertEquals(1000, offsets[0].get());
            Assert.assertEquals(500, offsets[1].get());
            Assert.assertEquals(2100, offsets[2].get());
        } finally {
            reader.shutdown().join();
        }
    }

    @Test
    public void directDecompressorTest() throws InterruptedException {
        ReaderSettings readerSettings = ReaderSettings.newBuilder()
                .addTopic(TopicReadSettings.newBuilder().setPath(TEST_TOPIC).build())
                .setConsumerName(TEST_CONSUMER1)
                .setDecompressionExecutor(Runnable::run)
                .setMaxMemoryUsageBytes(1000)
                .build();

        AtomicLong[] offsets = new AtomicLong[] { new AtomicLong(), new AtomicLong(), new AtomicLong() };
        CountDownLatch read = new CountDownLatch(3600);
        AsyncReader reader = client.createAsyncReader(readerSettings, ReadEventHandlersSettings.newBuilder()
                .setExecutor(Runnable::run)
                .setEventHandler((DataReceivedEvent event) -> {
                    AtomicLong offset = offsets[(int) event.getPartitionSession().getPartitionId()];
                    for (Message msg : event.getMessages()) {
                        Assert.assertEquals(offset.getAndIncrement(), msg.getOffset());
                        read.countDown();
                    }
                }).build());

        reader.init().join();
        try {
            Assert.assertTrue(read.await(30, TimeUnit.SECONDS));
            Assert.assertEquals(1000, offsets[0].get());
            Assert.assertEquals(500, offsets[1].get());
            Assert.assertEquals(2100, offsets[2].get());
        } finally {
            reader.shutdown().join();
        }
    }

    @Test
    public void readAllInTxTest() throws InterruptedException {
        ReaderSettings readerSettings = ReaderSettings.newBuilder()
                .addTopic(TopicReadSettings.newBuilder().setPath(TEST_TOPIC).build())
                .setConsumerName(TEST_CONSUMER1)
                .build();

        AtomicLong[] offsets = new AtomicLong[] { new AtomicLong(), new AtomicLong(), new AtomicLong() };
        CountDownLatch read = new CountDownLatch(3600);

        try (TableClient tableClient = TableClient.newClient(ydbTransport).build()) {
            SessionRetryContext retryCtx = SessionRetryContext.create(tableClient).idempotent(true).build();
            UpdateOffsetsInTransactionSettings settings = UpdateOffsetsInTransactionSettings.newBuilder().build();

            AtomicReference<AsyncReader> ref = new AtomicReference<>();
            @SuppressWarnings("deprecation")
            AsyncReader reader = client.createAsyncReader(readerSettings, ReadEventHandlersSettings.newBuilder()
                    .setEventHandler((DataReceivedEvent event) -> {
                        AtomicLong offset = offsets[(int) event.getPartitionSession().getPartitionId()];
                        for (Message msg : event.getMessages()) {
                            Assert.assertEquals(offset.getAndIncrement(), msg.getOffset());
                        }

                        retryCtx.supplyStatus(session -> {
                            TableTransaction tx = session.beginTransaction(TxMode.SERIALIZABLE_RW).join().getValue();
                            ref.get().updateOffsetsInTransaction(tx, event.getPartitionOffsets(), settings).join();
                            return tx.commit();
                        }).join().expectSuccess();

                        event.getMessages().forEach(msg -> read.countDown());
                    }).build());

            ref.set(reader);
            reader.init().join();
            try {
                Assert.assertTrue(read.await(30, TimeUnit.SECONDS));
                Assert.assertEquals(1000, offsets[0].get());
                Assert.assertEquals(500, offsets[1].get());
                Assert.assertEquals(2100, offsets[2].get());
            } finally {
                reader.shutdown().join();
            }
        }

    }
}
