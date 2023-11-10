package tech.ydb.query;

import java.time.Duration;
import java.util.ArrayList;
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

import tech.ydb.core.Status;
import tech.ydb.query.result.QueryResultPart;
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

        SimpleTableClient client = SimpleTableClient.newClient(GrpcTableRpc.useTransport(ydbTransport)).build();
        SessionRetryContext retryCtx = SessionRetryContext.create(client).build();
        retryCtx.supplyStatus(session -> session.createTable(tablePath, tableDescription));
        logger.info("Prepare database OK");

        queryClient = QueryClient.newClient(ydbTransport).build();
    }

    @AfterClass
    public static void dropAll() {
        try {
            logger.info("Clean database...");
            String tablePath = ydbTransport.getDatabase() + "/" + TEST_TABLE;

            SimpleTableClient client = SimpleTableClient.newClient(GrpcTableRpc.useTransport(ydbTransport)).build();
            SessionRetryContext retryCtx = SessionRetryContext.create(client).build();
            retryCtx.supplyStatus(session -> session.dropTable(tablePath)).join().isSuccess();
            logger.info("Clean database OK");
        } finally {
            queryClient.close();
        }
    }

    @Test
    public void testSimpleSelect() {
        try (QuerySession session = queryClient.createSession(Duration.ofSeconds(5)).join().getValue()) {
            session.executeQuery("SELECT 2 + 3;", TxMode.serializableRw()).start(part -> {
                ResultSetReader rs = part.getResultSetReader();

                Assert.assertTrue(rs.next());
                Assert.assertEquals(1, rs.getColumnCount());
                Assert.assertEquals("column0", rs.getColumnName(0));
                Assert.assertEquals(5, rs.getColumn(0).getInt32());

                Assert.assertFalse(rs.next());
            }).join().expectSuccess();
        }
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
                session.executeQuery(query, TxMode.serializableRw(), params)
                        .start(this::printQuerySetPart)
                        .join().expectSuccess();
            }
        }

        try (QuerySession session = queryClient.createSession(Duration.ofSeconds(5)).join().getValue()) {
            String query = "SELECT id, name, payload, is_valid FROM " + TEST_TABLE + " ORDER BY id;";
            session.executeQuery(query, TxMode.serializableRw())
                    .start(this::printQuerySetPart)
                    .join().expectSuccess();
        }

        try (QuerySession session = queryClient.createSession(SESSION_TIMEOUT).join().getValue()) {
            session.executeQuery("DELETE FROM " + TEST_TABLE, TxMode.serializableRw())
                    .start(this::printQuerySetPart)
                    .join().expectSuccess();
        }
    }

    public void printQuerySetPart(QueryResultPart part) {
        ResultSetReader rs = part.getResultSetReader();
        logger.info("got query result part with index {} and {} rows", part.getResultSetIndex(), rs.getRowCount());
    }

    @Test
    @Ignore
    public void testSchemeQuery() {
        try (QueryClient client = QueryClient.newClient(ydbTransport).build()) {
            try (QuerySession session = client.createSession(Duration.ofSeconds(5)).join().getValue()) {
                TxMode tx = TxMode.serializableRw();

                CompletableFuture<Status> createTable = session
                        .executeQuery("CREATE TABLE demo_table (id Int32, data Text, PRIMARY KEY(id));", tx)
                        .start(this::printQuerySetPart);
                createTable.join().expectSuccess();


                CompletableFuture<Status> dropTable = session
                        .executeQuery("DROP TABLE demo_table;", tx)
                        .start(this::printQuerySetPart);
                dropTable.join().expectSuccess();
            }
        }
    }
}
