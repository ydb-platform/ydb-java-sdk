package tech.ydb.query.impl;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import tech.ydb.common.transaction.TxMode;
import tech.ydb.core.Result;
import tech.ydb.core.Status;
import tech.ydb.proto.ValueProtos;
import tech.ydb.query.QueryClient;
import tech.ydb.query.TestExampleData;
import tech.ydb.query.script.ScriptClient;
import tech.ydb.query.script.result.OperationScript;
import tech.ydb.query.script.impl.ScriptClientImpl;
import tech.ydb.query.script.result.FetchScriptResult;
import tech.ydb.query.script.settings.ExecuteScriptSettings;
import tech.ydb.query.script.settings.FetchScriptSettings;
import tech.ydb.query.script.settings.FindScriptSettings;
import tech.ydb.query.settings.QueryExecMode;
import tech.ydb.query.tools.QueryReader;
import tech.ydb.query.tools.SessionRetryContext;
import tech.ydb.table.query.Params;
import tech.ydb.table.result.ResultSetReader;
import tech.ydb.table.result.impl.ProtoValueReaders;
import tech.ydb.table.values.ListType;
import tech.ydb.table.values.ListValue;
import tech.ydb.table.values.PrimitiveType;
import tech.ydb.table.values.PrimitiveValue;
import tech.ydb.table.values.StructType;
import tech.ydb.test.junit4.GrpcTransportRule;


/**
 * Integration tests that validate the execution of YQL scripts
 * using the YDB Query API and scripting features.
 *
 * <p>Tests cover:
 * <ul>
 *   <li>Script execution with and without parameters</li>
 *   <li>Error handling in scripts</li>
 *   <li>Sequential script execution</li>
 *   <li>Fetching results from executed scripts</li>
 * </ul>
 *
 * <p>Author: Evgeny Kuvardin
 */
public class ScriptExampleTest {

    @ClassRule
    public final static GrpcTransportRule ydbRule = new GrpcTransportRule();

    private static QueryClient client;
    private static SessionRetryContext retryCtx;
    private static ScriptClient scriptClient;

    // Create type for struct of series
    StructType seriesType = StructType.of(
            "series_id", PrimitiveType.Uint64,
            "title", PrimitiveType.Text,
            "release_date", PrimitiveType.Date,
            "series_info", PrimitiveType.Text
    );
    // Create and fill list of series
    ListValue seriesData = ListType.of(seriesType).newValue(
            TestExampleData.SERIES.stream().map(series -> seriesType.newValue(
                    "series_id", PrimitiveValue.newUint64(series.seriesID()),
                    "title", PrimitiveValue.newText(series.title()),
                    "series_info", PrimitiveValue.newText(series.seriesInfo()),
                    "release_date", PrimitiveValue.newDate(series.releaseDate())
            )).collect(Collectors.toList())
    );

    // Create type for struct of season
    StructType seasonType = StructType.of(
            "series_id", PrimitiveType.Uint64,
            "season_id", PrimitiveType.Uint64,
            "title", PrimitiveType.Text,
            "first_aired", PrimitiveType.Date,
            "last_aired", PrimitiveType.Date
    );
    // Create and fill list of seasons
    ListValue seasonsData = ListType.of(seasonType).newValue(
            TestExampleData.SEASONS.stream().map(season -> seasonType.newValue(
                    "series_id", PrimitiveValue.newUint64(season.seriesID()),
                    "season_id", PrimitiveValue.newUint64(season.seasonID()),
                    "title", PrimitiveValue.newText(season.title()),
                    "first_aired", PrimitiveValue.newDate(season.firstAired()),
                    "last_aired", PrimitiveValue.newDate(season.lastAired())
            )).collect(Collectors.toList())
    );

    @BeforeClass
    public static void init() {
        client = QueryClient.newClient(ydbRule)
                .sessionPoolMaxSize(5)
                .build();
        retryCtx = SessionRetryContext.create(client).build();

        scriptClient = ScriptClientImpl.newClient(ydbRule);

        Assert.assertNotNull(client.getScheduler());
    }

    @After
    public void clean() {
        retryCtx.supplyResult(session -> session.createQuery("DROP TABLE series;", TxMode.NONE).execute())
                .join();
        retryCtx.supplyResult(session -> session.createQuery("DROP TABLE seasons;", TxMode.NONE).execute())
                .join();
    }

    @AfterClass
    public static void cleanAll() {
        client.close();
    }

    @Test
    public void createScript() {
        Status status = runCreateSuccessScript();
        Assert.assertTrue(status.isSuccess());

        String query
                = "SELECT series_id, title, release_date "
                + "FROM series WHERE series_id = 1";

        // Executes data query with specified transaction control settings.
        QueryReader result = retryCtx.supplyResult(
                session -> QueryReader.readFrom(session.createQuery(query, TxMode.SERIALIZABLE_RW))
        ).join().getValue();

        ResultSetReader rs = result.getResultSet(0);

        // Check that table exists and contains no data
        Assert.assertFalse(rs.next());
    }

    /**
     * Ensures that script execution fails when it contains syntax errors.
     * <p>
     * Attempts to execute a malformed YQL script and verifies that the result
     * indicates failure.
     */
    @Test
    public void createScriptShouldFail() {
        Status statusOperation = runCreateScript("CREATE TABLE series ("
                + "  series_id UInt64,"
                + "  title Text,"
                + "  series_info Text,"
                + "  release_date Date,"
                + "  PRIMARY KEY(series_id)"
                + ");"
                + "ZCREATE TABLE seasons ("
                + "  series_id UInt64,"
                + "  season_id UInt64,"
                + "  title Text,"
                + "  first_aired Date,"
                + "  last_aired Date,"
                + "  PRIMARY KEY(series_id, season_id)"
                + ")");

        Assert.assertFalse(statusOperation.isSuccess());

        String query
                = "SELECT series_id, title, release_date "
                + "FROM series WHERE series_id = 1";

        // Executes data query with specified transaction control settings.
        Result<QueryReader> result = retryCtx.supplyResult(
                session -> QueryReader.readFrom(session.createQuery(query, TxMode.SERIALIZABLE_RW))
        ).join();


        // Check that table exists and contains no data
        Assert.assertFalse(result.isSuccess());
    }

    /**
     * Validates sequential script execution using QueryClient.executeScript.
     * <p>
     * Creates tables, then inserts data in a separate script execution, and
     * verifies data persistence.
     */
    @Test
    public void createInsertQueryScript() {
        runCreateSuccessScript();

        ExecuteScriptSettings executeScriptSettings = ExecuteScriptSettings.newBuilder()
                .withExecMode(QueryExecMode.EXECUTE)
                .withTtl(Duration.ofSeconds(10))
                .build();

        Status status = scriptClient.startJoinScript(""
                        + "DECLARE $values AS List<Struct<"
                        + "  series_id: Uint64,"
                        + "  season_id: Uint64,"
                        + "  title: Text,"
                        + "  first_aired: Date,"
                        + "  last_aired: Date"
                        + ">>;"
                        + "DECLARE $values1 AS List<Struct<"
                        + "                        series_id: Uint64,"
                        + "                        title: Text,"
                        + "                        series_info: Text,"
                        + "                        release_date: Date"
                        + "                        >>;"
                        + "UPSERT INTO seasons SELECT * FROM AS_TABLE($values);"
                        + "UPSERT INTO series SELECT * FROM AS_TABLE($values1);",
                Params.of("$values", seasonsData, "$values1", seriesData), executeScriptSettings);

        Assert.assertTrue(status.isSuccess());

        String query
                = "SELECT series_id "
                + "FROM seasons WHERE series_id = 1";

        // Executes data query with specified transaction control settings.
        Result<QueryReader> result = retryCtx.supplyResult(
                session -> QueryReader.readFrom(session.createQuery(query, TxMode.SERIALIZABLE_RW))
        ).join();

        ResultSetReader rs = result.getValue().getResultSet(0);

        Assert.assertTrue(rs.next());
        Assert.assertEquals(1, rs.getColumn("series_id").getUint64());
    }

    /**
     * Validate that find script is working
     * <p>
     * In this test we start  script then try it to find and wait for finish execution
     */
    @Test
    public void findAndStartScript() {
        runCreateSuccessScript();

        ExecuteScriptSettings executeScriptSettings = ExecuteScriptSettings.newBuilder()
                .withExecMode(QueryExecMode.EXECUTE)
                .build();

        CompletableFuture<OperationScript> future = scriptClient.startScript(""
                        + "DECLARE $values AS List<Struct<"
                        + "  series_id: Uint64,"
                        + "  season_id: Uint64,"
                        + "  title: Text,"
                        + "  first_aired: Date,"
                        + "  last_aired: Date"
                        + ">>;"
                        + "DECLARE $values1 AS List<Struct<"
                        + "                        series_id: Uint64,"
                        + "                        title: Text,"
                        + "                        series_info: Text,"
                        + "                        release_date: Date"
                        + "                        >>;"
                        + "UPSERT INTO seasons SELECT * FROM AS_TABLE($values);"
                        + "UPSERT INTO series SELECT * FROM AS_TABLE($values1);"
                        + "SELECT season_id FROM seasons where series_id = 1 order by series_id;",
                Params.of("$values", seasonsData, "$values1", seriesData), executeScriptSettings);

        OperationScript operationScript = future.join();


        OperationScript operationScript1 = scriptClient.findScript(operationScript.getId(), FindScriptSettings.newBuilder().build()).join();


        Assert.assertEquals(operationScript1.getId(), operationScript.getId());

        Status status = operationScript1.waitForResult();
        Assert.assertTrue(status.isSuccess());
    }

    /**
     * Tests fetching results from an executed script using {@link FetchScriptSettings}.
     *
     * <p>Scenario:
     * <ol>
     *   <li>Create tables</li>
     *   <li>Insert sample data via parameterized script</li>
     *   <li>Fetch the result set from the executed operation</li>
     * </ol>
     *
     * @throws ExecutionException   if the script future fails
     * @throws InterruptedException if the fetch operation is interrupted
     */
    @Test
    public void fetchScript() throws ExecutionException, InterruptedException {
        runCreateSuccessScript();

        ExecuteScriptSettings executeScriptSettings = ExecuteScriptSettings.newBuilder()
                .withExecMode(QueryExecMode.EXECUTE)
                .build();

        CompletableFuture<OperationScript> future = scriptClient.startScript(""
                        + "DECLARE $values AS List<Struct<"
                        + "  series_id: Uint64,"
                        + "  season_id: Uint64,"
                        + "  title: Text,"
                        + "  first_aired: Date,"
                        + "  last_aired: Date"
                        + ">>;"
                        + "DECLARE $values1 AS List<Struct<"
                        + "                        series_id: Uint64,"
                        + "                        title: Text,"
                        + "                        series_info: Text,"
                        + "                        release_date: Date"
                        + "                        >>;"
                        + "UPSERT INTO seasons SELECT * FROM AS_TABLE($values);"
                        + "UPSERT INTO series SELECT * FROM AS_TABLE($values1);"
                        + "SELECT season_id FROM seasons where series_id = 1 order by series_id;",
                Params.of("$values", seasonsData, "$values1", seriesData), executeScriptSettings);

        OperationScript operationScript = future.join();
        Status status = operationScript.waitForResult();


        FetchScriptSettings fetchScriptSettings1 = FetchScriptSettings.newBuilder()
                .withRowsLimit(1)
                .withSetResultSetIndex(0)
                .withEOperationId(operationScript.getId())
                .withFetchToken("")
                .build();

        FetchScriptResult fetchScriptResult = checkFetch(fetchScriptSettings1, 1);

        FetchScriptSettings fetchScriptSettings2 = FetchScriptSettings.newBuilder()
                .withRowsLimit(1)
                .withSetResultSetIndex(0)
                .withEOperationId(operationScript.getId())
                .withFetchToken(fetchScriptResult.getNextFetchToken())
                .build();

        checkFetch(fetchScriptSettings2, 2);
    }

    private FetchScriptResult checkFetch(FetchScriptSettings fetchScriptSettings, int value) {
        FetchScriptResult fetchScriptResult = scriptClient.fetchScriptResults(fetchScriptSettings).join();

        ValueProtos.ResultSet resultSet = fetchScriptResult.getResultSet();
        Assert.assertEquals(1, resultSet.getRowsCount());

        ResultSetReader reader = ProtoValueReaders.forResultSet(resultSet);
        reader.next();
        Assert.assertEquals(value, reader.getColumn(0).getUint64());
        return fetchScriptResult;
    }

    private Status runCreateSuccessScript() {
        return runCreateScript(""
                + "CREATE TABLE series ("
                + "  series_id UInt64,"
                + "  title Text,"
                + "  series_info Text,"
                + "  release_date Date,"
                + "  PRIMARY KEY(series_id)"
                + ");"
                + ""
                + "CREATE TABLE seasons ("
                + "  series_id UInt64,"
                + "  season_id UInt64,"
                + "  title Text,"
                + "  first_aired Date,"
                + "  last_aired Date,"
                + "  PRIMARY KEY(series_id, season_id)"
                + ")");
    }

    private Status runCreateScript(String query) {
        return scriptClient.startJoinScript(query, Params.empty(), ExecuteScriptSettings.newBuilder().build());
    }
}
