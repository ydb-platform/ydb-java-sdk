package tech.ydb.query.impl;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tech.ydb.common.transaction.TxMode;
import tech.ydb.core.Result;
import tech.ydb.core.Status;
import tech.ydb.core.StatusCode;
import tech.ydb.query.QueryClient;
import tech.ydb.query.QuerySession;
import tech.ydb.query.QueryTransaction;
import tech.ydb.query.result.QueryInfo;
import tech.ydb.query.result.QueryResultPart;
import tech.ydb.query.settings.ExecuteQuerySettings;
import tech.ydb.query.settings.QueryExecMode;
import tech.ydb.query.settings.QueryStatsMode;
import tech.ydb.query.tools.QueryReader;
import tech.ydb.table.SessionRetryContext;
import tech.ydb.table.description.TableDescription;
import tech.ydb.table.impl.SimpleTableClient;
import tech.ydb.table.query.Params;
import tech.ydb.table.result.ResultSetReader;
import tech.ydb.table.rpc.grpc.GrpcTableRpc;
import tech.ydb.table.values.PrimitiveType;
import tech.ydb.table.values.PrimitiveValue;
import tech.ydb.test.junit4.GrpcTransportRule;

/**
 *
 * @author Aleksandr Gorshenin
 */
public class QueryIntegrationTest {
    private final static Logger logger = LoggerFactory.getLogger(QueryIntegrationTest.class);
    private final static String TEST_TABLE = "query_service_test";
    private final static String TEST_DOUBLE_TABLE = "query_double_table";

    private final static byte[] BYTES_EMPTY = new byte[0];
    private final static byte[] BYTES_LEN2 = new byte[] { 0x00, 0x22 };
    private final static byte[] BYTES_LEN5 = new byte[] { 0x12, 0x23, 0x34, 0x45, 0x56 };

    private final static Duration SESSION_TIMEOUT = Duration.ofSeconds(5);

    private static class Entity {
        private final int id;
        private final String name;
        private final byte[] payload;
        private final boolean isValid;

        public Entity(int id, String name, byte[] payload, boolean isValid) {
            this.id = id;
            this.name = name;
            this.payload = payload;
            this.isValid = isValid;
        }
    }

    @ClassRule
    public final static GrpcTransportRule ydbTransport = new GrpcTransportRule();

    private static QueryClient queryClient = null;

    @BeforeClass
    public static void initSchema() {
        logger.info("Prepare database...");

        String tablePath = ydbTransport.getDatabase() + "/" + TEST_TABLE;
        TableDescription tableDescription = TableDescription.newBuilder()
                .addNonnullColumn("id", PrimitiveType.Int32)
                .addNullableColumn("name", PrimitiveType.Text)
                .addNullableColumn("payload", PrimitiveType.Bytes)
                .addNullableColumn("is_valid", PrimitiveType.Bool)
                .setPrimaryKey("id")
                .build();


        String table2Path = ydbTransport.getDatabase() + "/" + TEST_DOUBLE_TABLE;
        TableDescription table2Description = TableDescription.newBuilder()
                .addNonnullColumn("id", PrimitiveType.Int32)
                .addNullableColumn("amount", PrimitiveType.Double)
                .setPrimaryKey("id")
                .build();

        SimpleTableClient client = SimpleTableClient.newClient(GrpcTableRpc.useTransport(ydbTransport)).build();
        SessionRetryContext retryCtx = SessionRetryContext.create(client).build();
        retryCtx.supplyStatus(session -> session.createTable(tablePath, tableDescription)).join();
        retryCtx.supplyStatus(session -> session.createTable(table2Path, table2Description)).join();
        logger.info("Prepare database OK");

        queryClient = QueryClient.newClient(ydbTransport).build();
    }

    @AfterClass
    public static void dropAll() {
        try {
            logger.info("Clean database...");
            String tablePath = ydbTransport.getDatabase() + "/" + TEST_TABLE;
            String table2Path = ydbTransport.getDatabase() + "/" + TEST_DOUBLE_TABLE;

            SimpleTableClient client = SimpleTableClient.newClient(GrpcTableRpc.useTransport(ydbTransport)).build();
            SessionRetryContext retryCtx = SessionRetryContext.create(client).build();
            retryCtx.supplyStatus(session -> session.dropTable(tablePath)).join().isSuccess();
            retryCtx.supplyStatus(session -> session.dropTable(table2Path)).join().isSuccess();
            logger.info("Clean database OK");
        } finally {
            queryClient.close();
        }
    }

    @Test
    public void testSimpleSelect() {
        try (QuerySession session = queryClient.createSession(Duration.ofSeconds(5)).join().getValue()) {
            QueryReader reader = QueryReader.readFrom(
                    session.createQuery("SELECT 2 + 3;", TxMode.SERIALIZABLE_RW)
            ).join().getValue();


            Assert.assertEquals(1, reader.getResultSetCount());
            ResultSetReader rs = reader.getResultSet(0);

            Assert.assertTrue(rs.next());
            Assert.assertEquals(1, rs.getColumnCount());
            Assert.assertEquals("column0", rs.getColumnName(0));
            Assert.assertEquals(5, rs.getColumn(0).getInt32());

            Assert.assertFalse(rs.next());
        }
    }

    @Test
    @Ignore
    public void testSimplePrepare() {
        String query = ""
                + "DECLARE $id AS Int32?;\n"
                + "UPSERT INTO `" + TEST_TABLE + "` (id) "
                + "VALUES ($id)";
        try (QuerySession session = queryClient.createSession(Duration.ofSeconds(5)).join().getValue()) {
            ExecuteQuerySettings settings = ExecuteQuerySettings.newBuilder()
                    .withExecMode(QueryExecMode.PARSE)
                    .build();

            QueryReader reader = QueryReader.readFrom(
                    session.createQuery(query, TxMode.NONE, Params.empty(), settings)
            ).join().getValue();


            Assert.assertEquals(1, reader.getResultSetCount());
            ResultSetReader rs = reader.getResultSet(0);

            Assert.assertTrue(rs.next());
            Assert.assertEquals(1, rs.getColumnCount());
            Assert.assertEquals("column0", rs.getColumnName(0));
            Assert.assertEquals(5, rs.getColumn(0).getInt32());

            Assert.assertFalse(rs.next());
        }
    }

    @Test
    public void testErrors() {
        SessionImpl s1 = (SessionImpl)queryClient.createSession(Duration.ofSeconds(5)).join().getValue();
        String id = s1.getId();
        s1.close();

        SessionImpl s2 = (SessionImpl)queryClient.createSession(Duration.ofSeconds(5)).join().getValue();
        Assert.assertEquals(id, s2.getId());
        s2.updateSessionState(Status.of(StatusCode.ABORTED));
        s2.close();

        SessionImpl s3 = (SessionImpl)queryClient.createSession(Duration.ofSeconds(5)).join().getValue();
        Assert.assertEquals(id, s3.getId());
        s3.updateSessionState(Status.of(StatusCode.BAD_SESSION));
        s3.close();

        SessionImpl s4 = (SessionImpl)queryClient.createSession(Duration.ofSeconds(5)).join().getValue();
        Assert.assertNotEquals(id, s4.getId());
        s4.close();
    }

    @Test
    public void testSimpleCRUD() {
        List<Entity> entities = new ArrayList<>();
        entities.add(new Entity(1, "entity 1", BYTES_EMPTY, true));
        entities.add(new Entity(2, "entity 2", BYTES_EMPTY, true));
        entities.add(new Entity(3, "entity 3", BYTES_LEN2, false));
        entities.add(new Entity(3, "dublicate", BYTES_LEN5, true));
        entities.add(new Entity(5, "entity 5", BYTES_LEN2, false));

        for (Entity entity: entities) {
            String query = "UPSERT INTO `" + TEST_TABLE + "` (id, name, payload, is_valid) "
                    + "VALUES ($id, $name, $payload, $is_valid)";

            Params params = Params.of(
                    "$id", PrimitiveValue.newInt32(entity.id),
                    "$name", PrimitiveValue.newText(entity.name),
                    "$payload", PrimitiveValue.newBytes(entity.payload),
                    "$is_valid", PrimitiveValue.newBool(entity.isValid)
            );

            try (QuerySession session = queryClient.createSession(SESSION_TIMEOUT).join().getValue()) {
                session.createQuery(query, TxMode.SERIALIZABLE_RW, params)
                        .execute(this::printQuerySetPart)
                        .join().getStatus().expectSuccess();
            }
        }

        try (QuerySession session = queryClient.createSession(Duration.ofSeconds(5)).join().getValue()) {
            String query = "SELECT id, name, payload, is_valid FROM " + TEST_TABLE + " ORDER BY id;";
            session.createQuery(query, TxMode.SERIALIZABLE_RW)
                    .execute(this::printQuerySetPart)
                    .join().getStatus().expectSuccess();
        }

        try (QuerySession session = queryClient.createSession(SESSION_TIMEOUT).join().getValue()) {
            session.createQuery("DELETE FROM " + TEST_TABLE, TxMode.SERIALIZABLE_RW)
                    .execute(this::printQuerySetPart)
                    .join().getStatus().expectSuccess();
        }
    }

    public void printQuerySetPart(QueryResultPart part) {
        ResultSetReader rs = part.getResultSetReader();
        if (rs != null) {
            logger.info("got query result part with index {} and {} rows", part.getResultSetIndex(), rs.getRowCount());
        }
    }

    @Test
    @Ignore
    public void updateMultipleTablesInOneTransaction() {
        try (QueryClient client = QueryClient.newClient(ydbTransport).build()) {
            try (QuerySession session = client.createSession(Duration.ofSeconds(5)).join().getValue()) {
                QueryTransaction tx = session.createNewTransaction(TxMode.SERIALIZABLE_RW);
                Assert.assertFalse(tx.isActive());
                QueryReader.readFrom(
                        tx.createQuery("UPDATE " + TEST_TABLE + " SET name='test' WHERE id=1")
                ).join().getStatus().expectSuccess();
                Assert.assertTrue(tx.isActive());

                QueryReader.readFrom(
                        tx.createQueryWithCommit("UPDATE " + TEST_DOUBLE_TABLE + " SET amount=300 WHERE id=1")
                ).join().getStatus().expectSuccess();
                Assert.assertFalse(tx.isActive());
            }
        }
    }

    @Test
    public void interactiveTransaction() {
        try (QueryClient client = QueryClient.newClient(ydbTransport).build()) {
            try (QuerySession session = client.createSession(Duration.ofSeconds(5)).join().getValue()) {
                QueryTransaction tx = session.createNewTransaction(TxMode.SERIALIZABLE_RW);
                Assert.assertFalse(tx.isActive());
                tx.createQuery("INSERT INTO " + TEST_TABLE + " (id, name) VALUES (1, 'rec1');").execute(null)
                        .join().getStatus().expectSuccess();
                Assert.assertTrue(tx.isActive());
                tx.createQuery("INSERT INTO " + TEST_TABLE + " (id, name) VALUES (3, 'rec3');").execute(null)
                        .join().getStatus().expectSuccess();
                Assert.assertTrue(tx.isActive());

                Iterator<ResultSetReader> rsIter = QueryReader.readFrom(
                        tx.createQuery("SELECT id, name FROM " + TEST_TABLE + " ORDER BY id")
                ).join().getValue().iterator();
                Assert.assertTrue(tx.isActive());

                Assert.assertTrue(rsIter.hasNext());
                ResultSetReader rs = rsIter.next();
                Assert.assertTrue(rs.next());
                Assert.assertEquals(1, rs.getColumn("id").getInt32());
                Assert.assertTrue(rs.next());
                Assert.assertEquals(3, rs.getColumn("id").getInt32());
                Assert.assertFalse(rs.next());
                Assert.assertFalse(rsIter.hasNext());

                tx.commit().join().getStatus().expectSuccess();
                Assert.assertFalse(tx.isActive());

                tx.createQuery("INSERT INTO " + TEST_TABLE + " (id, name) VALUES (2, 'rec2');").execute(null)
                        .join().getStatus().expectSuccess();

                rsIter = QueryReader.readFrom(
                        tx.createQuery("SELECT id, name FROM " + TEST_TABLE + " ORDER BY id")
                ).join().getValue().iterator();

                Assert.assertTrue(rsIter.hasNext());
                rs = rsIter.next();
                Assert.assertTrue(rs.next());
                Assert.assertEquals(1, rs.getColumn("id").getInt32());
                Assert.assertTrue(rs.next());
                Assert.assertEquals(2, rs.getColumn("id").getInt32());
                Assert.assertTrue(rs.next());
                Assert.assertEquals(3, rs.getColumn("id").getInt32());
                Assert.assertFalse(rs.next());
                Assert.assertFalse(rsIter.hasNext());

                tx.rollback().join().expectSuccess();

                rsIter = QueryReader.readFrom(
                        tx.createQueryWithCommit("SELECT id, name FROM " + TEST_TABLE + " ORDER BY id")
                ).join().getValue().iterator();

                Assert.assertTrue(rsIter.hasNext());
                rs = rsIter.next();
                Assert.assertTrue(rs.next());
                Assert.assertEquals(1, rs.getColumn("id").getInt32());
                Assert.assertTrue(rs.next());
                Assert.assertEquals(3, rs.getColumn("id").getInt32());
                Assert.assertFalse(rs.next());
                Assert.assertFalse(rsIter.hasNext());

                QueryInfo info = tx.createQuery("DELETE FROM " + TEST_TABLE, true, Params.empty(),
                        ExecuteQuerySettings.newBuilder().withStatsMode(QueryStatsMode.FULL).build()
                ).execute(null).join().getValue();
                Assert.assertTrue(info.hasStats());
                logger.info("got stats {}", info.getStats());
            }
        }
    }

    @Test
    public void testSchemeQuery() {
        try (QueryClient client = QueryClient.newClient(ydbTransport).build()) {
            try (QuerySession session = client.createSession(Duration.ofSeconds(5)).join().getValue()) {
                CompletableFuture<Result<QueryInfo>> createTable = session
                        .createQuery("CREATE TABLE demo_table (id Int32, data Text, PRIMARY KEY(id));", TxMode.NONE)
                        .execute(this::printQuerySetPart);
                createTable.join().getStatus().expectSuccess();


                CompletableFuture<Result<QueryInfo>> dropTable = session
                        .createQuery("DROP TABLE demo_table;", TxMode.NONE)
                        .execute(this::printQuerySetPart);
                dropTable.join().getStatus().expectSuccess();
            }
        }
    }

    @Test
    public void testQueryStats() {
        try (QueryClient client = QueryClient.newClient(ydbTransport).build()) {
            try (QuerySession session = client.createSession(Duration.ofSeconds(5)).join().getValue()) {
                CompletableFuture<Result<QueryInfo>> createTable = session
                        .createQuery("CREATE TABLE demo_table (id Int32, data Text, PRIMARY KEY(id));", TxMode.NONE)
                        .execute(this::printQuerySetPart);
                createTable.join().getStatus().expectSuccess();


                CompletableFuture<Result<QueryInfo>> dropTable = session
                        .createQuery("DROP TABLE demo_table;", TxMode.NONE)
                        .execute(this::printQuerySetPart);
                dropTable.join().getStatus().expectSuccess();
            }
        }
    }

    @Test
    public void testQueryWarngins() {
        try (QueryClient client = QueryClient.newClient(ydbTransport).build()) {
            try (QuerySession session = client.createSession(Duration.ofSeconds(5)).join().getValue()) {
                CompletableFuture<Result<QueryInfo>> createTable = session
                        .createQuery("CREATE TABLE demo_idx("
                                + "id Int32, value Int32, "
                                + "PRIMARY KEY(id), INDEX idx_value GLOBAL ON(value)"
                                + ");", TxMode.NONE)
                        .execute(this::printQuerySetPart);
                createTable.join().getStatus().expectSuccess();

                try {
                    Result<QueryReader> result = QueryReader.readFrom(session.createQuery(
                            "SELECT * FROM demo_idx VIEW idx_value WHERE id = 1", TxMode.SERIALIZABLE_RW
                    )).join();

                    Assert.assertTrue(result.isSuccess());
                    Assert.assertTrue(result.getStatus().getIssues().length == 0);

                    QueryReader reader = result.getValue();
                    Assert.assertEquals(1, reader.getIssueList().size());
                    Assert.assertEquals("#1060 Execution (S_WARNING)\n  1:1 - 1:1: "
                            + "#2503 Given predicate is not suitable for used index: idx_value (S_WARNING)",
                            reader.getIssueList().get(0).toString()
                    );
                } finally {
                    CompletableFuture<Result<QueryInfo>> dropTable = session
                            .createQuery("DROP TABLE demo_idx;", TxMode.NONE)
                            .execute(this::printQuerySetPart);
                    dropTable.join().getStatus().expectSuccess();
                }
            }
        }
    }
}
