package tech.ydb.table.integration;

import java.time.Duration;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.IntConsumer;
import java.util.stream.Collectors;
import java.util.stream.LongStream;

import javax.annotation.Nonnull;

import com.google.common.hash.Hashing;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import tech.ydb.core.Status;
import tech.ydb.core.grpc.GrpcFlowControl;
import tech.ydb.core.grpc.GrpcReadStream;
import tech.ydb.proto.table.YdbTable;
import tech.ydb.table.Session;
import tech.ydb.table.SessionRetryContext;
import tech.ydb.table.description.TableDescription;
import tech.ydb.table.impl.SimpleTableClient;
import tech.ydb.table.query.BulkUpsertData;
import tech.ydb.table.query.ReadTablePart;
import tech.ydb.table.result.ResultSetReader;
import tech.ydb.table.rpc.grpc.GrpcTableRpc;
import tech.ydb.table.settings.ReadTableSettings;
import tech.ydb.table.values.ListType;
import tech.ydb.table.values.PrimitiveType;
import tech.ydb.table.values.PrimitiveValue;
import tech.ydb.table.values.StructType;
import tech.ydb.table.values.StructValue;
import tech.ydb.table.values.TupleValue;
import tech.ydb.table.values.proto.ProtoValue;
import tech.ydb.test.junit4.GrpcTransportRule;

/**
 *
 * @author Aleksandr Gorshenin
 */
public class ReadTableTest {
    @ClassRule
    public static final GrpcTransportRule YDB = new GrpcTransportRule();
    private static final String TEST_TABLE = "read_table_test";
    private static final long TEST_TABLE_SIZE = 1000;

    private static final SimpleTableClient client = SimpleTableClient.newClient(GrpcTableRpc.useTransport(YDB)).build();
    private static final SessionRetryContext retryCtx = SessionRetryContext.create(client).build();


    @Nonnull
    private static String tablePath(String tableName) {
        return YDB.getDatabase() + "/" + tableName;
    }

    private static byte[] pseudoRndData(long seed) {
        Random rnd = new Random(seed);
        int length = rnd.nextInt(256) + 256;
        byte[] data = new byte[length];
        rnd.nextBytes(data);
        return data;
    }

    @BeforeClass
    public static void prepareTable() {
        String tablePath = tablePath(TEST_TABLE);

        TableDescription tableDesc = TableDescription.newBuilder()
                .addNonnullColumn("id", PrimitiveType.Int64)
                .addNonnullColumn("hash", PrimitiveType.Text)
                .addNullableColumn("data", PrimitiveType.Bytes)
                .setPrimaryKey("id")
                .build();

        retryCtx.supplyStatus(session -> session.createTable(tablePath, tableDesc))
                .join().expectSuccess("Can't create table " + tablePath);

        StructType batchType = StructType.of(
                "id", PrimitiveType.Int64,
                "hash", PrimitiveType.Text,
                "data", PrimitiveType.Bytes
        );
        List<StructValue> batchData = LongStream.range(1, TEST_TABLE_SIZE + 1).mapToObj(id -> {
            byte[] data = pseudoRndData(id);
            String hash = Hashing.sha256().hashBytes(data).toString();
            return batchType.newValue(
                    "id", PrimitiveValue.newInt64(id),
                    "hash", PrimitiveValue.newText(hash),
                    "data", PrimitiveValue.newBytes(data)
            );
        }).collect(Collectors.toList());

        retryCtx.supplyStatus(session -> session.executeBulkUpsert(tablePath,
                new BulkUpsertData(ProtoValue.toTypedValue(ListType.of(batchType).newValue(batchData)))
        )).join().expectSuccess("bulk upsert problem in table " + tablePath);
    }

    @AfterClass
    public static void dropTable() {
        String tablePath = tablePath(TEST_TABLE);
        retryCtx.supplyStatus(session -> session.dropTable(tablePath)).join();
    }

    @Test
    public void readTableTest() {
        String tablePath = tablePath(TEST_TABLE);
        AtomicLong rowsRead = new AtomicLong(0);

        ReadTableSettings rts = ReadTableSettings.newBuilder().column("id").build();
        retryCtx.supplyStatus(session -> {
            rowsRead.set(0);
            return session.executeReadTable(tablePath, rts).start(part -> {
                YdbTable.ReadTableResponse proto = part.getReadTableResponse();
                ResultSetReader rsr = part.getResultSetReader();
                ReadTablePart.VirtualTimestamp vt = part.getVirtualTimestamp();

                Assert.assertNotNull(proto);
                Assert.assertNotNull(rsr);
                Assert.assertNotNull(vt);

                Assert.assertSame(rsr, part.getResultSetReader());
                Assert.assertSame(vt, part.getVirtualTimestamp());

                Assert.assertEquals(proto.getSnapshot().getPlanStep(), vt.getPlanStep());
                Assert.assertEquals(proto.getSnapshot().getTxId(), vt.getTxId());

                rowsRead.addAndGet(part.getResultSetReader().getRowCount());
            });
        }).join().expectSuccess("Cannot read table " + tablePath);

        Assert.assertEquals(TEST_TABLE_SIZE, rowsRead.get());
    }

    @Test
    public void limitedReadTableTest() {
        String tablePath = tablePath(TEST_TABLE);
        AtomicLong rewsRead = new AtomicLong(0);

        ReadTableSettings rts = ReadTableSettings.newBuilder().column("id").batchLimitRows(100).build();
        Assert.assertNull(rts.getFromKey());
        Assert.assertNull(rts.getToKey());
        Assert.assertNull(rts.getFromKeyRaw());
        Assert.assertNull(rts.getToKeyRaw());
        Assert.assertEquals(0, rts.batchLimitBytes());
        Assert.assertEquals(100, rts.batchLimitRows());

        retryCtx.supplyStatus(session -> {
            rewsRead.set(0);
            return session.executeReadTable(tablePath, rts).start(part -> {
                Assert.assertTrue(part.getResultSetReader().getRowCount() <= 100);
                rewsRead.addAndGet(part.getResultSetReader().getRowCount());
            });
        }).join().expectSuccess("Cannot read table " + tablePath);

        Assert.assertEquals(TEST_TABLE_SIZE, rewsRead.get());
    }

    @Test
    public void partialReadTableTest() {
        String tablePath = tablePath(TEST_TABLE);
        AtomicLong rowsRead = new AtomicLong(0);

        PrimitiveValue from = PrimitiveValue.newInt64(1);
        PrimitiveValue to = PrimitiveValue.newInt64(TEST_TABLE_SIZE);
        ReadTableSettings rts = ReadTableSettings.newBuilder().column("id")
                .fromKeyExclusive(from) // always coverted to optional type
                .toKeyExclusive(to)
                .build();

        Assert.assertEquals(TupleValue.of(from.makeOptional()), rts.getFromKey());
        Assert.assertEquals(TupleValue.of(to.makeOptional()), rts.getToKey());
        Assert.assertEquals(ProtoValue.toTypedValue(TupleValue.of(from.makeOptional())), rts.getFromKeyRaw());
        Assert.assertEquals(ProtoValue.toTypedValue(TupleValue.of(to.makeOptional())), rts.getToKeyRaw());
        Assert.assertEquals(0, rts.batchLimitBytes());
        Assert.assertEquals(0, rts.batchLimitRows());

        retryCtx.supplyStatus(session -> {
            rowsRead.set(0);
            return session.executeReadTable(tablePath, rts).start(part -> {
                rowsRead.addAndGet(part.getResultSetReader().getRowCount());
            });
        }).join().expectSuccess("Cannot read table " + tablePath);

        Assert.assertEquals(TEST_TABLE_SIZE - 2, rowsRead.get());

        ReadTableSettings rts2 = ReadTableSettings.newBuilder().column("id")
                .fromKeyInclusive(PrimitiveValue.newInt64(2))
                .toKeyInclusive(PrimitiveValue.newInt64(TEST_TABLE_SIZE - 1))
                .build();
        retryCtx.supplyStatus(session -> {
            rowsRead.set(0);
            return session.executeReadTable(tablePath, rts2).start(part -> {
                rowsRead.addAndGet(part.getResultSetReader().getRowCount());
            });
        }).join().expectSuccess("Cannot read table " + tablePath);

        Assert.assertEquals(TEST_TABLE_SIZE - 2, rowsRead.get());
    }

    @Test
    public void flowControlReadTableTest() {
        String tablePath = tablePath(TEST_TABLE);

        AtomicLong rowsRead = new AtomicLong(0);
        TestFlowCall flow = new TestFlowCall();

        ReadTableSettings rts = ReadTableSettings.newBuilder().column("id")
                .withGrpcFlowControl(flow)
                .batchLimitBytes(5000)
                .build();

        try (Session session = client.createSession(Duration.ofSeconds(5)).join().getValue()) {
            GrpcReadStream<ReadTablePart> stream = session.executeReadTable(tablePath, rts);
            Assert.assertTrue(flow.isExists());
            Assert.assertFalse(flow.isStarted());

            CompletableFuture<Status> res = stream.start(part -> {
                rowsRead.addAndGet(part.getResultSetReader().getRowCount());
            });

            Assert.assertTrue(flow.isStarted());

            int requested = 0;
            long read = rowsRead.get();
            Assert.assertEquals(0l, read);

            while (read < TEST_TABLE_SIZE) {
                flow.requestNext(1);
                requested++;
                flow.waitUntil(requested);
                Assert.assertTrue(rowsRead.get() > read);
                read = rowsRead.get();
            }

            Assert.assertTrue(res.join().isSuccess());
        }
    }

    private class TestFlowCall implements GrpcFlowControl {
        private class CallImpl implements Call{
            private final IntConsumer request;
            private final Semaphore semaphore = new Semaphore(0);
            private boolean isStarted = false;

            CallImpl(IntConsumer request) {
                this.request = request;
            }

            @Override
            public void onStart() {
                isStarted = true;
            }

            @Override
            public void onMessageRead() {
                semaphore.release();
            }
        }

        private final AtomicReference<CallImpl> current = new AtomicReference<>();

        @Override
        public Call newCall(IntConsumer req) {
            CallImpl call = new CallImpl(req);
            current.set(call);
            return call;
        }

        public boolean isExists() {
            return current.get() != null;
        }

        public boolean isStarted() {
            CallImpl c = current.get();
            Assert.assertNotNull(c);
            return c.isStarted;
        }

        public void requestNext(int count) {
            CallImpl c = current.get();
            Assert.assertNotNull(c);
            c.request.accept(count);
        }

        public void waitUntil(int count) {
            CallImpl c = current.get();
            Assert.assertNotNull(c);
            c.semaphore.acquireUninterruptibly(count);
            c.semaphore.release(count);
        }
    }
}
