package tech.ydb.topic;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
import org.junit.rules.TestName;
import org.junit.rules.Timeout;
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
import tech.ydb.topic.description.PartitionInfo;
import tech.ydb.topic.read.SyncReader;
import tech.ydb.topic.settings.MultiWriterSettings;
import tech.ydb.topic.settings.ReaderSettings;
import tech.ydb.topic.settings.TopicReadSettings;
import tech.ydb.topic.write.AsyncMultiWriter;
import tech.ydb.topic.write.MultiMessage;
import tech.ydb.topic.write.MultiWriteResult;
import tech.ydb.topic.write.TransactionalMultiWriter;

/** Ordering follows partition ancestry and offsets, never arrival order from independent streams. */
public class MultiWriterOrderingIntegrationTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(MultiWriterOrderingIntegrationTest.class);

    @ClassRule
    public static final GrpcTransportRule YDB = MultiWriterIntegrationTest.YDB;

    @Rule
    public final Timeout timeout = Timeout.seconds(90);

    @Rule
    public final TestName name = new TestName();

    private MultiWriterIntegrationTest fixture;

    @BeforeClass
    public static void openClient() {
        MultiWriterIntegrationTest.openClient();
    }

    @AfterClass
    public static void closeClient() {
        MultiWriterIntegrationTest.closeClient();
    }

    @Before
    public void createTopic() throws Exception {
        MultiWriterFaultInterceptor.Fault fault = name.getMethodName().equals("ordinarySplitLostAckOrder")
                ? MultiWriterFaultInterceptor.Fault.LOST_ACK : MultiWriterFaultInterceptor.Fault.NONE;
        fixture = new MultiWriterIntegrationTest(name.getMethodName(), false, false, false, fault);
        fixture.createTopic();
    }

    @After
    public void cleanup() throws Exception {
        fixture.cleanup();
    }

    @Test
    public void ordinarySplitLostAckOrder() throws Exception {
        AsyncMultiWriter writer = MultiWriterIntegrationTest.client.createAsyncMultiWriter(fixture.settings());
        fixture.writers.add(writer);
        await(writer.init());
        CompletableFuture<MultiWriteResult> a = writer.send(message("A"));
        Assert.assertTrue(MultiWriterIntegrationTest.PROXY.reached().await(10, TimeUnit.SECONDS));
        fixture.splitParentZero();
        CompletableFuture<MultiWriteResult> b = writer.send(message("B"));
        MultiWriterIntegrationTest.PROXY.release();
        await(a);
        await(b);
        await(writer.send(message("C")));
        await(writer.flush());
        await(writer.shutdown());
        assertPositions(Arrays.asList("A", "B", "C"));
    }

    @Test
    public void reverseTransactionCommit() throws Exception {
        completeAfterOtherWrite(true);
    }

    @Test
    public void ordinaryDuringTransaction() throws Exception {
        completeAfterOtherWrite(false);
    }

    private void completeAfterOtherWrite(boolean secondTransaction) throws Exception {
        try (TableClient table = TableClient.newClient(YDB).build();
                Session firstSession = await(table.createSession(Duration.ofSeconds(5))).getValue();
                Session secondSession = await(table.createSession(Duration.ofSeconds(5))).getValue()) {
            TableTransaction first = await(firstSession.beginTransaction(TxMode.SERIALIZABLE_RW)).getValue();
            TransactionalMultiWriter firstWriter = MultiWriterIntegrationTest.client
                    .createTransactionalMultiWriter(settings("observed-first"), first);
            fixture.writers.add(firstWriter);
            await(firstWriter.init());
            await(firstWriter.send(message("A")));
            await(firstWriter.prepareCommit());
            if (secondTransaction) {
                TableTransaction second = await(secondSession.beginTransaction(TxMode.SERIALIZABLE_RW)).getValue();
                TransactionalMultiWriter secondWriter = MultiWriterIntegrationTest.client
                        .createTransactionalMultiWriter(settings("observed-second"), second);
                fixture.writers.add(secondWriter);
                await(secondWriter.init());
                await(secondWriter.send(message("B")));
                await(secondWriter.prepareCommit());
                Assert.assertTrue("Both transactions are staged", fixture.readToEnds().isEmpty());
                Status secondStatus = await(second.commit());
                LOGGER.info("{} second transaction commit {}", name.getMethodName(), secondStatus);
                secondStatus.expectSuccess();
            } else {
                AsyncMultiWriter ordinary = MultiWriterIntegrationTest.client
                        .createAsyncMultiWriter(settings("observed-second"));
                fixture.writers.add(ordinary);
                await(ordinary.init());
                await(ordinary.send(message("B")));
                await(ordinary.shutdown());
            }
            Map<String, Integer> before = fixture.readToEnds();
            Assert.assertEquals(0, (int) before.getOrDefault("A", 0));
            Assert.assertEquals(1, (int) before.getOrDefault("B", 0));
            Status firstStatus = await(first.commit());
            LOGGER.info("{} first transaction commit {}", name.getMethodName(), firstStatus);
            if (firstStatus.isSuccess()) {
                assertPositions(Arrays.asList("B", "A"));
            } else {
                Assert.assertEquals("Only a server conflict is an expected alternative", StatusCode.ABORTED,
                        firstStatus.getCode());
                assertPositions(Arrays.asList("B"));
            }
        }
    }

    private MultiWriterSettings settings(String prefix) {
        return MultiWriterSettings.newBuilder().setTopicPath(fixture.path).setProducerPrefix(prefix)
                .setRoutingMode(MultiWriterSettings.RoutingMode.BOUND).setCodec(Codec.RAW)
                .setInitTimeout(Duration.ofSeconds(10)).setRecoveryTimeout(Duration.ofSeconds(10))
                .setCloseTimeout(Duration.ofSeconds(5)).build();
    }

    private void assertPositions(List<String> expected) throws Exception {
        List<PartitionInfo> graph = fixture.describe(true);
        Map<Long, Long> ends = new HashMap<>();
        Map<Long, List<Long>> children = new HashMap<>();
        List<Long> ids = new ArrayList<>();
        long remaining = 0;
        for (PartitionInfo partition : graph) {
            Assert.assertEquals(0, partition.getPartitionStats().getPartitionOffsets().getStart());
            long end = partition.getPartitionStats().getPartitionOffsets().getEnd();
            ends.put(partition.getPartitionId(), end);
            ids.add(partition.getPartitionId());
            children.put(partition.getPartitionId(), partition.getChildPartitionIds());
            remaining += end;
        }
        LOGGER.info("{} ordering terminal offsets {} physical count {}", name.getMethodName(), ends, remaining);
        SyncReader reader = MultiWriterIntegrationTest.client.createSyncReader(ReaderSettings.newBuilder()
                .setConsumerName("parity").addTopic(TopicReadSettings.newBuilder()
                        .setPath(fixture.path).setPartitionIds(ids).build()).build());
        Map<Long, Long> reached = new HashMap<>();
        Map<String, Position> targets = new HashMap<>();
        try {
            reader.init();
            while (remaining-- > 0) {
                tech.ydb.topic.read.Message message = reader.receive(10, TimeUnit.SECONDS);
                Assert.assertNotNull("Each partition must reach its end offset", message);
                long partition = message.getPartitionSession().getPartitionId();
                Assert.assertEquals("Physical offset gap or duplicate", (long) reached.getOrDefault(partition, 0L),
                        message.getOffset());
                Assert.assertTrue(message.getOffset() < ends.get(partition));
                reached.put(partition, message.getOffset() + 1);
                String value = new String(message.getData(), StandardCharsets.UTF_8);
                if (value.length() == 1) {
                    Assert.assertNull("Physical duplicate: " + value,
                            targets.put(value, new Position(partition, message.getOffset())));
                }
            }
            for (Map.Entry<Long, Long> end : ends.entrySet()) {
                Assert.assertEquals(end.getValue(), reached.getOrDefault(end.getKey(), 0L));
            }
        } finally {
            reader.shutdown();
        }
        LOGGER.info("{} target positions {}", name.getMethodName(), targets);
        Assert.assertEquals(expected.size(), targets.size());
        Position previous = null;
        for (String value : expected) {
            Position current = targets.get(value);
            Assert.assertNotNull("Missing " + value, current);
            if (previous != null) {
                if (previous.partition == current.partition) {
                    Assert.assertTrue("Offset order", previous.offset < current.offset);
                } else {
                    Assert.assertTrue("Parent must precede child", ancestor(children, previous.partition,
                            current.partition));
                }
            }
            previous = current;
        }
    }

    private boolean ancestor(Map<Long, List<Long>> children, long parent, long child) {
        for (long next : children.get(parent)) {
            if (next == child || ancestor(children, next, child)) {
                return true;
            }
        }
        return false;
    }

    private static MultiMessage message(String value) {
        return MultiMessage.of(value.getBytes(StandardCharsets.UTF_8), "key-2");
    }

    private static <T> T await(CompletableFuture<T> future) throws Exception {
        return future.get(15, TimeUnit.SECONDS);
    }

    private static final class Position {
        final long partition;
        final long offset;

        Position(long partition, long offset) {
            this.partition = partition;
            this.offset = offset;
        }

        @Override
        public String toString() {
            return partition + ":" + offset;
        }
    }
}
