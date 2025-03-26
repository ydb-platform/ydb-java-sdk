package tech.ydb.query.impl;

import org.junit.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tech.ydb.common.transaction.TxMode;
import tech.ydb.core.Issue;
import tech.ydb.core.Result;
import tech.ydb.core.Status;
import tech.ydb.core.StatusCode;
import tech.ydb.query.QueryClient;
import tech.ydb.query.QuerySession;
import tech.ydb.query.QueryStream;
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

import java.time.Duration;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Test on resource poll.
 * Take an account, that resource poll with name "default" exists every time and can't be deleted
 * Also when we specify pool with empty string "" it's equivalent to default pool
 *
 * @author Evgeny Kuvardin
 */
public class QueryIntegrationResourcePoolTest {
    private final static Logger logger = LoggerFactory.getLogger(QueryIntegrationResourcePoolTest.class);
    private final static String TEST_TABLE = "query_service_test";
    private final static String TEST_DOUBLE_TABLE = "query_double_table";
    private final static String TEST_RESOURCE_POOL = "test_pool";


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

        try (QueryClient queryClient = QueryClient.newClient(ydbTransport).build()) {
            try (QuerySession querySession = queryClient.createSession(Duration.ofSeconds(5)).join().getValue()) {
                Result<QueryInfo> result = querySession.createQuery("CREATE RESOURCE POOL " + TEST_RESOURCE_POOL + " WITH (\n" +
                        "    CONCURRENT_QUERY_LIMIT=10,\n" +
                        "    QUEUE_SIZE=1000,\n" +
                        "    DATABASE_LOAD_CPU_THRESHOLD=80,\n" +
                        "    TOTAL_CPU_LIMIT_PERCENT_PER_NODE=70);", TxMode.NONE).execute().join();

                Assert.assertTrue(result.getValue().toString(), result.isSuccess());
            }
        }
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

        try (QueryClient queryClient = QueryClient.newClient(ydbTransport).build()) {
            try (QuerySession querySession = queryClient.createSession(Duration.ofSeconds(5)).join().getValue()) {
                Result<QueryInfo> result = querySession.createQuery("DROP RESOURCE POOL " + TEST_RESOURCE_POOL + ";", TxMode.NONE).execute().join();

                Assert.assertTrue(result.getValue().toString(), result.isSuccess());
            }
        }

        logger.info("Clean database OK");
    }

    @Test
    public void selectWithResourcePoolTest() {
        try (QueryClient client = QueryClient.newClient(ydbTransport).build()) {
            try (QuerySession session = client.createSession(Duration.ofSeconds(5)).join().getValue()) {
                ExecuteQuerySettings settings = ExecuteQuerySettings.newBuilder()
                        .withExecMode(QueryExecMode.EXECUTE)
                        .withResourcePool("test_pool")
                        .build();

                Assert.assertTrue("Query shouldn't fall",
                        session.createQuery("SELECT 2 + 3;", TxMode.SERIALIZABLE_RW, Params.empty(), settings).execute()
                                .join().isSuccess());
            }
        }
    }

    @Test
    public void selectWithDefaultResourcePoolTest() {
        try (QueryClient client = QueryClient.newClient(ydbTransport).build()) {
            try (QuerySession session = client.createSession(Duration.ofSeconds(5)).join().getValue()) {
                ExecuteQuerySettings settings = ExecuteQuerySettings.newBuilder()
                        .withExecMode(QueryExecMode.EXECUTE)
                        .withResourcePool("default")
                        .build();

                Assert.assertTrue("Query shouldn't fall",
                        session.createQuery("SELECT 2 + 3;", TxMode.SERIALIZABLE_RW, Params.empty(), settings).execute()
                                .join().isSuccess());
            }
        }
    }

    @Test
    public void selectWithDefaultResourcePoolAndEmptyStringTest() {
        try (QueryClient client = QueryClient.newClient(ydbTransport).build()) {
            try (QuerySession session = client.createSession(Duration.ofSeconds(5)).join().getValue()) {
                ExecuteQuerySettings settings = ExecuteQuerySettings.newBuilder()
                        .withExecMode(QueryExecMode.EXECUTE)
                        .withResourcePool("")
                        .build();

                Assert.assertTrue("Query shouldn't fall",
                        session.createQuery("SELECT 2 + 3;", TxMode.SERIALIZABLE_RW, Params.empty(), settings).execute()
                                .join().isSuccess());
            }
        }
    }

    @Test
    public void selectShouldFailWithUnknownResourcePollTest() {
        try (QueryClient client = QueryClient.newClient(ydbTransport).build()) {
            try (QuerySession session = client.createSession(Duration.ofSeconds(5)).join().getValue()) {
                ExecuteQuerySettings settings = ExecuteQuerySettings.newBuilder()
                        .withExecMode(QueryExecMode.EXECUTE)
                        .withResourcePool("some_unknown_pool")
                        .build();

                Assert.assertFalse("Query should fall",
                        session.createQuery("SELECT 2 + 3;", TxMode.SERIALIZABLE_RW, Params.empty(), settings).execute()
                                .join().isSuccess());
            }
        }
    }
}
