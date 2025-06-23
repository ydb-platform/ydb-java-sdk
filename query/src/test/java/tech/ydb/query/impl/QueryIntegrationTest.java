package tech.ydb.query.impl;

import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.Channels;
import java.time.Duration;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import com.google.protobuf.ByteString;
import org.apache.arrow.memory.BufferAllocator;
import org.apache.arrow.memory.RootAllocator;
import org.apache.arrow.vector.FieldVector;
import org.apache.arrow.vector.VectorLoader;
import org.apache.arrow.vector.VectorSchemaRoot;
import org.apache.arrow.vector.ipc.ReadChannel;
import org.apache.arrow.vector.ipc.message.ArrowRecordBatch;
import org.apache.arrow.vector.ipc.message.MessageSerializer;
import org.apache.arrow.vector.types.pojo.Field;
import org.apache.arrow.vector.types.pojo.Schema;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tech.ydb.common.transaction.TxMode;
import tech.ydb.core.Issue;
import tech.ydb.core.Result;
import tech.ydb.core.Status;
import tech.ydb.core.StatusCode;
import tech.ydb.proto.ValueProtos;
import tech.ydb.query.QueryClient;
import tech.ydb.query.QuerySession;
import tech.ydb.query.QueryStream;
import tech.ydb.query.QueryTransaction;
import tech.ydb.query.result.QueryInfo;
import tech.ydb.query.result.QueryResultPart;
import tech.ydb.query.settings.ExecuteQuerySettings;
import tech.ydb.query.settings.QueryExecMode;
import tech.ydb.query.settings.QueryResultFormat;
import tech.ydb.query.settings.QueryStatsMode;
import tech.ydb.query.tools.QueryReader;
import tech.ydb.table.SessionRetryContext;
import tech.ydb.table.description.TableDescription;
import tech.ydb.table.impl.SimpleTableClient;
import tech.ydb.table.query.Params;
import tech.ydb.table.result.ResultSetReader;
import tech.ydb.table.rpc.grpc.GrpcTableRpc;
import tech.ydb.table.utils.Hex;
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
    }

    @AfterClass
    public static void dropAll() {
        logger.info("Clean database...");
        String tablePath = ydbTransport.getDatabase() + "/" + TEST_TABLE;
        String table2Path = ydbTransport.getDatabase() + "/" + TEST_DOUBLE_TABLE;

        SimpleTableClient client = SimpleTableClient.newClient(GrpcTableRpc.useTransport(ydbTransport)).build();
        SessionRetryContext retryCtx = SessionRetryContext.create(client).build();
        retryCtx.supplyStatus(session -> session.dropTable(tablePath)).join().isSuccess();
        retryCtx.supplyStatus(session -> session.dropTable(table2Path)).join().isSuccess();
        logger.info("Clean database OK");
    }

    @Test
    public void testSimpleSelect() {
        try (QueryClient client = QueryClient.newClient(ydbTransport).build()) {
            try (QuerySession session = client.createSession(Duration.ofSeconds(5)).join().getValue()) {
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
    }

    @Test
    @Ignore
    public void testSimplePrepare() {
        try (QueryClient client = QueryClient.newClient(ydbTransport).build()) {
            String query = ""
                    + "DECLARE $id AS Int32?;\n"
                    + "UPSERT INTO `" + TEST_TABLE + "` (id) "
                    + "VALUES ($id)";
            try (QuerySession session = client.createSession(Duration.ofSeconds(5)).join().getValue()) {
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
    }

    @Test
    public void testErrors() {
        try (QueryClient client = QueryClient.newClient(ydbTransport).build()) {
            SessionImpl s1 = (SessionImpl)client.createSession(Duration.ofSeconds(5)).join().getValue();
            String id = s1.getId();
            s1.close();

            SessionImpl s2 = (SessionImpl)client.createSession(Duration.ofSeconds(5)).join().getValue();
            Assert.assertEquals(id, s2.getId());
            s2.updateSessionState(Status.of(StatusCode.ABORTED));
            s2.close();

            SessionImpl s3 = (SessionImpl)client.createSession(Duration.ofSeconds(5)).join().getValue();
            Assert.assertEquals(id, s3.getId());
            s3.updateSessionState(Status.of(StatusCode.BAD_SESSION));
            s3.close();

            SessionImpl s4 = (SessionImpl)client.createSession(Duration.ofSeconds(5)).join().getValue();
            Assert.assertNotEquals(id, s4.getId());
            s4.close();
        }
    }

    @Test
    public void testCancelStream() {
        try (QueryClient client = QueryClient.newClient(ydbTransport).build()) {
            QuerySession s1 = client.createSession(Duration.ofSeconds(5)).join().getValue();
            String id = s1.getId();
            s1.close();

            try (QuerySession s2 = client.createSession(Duration.ofSeconds(5)).join().getValue()) {
                Assert.assertEquals(id, s2.getId());
                s2.createQuery("SELECT 2 + 2;", TxMode.SNAPSHOT_RO).execute(this::printQuerySetPart)
                        .join().getStatus().expectSuccess("cannot execute query");
            }

            try (QuerySession s3 = client.createSession(Duration.ofSeconds(5)).join().getValue()) {
                Assert.assertEquals(id, s3.getId());
                final QueryStream query = s3.createQuery("SELECT 2 + 2;", TxMode.SNAPSHOT_RO);
                final CompletableFuture<Void> stop = new CompletableFuture<>();
                CompletableFuture<Result<QueryInfo>> future = query.execute(part -> {
                    stop.join();
                    printQuerySetPart(part);
                });
                query.cancel();
                stop.complete(null);
                Result<QueryInfo> result = future.join();
                Assert.assertEquals(StatusCode.CLIENT_CANCELLED, result.getStatus().getCode());
            }

            try (QuerySession s4 = client.createSession(Duration.ofSeconds(5)).join().getValue()) {
                Assert.assertNotEquals(id, s4.getId());
                id = s4.getId();

                QueryTransaction tx = s4.beginTransaction(TxMode.SERIALIZABLE_RW).join().getValue();
                Assert.assertTrue(tx.isActive());

                final QueryStream query = tx.createQuery("SELECT 2 + 2;");
                final CompletableFuture<Void> stop = new CompletableFuture<>();
                CompletableFuture<Result<QueryInfo>> future = query.execute(part -> {
                    stop.join();
                    printQuerySetPart(part);
                });
                query.cancel();
                stop.complete(null);
                Result<QueryInfo> result = future.join();
                Assert.assertEquals(StatusCode.CLIENT_CANCELLED, result.getStatus().getCode());
                Assert.assertFalse(tx.isActive());
            }

            try (QuerySession s5 = client.createSession(Duration.ofSeconds(5)).join().getValue()) {
                Assert.assertNotEquals(id, s5.getId());
            }
        }
    }

    @Test
    public void testSimpleCRUD() {
        List<Entity> entities = new ArrayList<>();
        entities.add(new Entity(1, "1234567890", BYTES_EMPTY, true));
        entities.add(new Entity(2, "entity 2", BYTES_EMPTY, true));
        entities.add(new Entity(3, "entity 3", BYTES_LEN2, false));
        entities.add(new Entity(3, "duplicate", BYTES_LEN5, true));
        entities.add(new Entity(5, "entity 5", BYTES_LEN2, false));

        try (QueryClient client = QueryClient.newClient(ydbTransport).build()) {
            for (Entity entity: entities) {
                String query = "DECLARE $id AS Int32;"
                        + "DECLARE $name AS Text;"
                        + "DECLARE $payload AS Bytes;"
                        + "DECLARE $is_valid AS Bool;"
                        + "UPSERT INTO `" + TEST_TABLE + "` (id, name, payload, is_valid) "
                        + "VALUES ($id, $name, $payload, $is_valid)";

                Params params = Params.of(
                        "$id", PrimitiveValue.newInt32(entity.id),
                        "$name", PrimitiveValue.newText(entity.name),
                        "$payload", PrimitiveValue.newBytes(entity.payload),
                        "$is_valid", PrimitiveValue.newBool(entity.isValid)
                );

                try (QuerySession session = client.createSession(SESSION_TIMEOUT).join().getValue()) {
                    session.createQuery(query, TxMode.SERIALIZABLE_RW, params)
                            .execute(this::printQuerySetPart)
                            .join().getStatus().expectSuccess();
                }
            }

            try (QuerySession session = client.createSession(Duration.ofSeconds(5)).join().getValue()) {
                String query = "SELECT is_valid, name FROM " + TEST_TABLE + " ORDER BY id;";
                session.createQuery(query, TxMode.SERIALIZABLE_RW)
                        .execute(this::printQuerySetPart)
                        .join().getStatus().expectSuccess();
            }

            // Apache arrow format
            try (QuerySession session = client.createSession(Duration.ofSeconds(5)).join().getValue()) {
                String query = "SELECT id, name, payload, is_valid FROM " + TEST_TABLE + " ORDER BY id;";
                ExecuteQuerySettings settings = ExecuteQuerySettings.newBuilder()
                        .withResultFormat(QueryResultFormat.APACHE_ARROW)
                        .build();
                session.createQuery(query, TxMode.SNAPSHOT_RO, Params.empty(), settings)
                        .execute(new QueryStream.PartsHandler() {
                            @Override
                            public void onNextPart(QueryResultPart part) {
                                throw new AssertionError("Must be used apache arrow format");
                            }

                            @Override
                            public void onNextPartRaw(long index, ValueProtos.ResultSet rs) {
                                Assert.assertTrue("Must be used apache arrow format", rs.hasArrowBatchSettings());
                                BufferAllocator allocator = new RootAllocator();
                                readApacheArrowBatch(rs.getArrowBatchSettings().getSchema(), rs.getData(), allocator);
                            }
                        })
                        .join().getStatus().expectSuccess();
            }

            try (QuerySession session = client.createSession(Duration.ofSeconds(5)).join().getValue()) {
                String query = "SELECT id, is_valid, name, payload  FROM " + TEST_TABLE + " ORDER BY id;";
                ExecuteQuerySettings settings = ExecuteQuerySettings.newBuilder()
                        .withResultFormat(QueryResultFormat.APACHE_ARROW)
                        .build();
                session.createQuery(query, TxMode.SNAPSHOT_RO, Params.empty(), settings)
                        .execute(new QueryStream.PartsHandler() {
                            @Override
                            public void onNextPart(QueryResultPart part) {
                                throw new AssertionError("Must be used apache arrow format");
                            }

                            @Override
                            public void onNextPartRaw(long index, ValueProtos.ResultSet rs) {
                                Assert.assertTrue("Must be used apache arrow format", rs.hasArrowBatchSettings());
                                BufferAllocator allocator = new RootAllocator();
                                readApacheArrowBatch(rs.getArrowBatchSettings().getSchema(), rs.getData(), allocator);
                            }
                        })
                        .join().getStatus().expectSuccess();
            }

            try (QuerySession session = client.createSession(SESSION_TIMEOUT).join().getValue()) {
                session.createQuery("DELETE FROM " + TEST_TABLE, TxMode.SERIALIZABLE_RW)
                        .execute(this::printQuerySetPart)
                        .join().getStatus().expectSuccess();
            }
        }
    }

    private Schema readApacheArrowSchema(ByteString bytes) {
        try (InputStream is = bytes.newInput()) {
            try (ReadChannel channel = new ReadChannel(Channels.newChannel(is))) {
              return MessageSerializer.deserializeSchema(channel);
            }
        } catch (IOException ex) {
          throw new RuntimeException(ex);
        }
    }

    private void readApacheArrowBatch(ByteString schemaBytes, ByteString batchBytes, BufferAllocator bufferAllocator) {
        logger.info("ARROW SCHEMA = {}", Hex.toHex(schemaBytes));
        logger.info("ARROW DATA = {}", Hex.toHex(batchBytes));
        Schema schema = readApacheArrowSchema(schemaBytes);
        VectorSchemaRoot vector = VectorSchemaRoot.create(schema, bufferAllocator);
        VectorLoader loader = new VectorLoader(vector);

        try (InputStream is = batchBytes.newInput()) {
            try (ReadChannel channel = new ReadChannel(Channels.newChannel(is))) {
                ArrowRecordBatch batch = MessageSerializer.deserializeRecordBatch(channel, bufferAllocator);
                loader.load(batch);

                logger.info("LOAD {}", vector.getRowCount());
                for (Field field: schema.getFields()) {
                    FieldVector fv = vector.getVector(field);
                    logger.info("{} -> {} ", field.getName(), field.getFieldType().getType());
                    for (int idx = 0; idx < vector.getRowCount(); idx++) {
                        Object obj = fv.getObject(idx);
                        Object s = (obj instanceof byte[]) ? Arrays.toString((byte[]) obj) : obj;
                        logger.info("{}[{}]={}[{}] ", field.getName(), idx, s, obj.getClass());
                    }
                }

                batch.close();
            }
        } catch (IOException ex) {
          throw new RuntimeException(ex);
        }
    }

    public void printQuerySetPart(QueryResultPart part) {
        ResultSetReader rs = part.getResultSetReader();
        if (rs != null) {
            logger.info("got query result part with index {} and {} rows", part.getResultSetIndex(), rs.getRowCount());
            while (rs.next()) {
                for (int c = 0; c < rs.getColumnCount(); c++) {
                    System.out.println("read " + rs.getColumnName(c) + " = " + rs.getColumn(c).toString());
                }
            }
        }
    }

    @Test
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
    public void concurrentResultSetsTest() {
        try (QueryClient client = QueryClient.newClient(ydbTransport).build()) {
            try (QuerySession session = client.createSession(Duration.ofSeconds(5)).join().getValue()) {
                String query = ""
                        + "SELECT id, name, payload, is_valid FROM " + TEST_TABLE + " ORDER BY id;\n"
                        + "SELECT 3 + 5;\n"
                        + "SELECT id, name, payload, is_valid FROM " + TEST_TABLE + " ORDER BY id DESC;\n"
                        + "SELECT 3 + 1;\n";

                // consistent read - all result sets are ordered
                QueryStream qs1 = session.createQuery(query, TxMode.SNAPSHOT_RO, Params.empty(),
                        ExecuteQuerySettings.newBuilder().withConcurrentResultSets(false).build());

                Deque<Long> ordered = new ArrayDeque<>();
                Result<QueryInfo> res1 = qs1.execute(part -> {
                    Long id = part.getResultSetIndex();
                    if (!ordered.isEmpty()) {
                        Assert.assertTrue(id >= ordered.getLast());
                    }
                    ordered.addLast(id);
                }).join();
                Assert.assertTrue(res1.isSuccess());

                // concurrent read - all result sets are unordered
                QueryStream qs2 = session.createQuery(query, TxMode.SNAPSHOT_RO, Params.empty(),
                        ExecuteQuerySettings.newBuilder().withConcurrentResultSets(true).build());

                Set<Long> unordered = new HashSet<>();
                Result<QueryInfo> res2 = qs2.execute(part -> {
                    unordered.add(part.getResultSetIndex());
                }).join();
                Assert.assertTrue(res2.isSuccess());

                Assert.assertTrue(ordered.containsAll(unordered));
                Assert.assertTrue(unordered.containsAll(ordered));
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
    public void testQueryWarnings() {
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

    @Test
    public void testMultiStatement() {
        try (QueryClient client = QueryClient.newClient(ydbTransport).build()) {
            try (QuerySession session = client.createSession(Duration.ofSeconds(5)).join().getValue()) {
                String query = ""
                        + "DECLARE $s1 AS Int32;"
                        + "DECLARE $s2 AS Int32;"
                        + "DECLARE $id1 AS Int32;"
                        + "DECLARE $id2 AS Int32;"
                        + "DECLARE $name1 AS Text;"
                        + "DECLARE $name2 AS Text;"
                        + "SELECT * FROM `" + TEST_TABLE + "` WHERE id = $s1;"
                        + "INSERT INTO `" + TEST_TABLE + "` (id, name) VALUES ($id1, $name1);"
                        + "SELECT * FROM `" + TEST_TABLE + "` WHERE id = $s2;"
                        + "INSERT INTO `" + TEST_TABLE + "` (id, name) VALUES ($id2, $name2);"
                        + "SELECT * FROM `" + TEST_TABLE + "` ORDER BY id";

                Params params = Params.of(
                        "$s1", PrimitiveValue.newInt32(100),
                        "$s2", PrimitiveValue.newInt32(100),
                        "$id1", PrimitiveValue.newInt32(100),
                        "$name1", PrimitiveValue.newText("TEST1"),
                        "$id2", PrimitiveValue.newInt32(200),
                        "$name2", PrimitiveValue.newText("TEST2")
                );

                Result<QueryReader> result = QueryReader.readFrom(
                        session.createQuery(query, TxMode.SERIALIZABLE_RW, params)
                ).join();

                Assert.assertTrue(result.isSuccess());
                Assert.assertTrue(result.getStatus().getIssues().length == 0);

                QueryReader reader = result.getValue();
                Assert.assertEquals(3, reader.getResultSetCount());

                ResultSetReader rs1 = reader.getResultSet(0);
                Assert.assertFalse(rs1.next());

                ResultSetReader rs2 = reader.getResultSet(1);
                Assert.assertTrue(rs2.next());
                Assert.assertEquals(100, rs2.getColumn("id").getInt32());
                Assert.assertEquals("TEST1", rs2.getColumn("name").getText());
                Assert.assertFalse(rs2.next());

                ResultSetReader rs3 = reader.getResultSet(2);
                Assert.assertTrue(rs3.next());
                Assert.assertEquals(100, rs3.getColumn("id").getInt32());
                Assert.assertEquals("TEST1", rs3.getColumn("name").getText());
                Assert.assertTrue(rs3.next());
                Assert.assertEquals(200, rs3.getColumn("id").getInt32());
                Assert.assertEquals("TEST2", rs3.getColumn("name").getText());
                Assert.assertFalse(rs3.next());
            } finally {
                try (QuerySession session = client.createSession(SESSION_TIMEOUT).join().getValue()) {
                    session.createQuery("DELETE FROM " + TEST_TABLE, TxMode.SERIALIZABLE_RW).execute()
                            .join().getStatus().expectSuccess();
                }
            }
        }
    }

    @Test
    public void testNoTxStatement() {
        try (QueryClient client = QueryClient.newClient(ydbTransport).build()) {
            try (QuerySession session = client.createSession(Duration.ofSeconds(5)).join().getValue()) {
                String query = ""
                        + "DECLARE $s1 AS Int32;"
                        + "DECLARE $s2 AS Int32;"
                        + "DECLARE $id1 AS Int32;"
                        + "DECLARE $id2 AS Int32;"
                        + "DECLARE $name1 AS Text;"
                        + "DECLARE $name2 AS Text;"
                        + "SELECT * FROM `" + TEST_TABLE + "` WHERE id = $s1;"
                        + "INSERT INTO `" + TEST_TABLE + "` (id, name) VALUES ($id1, $name1);"
                        + "SELECT * FROM `" + TEST_TABLE + "` WHERE id = $s2;"
                        + "INSERT INTO `" + TEST_TABLE + "` (id, name) VALUES ($id2, $name2);"
                        + "SELECT * FROM `" + TEST_TABLE + "` ORDER BY id";

                Params params = Params.of(
                        "$s1", PrimitiveValue.newInt32(100),
                        "$s2", PrimitiveValue.newInt32(100),
                        "$id1", PrimitiveValue.newInt32(100),
                        "$name1", PrimitiveValue.newText("TEST1"),
                        "$id2", PrimitiveValue.newInt32(100),
                        "$name2", PrimitiveValue.newText("TEST2")
                );

                Result<QueryReader> result = QueryReader.readFrom(
                        session.createQuery(query, TxMode.NONE, params)
                ).join();

                Assert.assertFalse(result.isSuccess());
                Assert.assertEquals(StatusCode.PRECONDITION_FAILED, result.getStatus().getCode());
                Issue issue =  Issue.of(2012,
                        "Constraint violated. Table: `" + ydbTransport.getDatabase() + "/" + TEST_TABLE + "`.",
//                        "Conflict with existing key.",
                        Issue.Severity.ERROR
                );
                Assert.assertArrayEquals(new Issue[] { issue }, result.getStatus().getIssues());

                Iterator<ResultSetReader> rsIter = QueryReader.readFrom(
                        session.createQuery("SELECT id, name FROM " + TEST_TABLE + " ORDER BY id", TxMode.NONE)
                ).join().getValue().iterator();

                Assert.assertTrue(rsIter.hasNext());
                ResultSetReader rs = rsIter.next();
                Assert.assertFalse(rs.next());
            }
        }
    }
}
