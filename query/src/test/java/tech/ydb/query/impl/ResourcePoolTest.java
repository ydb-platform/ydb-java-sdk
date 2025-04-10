package tech.ydb.query.impl;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tech.ydb.common.transaction.TxMode;
import tech.ydb.core.Result;
import tech.ydb.core.Status;
import tech.ydb.core.StatusCode;
import tech.ydb.query.QueryClient;
import tech.ydb.query.result.QueryInfo;
import tech.ydb.query.settings.ExecuteQuerySettings;
import tech.ydb.query.settings.QueryExecMode;
import tech.ydb.query.settings.QueryStatsMode;
import tech.ydb.query.tools.SessionRetryContext;
import tech.ydb.table.query.Params;
import tech.ydb.test.junit4.GrpcTransportRule;

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
public class ResourcePoolTest {
    private final static Logger logger = LoggerFactory.getLogger(ResourcePoolTest.class);

    private final static String TEST_TABLE = "query_resource_pool_service_test";

    @ClassRule
    public final static GrpcTransportRule ydbTransport = new GrpcTransportRule();

    private static QueryClient client;
    private static SessionRetryContext retryCtx;

    @BeforeClass
    public static void initSchema() {
        client = QueryClient.newClient(ydbTransport).build();
        retryCtx = SessionRetryContext.create(client).retryNotFound(false).build();

        logger.info("Prepare database...");
        String createTable = "CREATE TABLE " + TEST_TABLE + "(id Int32 NOT NULL, name Text, PRIMARY KEY(id));";
        Result<QueryInfo> ct = retryCtx.supplyResult(s -> s.createQuery(createTable, TxMode.NONE).execute()).join();
        Assert.assertTrue("Cannot create test table " + ct.getStatus(), ct.isSuccess());
        logger.info("Prepare database OK");
    }

    @AfterClass
    public static void dropAll() {
        if (retryCtx != null) {
            logger.info("Clean database...");
            String dropTable = "DROP TABLE " + TEST_TABLE + ";";
            Result<QueryInfo> dt = retryCtx.supplyResult(s -> s.createQuery(dropTable, TxMode.NONE).execute()).join();
            logger.info("Clean database " + dt.getStatus());
            retryCtx = null;
        }

        if (client != null) {
            client.close();
            client = null;
        }
    }

    private Status selectWithPool(String poolName) {
        ExecuteQuerySettings settings = ExecuteQuerySettings.newBuilder()
                .withRequestTimeout(Duration.ofSeconds(5))
                .withExecMode(QueryExecMode.EXECUTE)
                .withResourcePool(poolName)
                .withStatsMode(QueryStatsMode.FULL)
                .build();

        String select = "SELECT id, name FROM " + TEST_TABLE + " ORDER BY id;";
        return retryCtx
                .supplyResult(s -> s.createQuery(select, TxMode.NONE, Params.empty(), settings).execute())
                .join()
                .getStatus();
    }

    private Status createResourcePool(String poolName) {
        String createPool = "CREATE RESOURCE POOL " + poolName + " WITH ("
                + "CONCURRENT_QUERY_LIMIT=10,"
                + "QUEUE_SIZE=\"-1\"," // Query size works unstable
                + "DATABASE_LOAD_CPU_THRESHOLD=80);";
        return retryCtx.supplyResult(s -> s.createQuery(createPool, TxMode.NONE).execute()).join().getStatus();
    }

    private Status dropResourcePool(String poolName) {
        String dropPool = "DROP RESOURCE POOL " + poolName + ";";
        return retryCtx.supplyResult(s -> s.createQuery(dropPool, TxMode.NONE).execute()).join().getStatus();
    }

    @Test
    public void useResourcePoolTest() {
        Status create = createResourcePool("test_pool1");
        Assert.assertTrue("Cannot create resource pool " + create, create.isSuccess());

        Status select = selectWithPool("test_pool1");
        Assert.assertTrue("Query shouldn't fail " + select, select.isSuccess());

        Status drop = dropResourcePool("test_pool1");
        Assert.assertTrue("Cannot drop resource pool " + drop, drop.isSuccess());
    }

    @Test
    public void caseSensitiveTest() {
        Status create = createResourcePool("test_pool2");
        Assert.assertTrue("Cannot create resource pool " + create, create.isSuccess());

        Status select = selectWithPool("test_Pool2");
        Assert.assertFalse("Query should fail " + select, select.isSuccess());
        Assert.assertEquals(StatusCode.NOT_FOUND, select.getCode());

        Status drop = dropResourcePool("test_pool2");
        Assert.assertTrue("Cannot drop resource pool " + drop, drop.isSuccess());
    }

    @Test
    public void defaultResourcePoolTest() {
        Status select1 = selectWithPool("default");
        Assert.assertTrue("Query shouldn't fail " + select1, select1.isSuccess());

        Status select2 = selectWithPool("");
        Assert.assertTrue("Query shouldn't fail " + select2, select2.isSuccess());
    }

    @Test
    public void unknownPoolErrorTest() {
        Status select = selectWithPool("uknown_pool");
        Assert.assertFalse("Query should fail " + select, select.isSuccess());
        Assert.assertEquals(StatusCode.NOT_FOUND, select.getCode());
    }

    /**
     * Check that we don't cache resource pool in session
     */
    @Test
    public void useDifferentPoolsTest() {
        Status create3 = createResourcePool("test_pool3");
        Assert.assertTrue("Cannot create resource pool " + create3, create3.isSuccess());
        Status create4 = createResourcePool("test_pool4");
        Assert.assertTrue("Cannot create resource pool " + create4, create4.isSuccess());

        ExecuteQuerySettings pool3 = ExecuteQuerySettings.newBuilder()
                .withRequestTimeout(Duration.ofSeconds(5))
                .withResourcePool("test_pool3")
                .build();
        ExecuteQuerySettings pool4 = ExecuteQuerySettings.newBuilder()
                .withRequestTimeout(Duration.ofSeconds(5))
                .withResourcePool("test_pool4")
                .build();

        String query = "SELECT id, name FROM " + TEST_TABLE + " ORDER BY id;";

        retryCtx.supplyStatus(session -> {
            Status p3 = session.createQuery(query, TxMode.NONE, Params.empty(), pool3).execute().join().getStatus();
            Assert.assertTrue("Query shouldn't fail " + p3, p3.isSuccess());

            Status p4 = session.createQuery(query, TxMode.NONE, Params.empty(), pool4).execute().join().getStatus();
            Assert.assertTrue("Query shouldn't fail " + p4, p4.isSuccess());

            Status none = session.createQuery(query, TxMode.NONE).execute().join().getStatus();
            Assert.assertTrue("Query shouldn't fail " + none, none.isSuccess());

            return CompletableFuture.completedFuture(Status.SUCCESS);
        }).join();

        Status drop4 = dropResourcePool("test_pool4");
        Assert.assertTrue("Cannot drop resource pool " + drop4, drop4.isSuccess());
        Status drop3 = dropResourcePool("test_pool3");
        Assert.assertTrue("Cannot drop resource pool " + drop3, drop3.isSuccess());
    }
}
