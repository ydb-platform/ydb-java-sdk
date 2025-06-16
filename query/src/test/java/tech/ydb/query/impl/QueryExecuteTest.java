package tech.ydb.query.impl;

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

import com.google.common.hash.Hashing;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import tech.ydb.common.transaction.TxMode;
import tech.ydb.core.Result;
import tech.ydb.core.grpc.GrpcFlowControl;
import tech.ydb.query.QueryClient;
import tech.ydb.query.QuerySession;
import tech.ydb.query.QueryStream;
import tech.ydb.query.result.QueryInfo;
import tech.ydb.query.settings.ExecuteQuerySettings;
import tech.ydb.table.SessionRetryContext;
import tech.ydb.table.description.TableDescription;
import tech.ydb.table.impl.SimpleTableClient;
import tech.ydb.table.query.Params;
import tech.ydb.table.rpc.grpc.GrpcTableRpc;
import tech.ydb.table.settings.BulkUpsertSettings;
import tech.ydb.table.values.ListType;
import tech.ydb.table.values.PrimitiveType;
import tech.ydb.table.values.PrimitiveValue;
import tech.ydb.table.values.StructType;
import tech.ydb.table.values.StructValue;
import tech.ydb.test.junit4.GrpcTransportRule;


/**
 *
 * @author Aleksandr Gorshenin
 */
public class QueryExecuteTest {
    @ClassRule
    public static final GrpcTransportRule YDB = new GrpcTransportRule();
    private static final String TEST_TABLE = "big_table_test";
    private static final long TEST_TABLE_SIZE = 5000;

    private static byte[] pseudoRndData(long seed) {
        Random rnd = new Random(seed);
        int length = rnd.nextInt(256) + 256;
        byte[] data = new byte[length];
        rnd.nextBytes(data);
        return data;
    }

    @BeforeClass
    public static void prepareTable() {
        SimpleTableClient client = SimpleTableClient.newClient(GrpcTableRpc.useTransport(YDB)).build();
        SessionRetryContext retryCtx = SessionRetryContext.create(client).build();
        String tablePath = YDB.getDatabase() + "/" + TEST_TABLE;

        TableDescription tableDesc = TableDescription.newBuilder()
                .addNonnullColumn("id", PrimitiveType.Int64)
                .addNonnullColumn("hash", PrimitiveType.Text)
                .addNullableColumn("data", PrimitiveType.Bytes)
                .setPrimaryKey("hash")
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

        retryCtx.supplyStatus(session -> session.executeBulkUpsert(
                tablePath, ListType.of(batchType).newValue(batchData), new BulkUpsertSettings())
        ).join().expectSuccess("bulk upsert problem in table " + tablePath);
    }

    @AfterClass
    public static void dropTable() {
        String tablePath = YDB.getDatabase() + "/" + TEST_TABLE;
        SimpleTableClient client = SimpleTableClient.newClient(GrpcTableRpc.useTransport(YDB)).build();
        SessionRetryContext retryCtx = SessionRetryContext.create(client).build();
        retryCtx.supplyStatus(session -> session.dropTable(tablePath)).join();
    }

    @Test
    public void streamReadTest() {
        AtomicLong rowReaded = new AtomicLong(0);

        try (QueryClient client = QueryClient.newClient(YDB).build()) {
            try (QuerySession session = client.createSession(Duration.ofSeconds(5)).join().getValue()) {
                session.createQuery("SELECT * FROM " + TEST_TABLE, TxMode.NONE).execute(part -> {
                    rowReaded.addAndGet(part.getResultSetReader().getRowCount());
                }).join().getStatus().expectSuccess("Cannot execute query");
            }
        }

        Assert.assertEquals(TEST_TABLE_SIZE, rowReaded.get());
    }


    @Test
    public void flowControlTest() {
        AtomicLong rowReaded = new AtomicLong(0);
        TestFlowCall flow = new TestFlowCall();

        ExecuteQuerySettings settings = ExecuteQuerySettings.newBuilder()
                .withPartBytesLimit(1000)
                .withGrpcFlowControl(flow)
                .build();

        try (QueryClient client = QueryClient.newClient(YDB).build()) {
            try (QuerySession session = client.createSession(Duration.ofSeconds(5)).join().getValue()) {
                String query = "SELECT * FROM " + TEST_TABLE;
                QueryStream stream = session.createQuery(query, TxMode.NONE, Params.empty(), settings);
                Assert.assertTrue(flow.isExists());
                Assert.assertFalse(flow.isStarted());

                CompletableFuture<Result<QueryInfo>> res = stream.execute(part -> {
                    rowReaded.addAndGet(part.getResultSetReader().getRowCount());
                });

                Assert.assertTrue(flow.isStarted());

                int requested = 0;
                long readed = rowReaded.get();
                Assert.assertEquals(0l, readed);

                while (readed < TEST_TABLE_SIZE) {
                    flow.requestNext(1);
                    requested++;
                    flow.waitUntil(requested);
                    Assert.assertTrue(rowReaded.get() > readed);
                    readed = rowReaded.get();
                }

                Assert.assertTrue(res.join().isSuccess());
            }
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
            public void onMessageReaded() {
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
