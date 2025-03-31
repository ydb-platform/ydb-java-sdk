package tech.ydb.query.impl;

import org.junit.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tech.ydb.common.transaction.TxMode;
import tech.ydb.core.Result;
import tech.ydb.query.QueryClient;
import tech.ydb.query.QuerySession;
import tech.ydb.query.result.QueryInfo;
import tech.ydb.query.settings.ExecuteQuerySettings;
import tech.ydb.query.settings.QueryExecMode;
import tech.ydb.query.settings.QueryStatsMode;
import tech.ydb.table.SessionRetryContext;
import tech.ydb.table.description.TableDescription;
import tech.ydb.table.impl.SimpleTableClient;
import tech.ydb.table.query.Params;
import tech.ydb.table.rpc.grpc.GrpcTableRpc;
import tech.ydb.table.values.PrimitiveType;
import tech.ydb.test.junit4.GrpcTransportRule;

import java.time.Duration;

/**
 * Test on resource poll.
 * <p>
 * Take an account, that resource poll with name "default" exists every time and can't be deleted
 * Also when we specify pool with empty string "" it's equivalent to default pool
 * <p>
 * Test marked with @Ignore should be uncommented when resource pool disappeared from experimental feature
 * <p>
 * Until this to run test go to @see tech.ydb.test.integration.YdbEnvironment
 * {@link tech.ydb.test.integration.YdbEnvironment}
 * dockerFeatures = createParam("YDB_DOCKER_FEATURE_FLAGS", "enable_resource_pools");
 * By the way feature available with image ydbplatform/local-ydb:24.3.11.13";
 *
 * @author Evgeny Kuvardin
 */
public class QueryIntegrationResourcePoolTest {
    private final static Logger logger = LoggerFactory.getLogger(QueryIntegrationResourcePoolTest.class);
    private final static String TEST_TABLE = "query_resource_pool_service_test";
    private final static String TEST_RESOURCE_POOL = "test_pool";
    private final static String TEST_RESOURCE_POOL_WITH_DELETE = "test_pool_fot_delete";


    @ClassRule
    public final static GrpcTransportRule ydbTransport = new GrpcTransportRule();

    @BeforeClass
    public static void initSchema() {
        logger.info("Prepare database...");

        String tablePath = ydbTransport.getDatabase() + "/" + TEST_TABLE;
        TableDescription tableDescription = TableDescription.newBuilder()
                .addNonnullColumn("id", PrimitiveType.Int32)
                .addNullableColumn("name", PrimitiveType.Text)
                .setPrimaryKey("id")
                .build();

        SimpleTableClient client = SimpleTableClient.newClient(GrpcTableRpc.useTransport(ydbTransport)).build();
        SessionRetryContext retryCtx = SessionRetryContext.create(client).build();
        Assert.assertTrue("Table should be created before tests",
                retryCtx.supplyStatus(session -> session.createTable(tablePath, tableDescription)).join().isSuccess());
        logger.info("Prepare database OK");

    }

    @AfterClass
    public static void dropAll() {
        logger.info("Clean database...");
        String tablePath = ydbTransport.getDatabase() + "/" + TEST_TABLE;

        SimpleTableClient client = SimpleTableClient.newClient(GrpcTableRpc.useTransport(ydbTransport)).build();
        SessionRetryContext retryCtx = SessionRetryContext.create(client).build();
        retryCtx.supplyStatus(session -> session.dropTable(tablePath)).join();

        logger.info("Clean database OK");
    }

    @Ignore
    @Test
    public void selectWithResourcePoolTest() {
        createResourcePool(TEST_RESOURCE_POOL);
        try (QueryClient client = QueryClient.newClient(ydbTransport).build()) {
            try (QuerySession session = client.createSession(Duration.ofSeconds(5)).join().getValue()) {
                ExecuteQuerySettings settings = ExecuteQuerySettings.newBuilder()
                        .withExecMode(QueryExecMode.EXECUTE)
                        .withResourcePool(TEST_RESOURCE_POOL)
                        .withStatsMode(QueryStatsMode.FULL)
                        .build();

                Assert.assertTrue("Query shouldn't fail",
                        session.createQuery("SELECT id, name FROM " + TEST_TABLE + " ORDER BY id;", TxMode.SERIALIZABLE_RW, Params.empty(), settings).execute()
                                .join().isSuccess());
            }
        } finally {
            deleteResourcePool(TEST_RESOURCE_POOL, true);
        }
    }

    @Ignore
    @Test
    public void selectWithResourcePoolShouldBeCaseSensitiveTest() {
        createResourcePool(TEST_RESOURCE_POOL);
        try (QueryClient client = QueryClient.newClient(ydbTransport).build()) {
            try (QuerySession session = client.createSession(Duration.ofSeconds(5)).join().getValue()) {
                ExecuteQuerySettings settings = ExecuteQuerySettings.newBuilder()
                        .withExecMode(QueryExecMode.EXECUTE)
                        .withResourcePool(TEST_RESOURCE_POOL.toUpperCase())
                        .withStatsMode(QueryStatsMode.FULL)
                        .build();

                Assert.assertFalse("Query should fail",
                        session.createQuery("SELECT id, name FROM " + TEST_TABLE + " ORDER BY id;", TxMode.SERIALIZABLE_RW, Params.empty(), settings).execute()
                                .join().isSuccess());
            }
        } finally {
            deleteResourcePool(TEST_RESOURCE_POOL, true);
        }
    }

    /**
     * Check that we don't cache resource pool in session
     */
    @Ignore
    @Test
    public void selectWithResourcePoolShouldNotCachePoolInSessionTest() {
        createResourcePool(TEST_RESOURCE_POOL_WITH_DELETE);

        try (QueryClient client = QueryClient.newClient(ydbTransport).build()) {
            try (QuerySession session = client.createSession(Duration.ofSeconds(5)).join().getValue()) {
                ExecuteQuerySettings settings = ExecuteQuerySettings.newBuilder()
                        .withExecMode(QueryExecMode.EXECUTE)
                        .withResourcePool(TEST_RESOURCE_POOL_WITH_DELETE)
                        .withStatsMode(QueryStatsMode.FULL)
                        .build();

                Assert.assertTrue("Query shouldn't fail",
                        session.createQuery("SELECT id, name FROM " + TEST_TABLE + " ORDER BY id;", TxMode.SERIALIZABLE_RW, Params.empty(), settings).execute()
                                .join().isSuccess());

                deleteResourcePool(TEST_RESOURCE_POOL_WITH_DELETE, true);

                Assert.assertTrue("Query shouldn't cache in session previous call to resource pool",
                        session.createQuery("SELECT id, name FROM " + TEST_TABLE + " ORDER BY id;", TxMode.SERIALIZABLE_RW, Params.empty()).execute()
                                .join().isSuccess());
            }
        } finally {
            deleteResourcePool(TEST_RESOURCE_POOL_WITH_DELETE, false);
        }
    }


    @Test
    public void selectWithDefaultResourcePoolTest() {
        try (QueryClient client = QueryClient.newClient(ydbTransport).build()) {
            try (QuerySession session = client.createSession(Duration.ofSeconds(5)).join().getValue()) {
                ExecuteQuerySettings settings = ExecuteQuerySettings.newBuilder()
                        .withExecMode(QueryExecMode.EXECUTE)
                        .withResourcePool("default")
                        .withStatsMode(QueryStatsMode.FULL)
                        .build();

                Assert.assertTrue("Query shouldn't fail with default pool name",
                        session.createQuery("SELECT id, name FROM " + TEST_TABLE + " ORDER BY id;", TxMode.SERIALIZABLE_RW, Params.empty(), settings).execute()
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

                Assert.assertTrue("Query shouldn't fail cause empty string equivalent to default pool.",
                        session.createQuery("SELECT id, name FROM " + TEST_TABLE + " ORDER BY id;", TxMode.SERIALIZABLE_RW, Params.empty(), settings).execute()
                                .join().isSuccess());
            }
        }
    }

    @Ignore
    @Test
    public void selectShouldFailWithUnknownResourcePollTest() {
        try (QueryClient client = QueryClient.newClient(ydbTransport).build()) {
            try (QuerySession session = client.createSession(Duration.ofSeconds(5)).join().getValue()) {
                ExecuteQuerySettings settings = ExecuteQuerySettings.newBuilder()
                        .withExecMode(QueryExecMode.EXECUTE)
                        .withResourcePool("some_unknown_pool")
                        .withStatsMode(QueryStatsMode.FULL)
                        .build();

                Assert.assertFalse("Query should fail cause poll not exists",
                        session.createQuery("SELECT id, name FROM " + TEST_TABLE + " ORDER BY id;", TxMode.SERIALIZABLE_RW, Params.empty(), settings).execute()
                                .join().isSuccess());
            }
        }
    }

    private static void createResourcePool(String resourcePoolName) {
        try (QueryClient queryClient = QueryClient.newClient(ydbTransport).build()) {
            try (QuerySession querySession = queryClient.createSession(Duration.ofSeconds(5)).join().getValue()) {
                Result<QueryInfo> result = querySession.createQuery("CREATE RESOURCE POOL " + resourcePoolName + " WITH (\n" +
                        "    CONCURRENT_QUERY_LIMIT=10,\n" +
                        "    QUEUE_SIZE=1000,\n" +
                        "    DATABASE_LOAD_CPU_THRESHOLD=80);", TxMode.NONE).execute().join();

                Assert.assertTrue(result.getStatus().toString(), result.isSuccess());
            }
        }
    }

    private static void deleteResourcePool(String resourcePoolName, boolean checkError) {
        try (QueryClient queryClient = QueryClient.newClient(ydbTransport).build()) {
            try (QuerySession querySession = queryClient.createSession(Duration.ofSeconds(5)).join().getValue()) {
                Result<QueryInfo> result = querySession.createQuery("DROP RESOURCE POOL " + resourcePoolName + ";", TxMode.NONE).execute().join();

                if (checkError) {
                    Assert.assertTrue(result.getStatus().toString(), result.isSuccess());
                }
            }
        }
    }
}
