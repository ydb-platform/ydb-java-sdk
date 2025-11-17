package tech.ydb.query.impl;

import java.time.Duration;
import java.util.Arrays;
import java.util.concurrent.ExecutionException;
import java.util.function.Predicate;
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
import tech.ydb.core.operation.Operation;
import tech.ydb.query.QueryClient;
import tech.ydb.query.TestExampleData;
import tech.ydb.query.script.ScriptClient;
import tech.ydb.query.script.impl.ScriptClientImpl;
import tech.ydb.query.script.result.ScriptResultPart;
import tech.ydb.query.script.settings.ExecuteScriptSettings;
import tech.ydb.query.script.settings.FetchScriptSettings;
import tech.ydb.query.script.settings.FindScriptSettings;
import tech.ydb.query.settings.QueryExecMode;
import tech.ydb.query.tools.QueryReader;
import tech.ydb.query.tools.SessionRetryContext;
import tech.ydb.table.query.Params;
import tech.ydb.table.result.ResultSetReader;
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

        retryCtx.supplyResult(session -> session.createQuery(""
                + "CREATE TABLE series ("
                + "  series_id UInt64,"
                + "  title Text,"
                + "  series_info Text,"
                + "  release_date Date,"
                + "  PRIMARY KEY(series_id)"
                + ")", TxMode.NONE).execute()
        ).join().getStatus().expectSuccess("Can't create table series");

        retryCtx.supplyResult(session -> session.createQuery(""
                + "CREATE TABLE seasons ("
                + "  series_id UInt64,"
                + "  season_id UInt64,"
                + "  title Text,"
                + "  first_aired Date,"
                + "  last_aired Date,"
                + "  PRIMARY KEY(series_id, season_id)"
                + ")", TxMode.NONE).execute()
        ).join().getStatus().expectSuccess("Can't create table seasons");
    }

    @After
    public void clean() {
        retryCtx.supplyResult(session -> session.createQuery("delete from series;", TxMode.NONE).execute())
                .join();
        retryCtx.supplyResult(session -> session.createQuery("delete from seasons;", TxMode.NONE).execute())
                .join();
    }

    @AfterClass
    public static void cleanAll() {
        retryCtx.supplyResult(session -> session.createQuery("drop table series;", TxMode.NONE).execute())
                .join();
        retryCtx.supplyResult(session -> session.createQuery("drop table seasons;", TxMode.NONE).execute())
                .join();

        client.close();
    }

    /**
     * Ensures that script execution fails when it contains syntax errors.
     * <p>
     * Attempts to execute a malformed YQL script and verifies that the result
     * indicates failure.
     */
    @Test
    public void createScriptShouldFail() {
        Status statusOperation = runCreateScript("CREATE TABLE series2 ("
                + "  series_id UInt64,"
                + "  title Text,"
                + "  series_info Text,"
                + "  release_date Date,"
                + "  PRIMARY KEY(series_id)"
                + ");"
                + "ZCREATE TABLE seasons2 ("
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
                + "FROM series2 WHERE series_id = 1";

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
        ExecuteScriptSettings executeScriptSettings = ExecuteScriptSettings.newBuilder()
                .withExecMode(QueryExecMode.EXECUTE)
                .withTtl(Duration.ofSeconds(10))
                .build();

        Status status = scriptClient.startQueryScript(""
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
                        Params.of("$values", seasonsData, "$values1", seriesData), executeScriptSettings)
                .thenCompose(p -> scriptClient.fetchQueryScriptStatus(p, 1))
                .join();

        Assert.assertNotNull(status);
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
        ExecuteScriptSettings executeScriptSettings = ExecuteScriptSettings.newBuilder()
                .withExecMode(QueryExecMode.EXECUTE)
                .build();

        Operation<Status> operation = scriptClient.startQueryScript(""
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
                Params.of("$values", seasonsData, "$values1", seriesData), executeScriptSettings).join();


        Operation<Status> operation1 = scriptClient.findQueryScript(operation.getId(), FindScriptSettings.newBuilder().build()).join();


        Assert.assertEquals(operation.getId(), operation1.getId());

        Status status = scriptClient.fetchQueryScriptStatus(operation1, 1).join();
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
     */
    @Test
    public void fetchScript() {
        ExecuteScriptSettings executeScriptSettings = ExecuteScriptSettings.newBuilder()
                .withExecMode(QueryExecMode.EXECUTE)
                .build();

        Operation<Status> operation = scriptClient.startQueryScript(""
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
                Params.of("$values", seasonsData, "$values1", seriesData), executeScriptSettings).join();

        scriptClient.fetchQueryScriptStatus(operation, 1).join();

        FetchScriptSettings fetchScriptSettings1 = FetchScriptSettings.newBuilder()
                .withRowsLimit(1)
                .withSetResultSetIndex(0)
                .build();

        Result<ScriptResultPart> resultPartResult = scriptClient.fetchQueryScriptResult(operation, null, fetchScriptSettings1)
                .join();

        checkFetch(resultPartResult, 1);

        FetchScriptSettings fetchScriptSettings2 = FetchScriptSettings.newBuilder()
                .withRowsLimit(1)
                .withSetResultSetIndex(resultPartResult.getValue().getResultSetIndex())
                .build();

        Result<ScriptResultPart> resultPartResult1 = scriptClient.fetchQueryScriptResult(operation, resultPartResult.getValue(), fetchScriptSettings2)
                .join();

        checkFetch(resultPartResult1, 2);
    }

    @Test
    public void fetchScriptWithManyResultSet() {
        ExecuteScriptSettings executeScriptSettings = ExecuteScriptSettings.newBuilder()
                .withExecMode(QueryExecMode.EXECUTE)
                .build();

        Operation<Status> operation = scriptClient.startQueryScript(""
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
                        + "SELECT season_id FROM seasons where series_id = 1 order by series_id;"
                        + "SELECT season_id FROM seasons where series_id = 2 order by series_id;",
                Params.of("$values", seasonsData, "$values1", seriesData), executeScriptSettings).join();

        scriptClient.fetchQueryScriptStatus(operation, 1).join();

        FetchScriptSettings fetchScriptSettings1 = FetchScriptSettings.newBuilder()
                .withRowsLimit(10)
                .withSetResultSetIndex(0)
                .build();

        Result<ScriptResultPart> resultPartResult = scriptClient.fetchQueryScriptResult(operation, null, fetchScriptSettings1)
                .join();

        ScriptResultPart part = resultPartResult.getValue();

        ResultSetReader reader = part.getResultSetReader();

        Assert.assertEquals(4, reader.getRowCount());

        FetchScriptSettings fetchScriptSettings2 = FetchScriptSettings.newBuilder()
                .withRowsLimit(10)
                .withSetResultSetIndex(1)
                .build();

        Result<ScriptResultPart> resultPartResult1 = scriptClient.fetchQueryScriptResult(operation, null, fetchScriptSettings2)
                .join();

        ScriptResultPart part1 = resultPartResult1.getValue();

        ResultSetReader reader2 = part1.getResultSetReader();

        Assert.assertEquals(5, reader2.getRowCount());
    }

    @Test
    public void fetchScriptWithError() {
        ExecuteScriptSettings executeScriptSettings = ExecuteScriptSettings.newBuilder()
                .withExecMode(QueryExecMode.EXECUTE)
                .build();

        Operation<Status> operation = scriptClient.startQueryScript(""
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
                        + "SELECT season_id FROM seasons where series_ids = 1 order by series_id;",
                Params.of("$values", seasonsData, "$values1", seriesData), executeScriptSettings).join();

        Status status = scriptClient.fetchQueryScriptStatus(operation, 1).join();

        FetchScriptSettings fetchScriptSettings1 = FetchScriptSettings.newBuilder()
                .withRowsLimit(1)
                .withSetResultSetIndex(0)
                .build();

        Result<ScriptResultPart> resultPartResult = scriptClient.fetchQueryScriptResult(operation, null, fetchScriptSettings1)
                .join();

        Assert.assertTrue(resultPartResult.getValue().hasErrors());

        Assert.assertTrue(
                Arrays.stream(resultPartResult.getValue().getIssues()).anyMatch(
                        issue -> issue.toString().contains("not found: series_ids.")));
    }

    private void checkFetch(Result<ScriptResultPart> resultPartResult, int value) {
        ScriptResultPart scriptResultPart = resultPartResult.getValue();

        ResultSetReader reader = scriptResultPart.getResultSetReader();
        Assert.assertEquals(1, reader.getRowCount());

        reader.next();
        Assert.assertEquals(value, reader.getColumn(0).getUint64());
    }

    private static Status runCreateScript(String query) {
        return scriptClient.startQueryScript(query, Params.empty(), ExecuteScriptSettings.newBuilder().build())
                .thenCompose(p -> scriptClient.fetchQueryScriptStatus(p, 1))
                .join();
    }
}
