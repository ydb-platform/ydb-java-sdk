package tech.ydb.topic;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.Timeout;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tech.ydb.common.transaction.TxMode;
import tech.ydb.core.Status;
import tech.ydb.core.StatusCode;
import tech.ydb.table.Session;
import tech.ydb.table.TableClient;
import tech.ydb.table.transaction.TableTransaction;
import tech.ydb.test.junit4.GrpcTransportRule;
import tech.ydb.topic.description.Codec;
import tech.ydb.topic.description.Consumer;
import tech.ydb.topic.description.MetadataItem;
import tech.ydb.topic.description.PartitionInfo;
import tech.ydb.topic.description.SupportedCodecs;
import tech.ydb.topic.read.SyncReader;
import tech.ydb.topic.settings.AlterPartitioningSettings;
import tech.ydb.topic.settings.AlterTopicSettings;
import tech.ydb.topic.settings.AutoPartitioningStrategy;
import tech.ydb.topic.settings.AutoPartitioningWriteStrategySettings;
import tech.ydb.topic.settings.CreateTopicSettings;
import tech.ydb.topic.settings.DescribeTopicSettings;
import tech.ydb.topic.settings.MultiWriterSettings;
import tech.ydb.topic.settings.PartitioningSettings;
import tech.ydb.topic.settings.ReaderSettings;
import tech.ydb.topic.settings.TopicReadSettings;
import tech.ydb.topic.settings.TopicRetryConfig;
import tech.ydb.topic.settings.WriterSettings;
import tech.ydb.topic.write.AsyncMultiWriter;
import tech.ydb.topic.write.AsyncWriter;
import tech.ydb.topic.write.Message;
import tech.ydb.topic.write.MultiMessage;
import tech.ydb.topic.write.MultiWriteResult;
import tech.ydb.topic.write.TransactionalMultiWriter;

/**
 * Live Go/Java parity matrix. Run against the pinned YDB documented in the implementation evidence.
 * Every split is confirmed by Describe; every affected partition is drained to its recorded end offset.
 */
@RunWith(Parameterized.class)
public class MultiWriterIntegrationTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(MultiWriterIntegrationTest.class);
    static final MultiWriterFaultInterceptor PROXY = new MultiWriterFaultInterceptor();
    private static final String KEY_A = "key-2";
    private static final String KEY_B = "key-0";
    static TopicClient client;

    @ClassRule
    public static final GrpcTransportRule YDB = new GrpcTransportRule()
            .withGrpcTransportCustomizer(builder -> builder.addChannelInitializer(PROXY)).failIfUnavailable();

    @Rule
    public final Timeout timeout = Timeout.seconds(90);

    private final String scenario;
    private final boolean transactional;
    private final boolean split;
    private final boolean rollback;
    private final MultiWriterFaultInterceptor.Fault fault;
    final List<AsyncMultiWriter> writers = new ArrayList<>();
    String path;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> scenarios() {
        MultiWriterFaultInterceptor.Fault none = MultiWriterFaultInterceptor.Fault.NONE;
        MultiWriterFaultInterceptor.Fault before = MultiWriterFaultInterceptor.Fault.BEFORE_STAGING;
        MultiWriterFaultInterceptor.Fault lost = MultiWriterFaultInterceptor.Fault.LOST_ACK;
        return Arrays.asList(new Object[][] {
            {"ordinary", false, false, false, none},
            {"ordinary_lost_ack", false, false, false, lost},
            {"ordinary_split_before_staging", false, true, false, before},
            {"ordinary_split_after_ack", false, true, false, none},
            {"ordinary_split_lost_ack", false, true, false, lost},
            {"tx_commit", true, false, false, none},
            {"tx_rollback", true, false, true, none},
            {"tx_lost_ack_commit", true, false, false, lost},
            {"tx_lost_ack_rollback", true, false, true, lost},
            {"tx_split_before_staging_commit", true, true, false, before},
            {"tx_split_before_staging_rollback", true, true, true, before},
            {"tx_split_after_ack_commit", true, true, false, none},
            {"tx_split_after_ack_rollback", true, true, true, none},
            {"tx_split_lost_ack_commit", true, true, false, lost},
            {"tx_split_lost_ack_rollback", true, true, true, lost},
            {"tx_lost_commit_response", true, false, false, MultiWriterFaultInterceptor.Fault.LOST_COMMIT},
            {"tx_split_lost_ack_application_retry", true, true, false, lost}
        });
    }

    public MultiWriterIntegrationTest(String scenario, boolean transactional, boolean split, boolean rollback,
            MultiWriterFaultInterceptor.Fault fault) {
        this.scenario = scenario;
        this.transactional = transactional;
        this.split = split;
        this.rollback = rollback;
        this.fault = fault;
    }

    @BeforeClass
    public static void openClient() {
        client = TopicClient.newClient(YDB).build();
    }

    @AfterClass
    public static void closeClient() {
        if (client != null) {
            client.close();
        }
    }

    @Before
    public void createTopic() throws Exception {
        PROXY.reset(fault);
        path = "java_multiwriter_" + scenario + "_" + UUID.randomUUID().toString();
        await(client.createTopic(path, CreateTopicSettings.newBuilder()
                .addConsumer(Consumer.newBuilder().setName("parity").build())
                .setSupportedCodecs(SupportedCodecs.newBuilder().addCodec(Codec.RAW).build())
                .setPartitionWriteSpeedBytesPerSecond(1 << 20)
                .setPartitionWriteBurstBytes(1 << 20)
                .setPartitioningSettings(PartitioningSettings.newBuilder()
                        .setMinActivePartitions(2).setMaxActivePartitions(4)
                        .setAutoPartitioningStrategy(AutoPartitioningStrategy.SCALE_UP)
                        .setWriteStrategySettings(AutoPartitioningWriteStrategySettings.newBuilder()
                                .setStabilizationWindow(Duration.ofSeconds(2))
                                .setUpUtilizationPercent(2).setDownUtilizationPercent(1).build())
                        .build()).build())).expectSuccess();
        List<PartitionInfo> initial = describe(false);
        Assert.assertEquals(2, initial.size());
        Assert.assertTrue("Bound routing metadata required", initial.get(0).hasKeyRange());
        logGraph("before", initial);
    }

    @After
    public void cleanup() throws Exception {
        PROXY.release();
        for (String event : PROXY.events()) {
            LOGGER.info("{} {}", scenario, event);
        }
        for (AsyncMultiWriter writer : writers) {
            try {
                await(writer.shutdown());
            } catch (Exception ex) {
                LOGGER.info("{} shutdown outcome {}", scenario, ex.toString());
            }
        }
        if (path != null) {
            await(client.dropTopic(path)).expectSuccess();
        }
    }

    @Test
    public void reproduceGoScenario() throws Exception {
        if (!transactional) {
            AsyncMultiWriter writer = client.createAsyncMultiWriter(settings());
            writers.add(writer);
            exercise(writer, null, null);
            return;
        }
        try (TableClient table = TableClient.newClient(YDB).build();
                Session session = await(table.createSession(Duration.ofSeconds(5))).getValue()) {
            TableTransaction tx = await(session.beginTransaction(TxMode.SERIALIZABLE_RW)).getValue();
            TransactionalMultiWriter writer = client.createTransactionalMultiWriter(settings(), tx);
            writers.add(writer);
            exercise(writer, tx, session);
        }
    }

    MultiWriterSettings settings() {
        return MultiWriterSettings.newBuilder().setTopicPath(path).setProducerPrefix("observed")
                .setRoutingMode(MultiWriterSettings.RoutingMode.BOUND).setCodec(Codec.RAW)
                .setInitTimeout(Duration.ofSeconds(10)).setRecoveryTimeout(Duration.ofSeconds(10))
                .setCloseTimeout(Duration.ofSeconds(5)).build();
    }

    private void exercise(AsyncMultiWriter writer, TableTransaction tx, Session session) throws Exception {
        await(writer.init());
        CompletableFuture<MultiWriteResult> a = writer.send(MultiMessage.of(bytes("A"), KEY_A));
        CompletableFuture<MultiWriteResult> b = writer.send(MultiMessage.of(bytes("B"), KEY_B));
        if (fault == MultiWriterFaultInterceptor.Fault.BEFORE_STAGING
                || fault == MultiWriterFaultInterceptor.Fault.LOST_ACK) {
            Assert.assertTrue("Real fault point not reached", PROXY.reached().await(10, TimeUnit.SECONDS));
        } else {
            await(a);
            await(b);
        }
        if (split) {
            splitParentZero();
        }
        PROXY.release();
        if (tx == null) {
            await(a);
            await(b);
            await(writer.flush());
            await(writer.shutdown());
            assertCount(readToEnds(), 1);
            return;
        }
        assertCount(readToEnds(), 0);
        Status completion;
        if (rollback) {
            completion = await(tx.rollback());
            completion.expectSuccess();
        } else {
            await(((TransactionalMultiWriter) writer).prepareCommit());
            await(a);
            await(b);
            completion = await(tx.commit());
            if (fault == MultiWriterFaultInterceptor.Fault.LOST_COMMIT) {
                Assert.assertFalse(completion.isSuccess());
                Assert.assertEquals(0, PROXY.reached().getCount());
            } else if (split && fault != MultiWriterFaultInterceptor.Fault.BEFORE_STAGING) {
                Assert.assertEquals(StatusCode.ABORTED, completion.getCode());
            } else {
                completion.expectSuccess();
            }
        }
        LOGGER.info("{} transaction outcome {}", scenario, completion);
        int visible = !rollback && (completion.isSuccess()
                || fault == MultiWriterFaultInterceptor.Fault.LOST_COMMIT) ? 1 : 0;
        assertCount(readToEnds(), visible);
        if (scenario.endsWith("application_retry")) {
            Assert.assertEquals(StatusCode.ABORTED, completion.getCode());
            TableTransaction retryTx = await(session.beginTransaction(TxMode.SERIALIZABLE_RW)).getValue();
            TransactionalMultiWriter retry = client.createTransactionalMultiWriter(settings(), retryTx);
            writers.add(retry);
            await(retry.init());
            retry.send(MultiMessage.of(bytes("A"), KEY_A));
            retry.send(MultiMessage.of(bytes("B"), KEY_B));
            await(retry.prepareCommit());
            await(retryTx.commit()).expectSuccess();
            assertCount(readToEnds(), 1);
        }
    }

    void splitParentZero() throws Exception {
        AsyncWriter first = filler("filler");
        AsyncWriter second = filler("filler2");
        try {
            await(first.init());
            await(second.init());
            byte[] payload = new byte[256 << 10];
            Arrays.fill(payload, (byte) 'f');
            long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(15);
            while (System.nanoTime() < deadline) {
                try {
                    await(first.send(Message.newBuilder().setData(payload)
                            .addMetadataItem(new MetadataItem("__partition_key", new byte[] {16})).build()));
                    await(second.send(Message.newBuilder().setData(payload)
                            .addMetadataItem(new MetadataItem("__partition_key", new byte[] {96})).build()));
                } catch (Exception ex) {
                    LOGGER.info("{} filler outcome {}", scenario, ex.toString());
                }
                List<PartitionInfo> graph = describe(false);
                for (PartitionInfo partition : graph) {
                    if (partition.getPartitionId() == 0 && !partition.isActive()
                            && !partition.getChildPartitionIds().isEmpty()) {
                        await(client.alterTopic(path, AlterTopicSettings.newBuilder()
                                .setAlterPartitioningSettings(AlterPartitioningSettings.newBuilder()
                                        .setAutoPartitioningStrategy(AutoPartitioningStrategy.PAUSED).build())
                                .build())).expectSuccess();
                        logGraph("confirmed split", graph);
                        return;
                    }
                }
                TimeUnit.MILLISECONDS.sleep(50);
            }
            Assert.fail("Server-confirmed split was not observed");
        } finally {
            await(first.shutdown());
            await(second.shutdown());
        }
    }

    private AsyncWriter filler(String producer) {
        return client.createAsyncWriter(WriterSettings.newBuilder().setTopicPath(path).setProducerId(producer)
                .setPartitionId(0).setCodec(Codec.RAW)
                .setRetryConfig(status -> status.getCode() == StatusCode.OVERLOADED
                        ? null : TopicRetryConfig.STANDARD.getStatusRetryPolicy(status)).build());
    }

    List<PartitionInfo> describe(boolean stats) throws Exception {
        return await(client.describeTopic(path, DescribeTopicSettings.newBuilder().withIncludeStats(stats).build()))
                .getValue().getPartitions();
    }

    Map<String, Integer> readToEnds() throws Exception {
        List<PartitionInfo> graph = describe(true);
        Map<Long, Long> ends = new HashMap<>();
        List<Long> ids = new ArrayList<>();
        long count = 0;
        for (PartitionInfo partition : graph) {
            long end = partition.getPartitionStats().getPartitionOffsets().getEnd();
            long start = partition.getPartitionStats().getPartitionOffsets().getStart();
            Assert.assertEquals("Fresh topic must retain offset zero", 0, start);
            ends.put(partition.getPartitionId(), end);
            ids.add(partition.getPartitionId());
            count += end - start;
        }
        LOGGER.info("{} terminal offsets {} physical count {}", scenario, ends, count);
        Map<String, Integer> contents = new HashMap<>();
        if (count == 0) {
            return contents;
        }
        SyncReader reader = client.createSyncReader(ReaderSettings.newBuilder().setConsumerName("parity")
                .addTopic(TopicReadSettings.newBuilder().setPath(path).setPartitionIds(ids).build()).build());
        Map<Long, Long> reached = new HashMap<>();
        try {
            reader.init();
            while (count-- > 0) {
                tech.ydb.topic.read.Message message = reader.receive(10, TimeUnit.SECONDS);
                Assert.assertNotNull("Every partition must reach its recorded end offset", message);
                long partition = message.getPartitionSession().getPartitionId();
                Assert.assertTrue("Read beyond final offset", message.getOffset() < ends.get(partition));
                reached.put(partition, message.getOffset() + 1);
                String value = new String(message.getData(), StandardCharsets.UTF_8);
                contents.put(value, contents.getOrDefault(value, 0) + 1);
            }
            for (Map.Entry<Long, Long> end : ends.entrySet()) {
                Assert.assertEquals("Partition not fully drained: " + end.getKey(), end.getValue(),
                        reached.getOrDefault(end.getKey(), 0L));
            }
        } finally {
            reader.shutdown();
        }
        return contents;
    }

    private void assertCount(Map<String, Integer> values, int expected) {
        LOGGER.info("{} physical A={} B={}", scenario, values.getOrDefault("A", 0), values.getOrDefault("B", 0));
        Assert.assertEquals(expected, (int) values.getOrDefault("A", 0));
        Assert.assertEquals(expected, (int) values.getOrDefault("B", 0));
    }

    private void logGraph(String phase, List<PartitionInfo> graph) {
        for (PartitionInfo partition : graph) {
            LOGGER.info("{} {} partition={} active={} parents={} children={} bounds={}/{}", scenario, phase,
                    partition.getPartitionId(), partition.isActive(), partition.getParentPartitionIds(),
                    partition.getChildPartitionIds(), Arrays.toString(partition.getFromBound()),
                    Arrays.toString(partition.getToBound()));
        }
    }

    private static byte[] bytes(String value) {
        return value.getBytes(StandardCharsets.UTF_8);
    }

    private static <T> T await(CompletableFuture<T> future) throws Exception {
        return future.get(15, TimeUnit.SECONDS);
    }
}
