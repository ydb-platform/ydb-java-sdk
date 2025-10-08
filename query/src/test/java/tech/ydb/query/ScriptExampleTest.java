package tech.ydb.query;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import tech.ydb.common.transaction.TxMode;
import tech.ydb.core.Result;
import tech.ydb.core.Status;
import tech.ydb.core.operation.Operation;
import tech.ydb.core.operation.OperationTray;
import tech.ydb.proto.query.YdbQuery;
import tech.ydb.proto.scripting.ScriptingProtos;
import tech.ydb.query.settings.ExecuteScriptSettings;
import tech.ydb.query.settings.FetchScriptSettings;
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
 *
 */
public class ScriptExampleTest {

    @ClassRule
    public final static GrpcTransportRule ydbRule = new GrpcTransportRule();

    private static QueryClient client;
    private static SessionRetryContext retryCtx;

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

        Assert.assertNotNull(client.getScheduler());
    }

    @AfterClass
    public static void clean() {
        retryCtx.supplyResult(session -> session.createQuery("DROP TABLE series;", TxMode.NONE).execute())
                .join();
        retryCtx.supplyResult(session -> session.createQuery("DROP TABLE seasons;", TxMode.NONE).execute())
                .join();

        client.close();
    }

    /**
     * Simple test that check script without parameters execute without errors
     */
    @Test
    public void createScript() {
        Result<ScriptingProtos.ExecuteYqlResult> result0 = retryCtx.supplyResult(session -> session.executeScriptYql(
                        "CREATE TABLE series ("
                                + "  series_id UInt64,"
                                + "  title Text,"
                                + "  series_info Text,"
                                + "  release_date Date,"
                                + "  PRIMARY KEY(series_id)"
                                + ");"
                                + "CREATE TABLE seasons ("
                                + "  series_id UInt64,"
                                + "  season_id UInt64,"
                                + "  title Text,"
                                + "  first_aired Date,"
                                + "  last_aired Date,"
                                + "  PRIMARY KEY(series_id, season_id)"
                                + ")"
                )
        ).join();

        Assert.assertTrue(result0.isSuccess());

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
     * Simple test that check script without parameters execute with errors if script has errors in text
     */
    @Test
    public void createScriptShouldFail() {
        Result<ScriptingProtos.ExecuteYqlResult> operationResult = retryCtx.supplyResult(session -> session.executeScriptYql(
                        "CREATE TABLE series ("
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
                                + ")"
                )
        ).join();

        Assert.assertFalse(operationResult.isSuccess());

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
     * Test create and then insert data using {@link ScriptingProtos} proto
     */
    @Test
    public void createInsertYqlScript() {
        Result<ScriptingProtos.ExecuteYqlResult> task = retryCtx.supplyResult(session -> session.executeScriptYql(""
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
                        + ")"
                )
        ).join();


        ExecuteScriptSettings executeScriptSettings = tech.ydb.query.settings.ExecuteScriptSettings.newBuilder()
                //  .withExecMode(QueryExecMode.EXECUTE)
                .build();

        Result<ScriptingProtos.ExecuteYqlResult> task1 = retryCtx.supplyResult(session -> session.executeScriptYql(""
                        + "DECLARE $values AS List<Struct<"
                        + "  series_id: Uint64,"
                        + "  season_id: Uint64,"
                        + "  title: Text,"
                        + "  first_aired: Date,"
                        + "  last_aired: Date"
                        + ">>;"
                        + "DECLARE $values1 AS List<Struct<"
                        + "  series_id: Uint64,"
                        + "  title: Text,"
                        + "  series_info: Text,"
                        + "  release_date: Date"
                        + " >>;"
                        + "UPSERT INTO seasons SELECT * FROM AS_TABLE($values);"
                        + "UPSERT INTO series SELECT * FROM AS_TABLE($values1);",
                Params.of("$values", seasonsData, "$values1", seriesData), executeScriptSettings)
        ).join();

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
     * Run 2 scripts one after another.
     * Ch
     */
    @Test
    public void createInsertQueryScript() {
        CompletableFuture<Operation<Status>> s =
                retryCtx.supplyOperation(querySession -> querySession.executeScript(""
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
                        + ")"));

        retryCtx.supplyStatus(
                ss ->
                        s.thenCompose(operation -> OperationTray.fetchOperation(operation, 1))
        ).join();


        ExecuteScriptSettings executeScriptSettings = tech.ydb.query.settings.ExecuteScriptSettings.newBuilder()
                .build();


        CompletableFuture<Operation<Status>> test = retryCtx.supplyOperation(querySession -> querySession.executeScript(""
                        + "DECLARE $values AS List<Struct<"
                        + "  series_id: Uint64,"
                        + "  season_id: Uint64,"
                        + "  title: Text,"
                        + "  first_aired: Date,"
                        + "  last_aired: Date"
                        + ">>;"
                        + "DECLARE $values1 AS List<Struct<" +
                        "                        series_id: Uint64," +
                        "                        title: Text," +
                        "                        series_info: Text," +
                        "                        release_date: Date" +
                        "                        >>;"
                        + "UPSERT INTO seasons SELECT * FROM AS_TABLE($values);"
                        + "UPSERT INTO series SELECT * FROM AS_TABLE($values1);",
                Params.of("$values", seasonsData, "$values1", seriesData), executeScriptSettings)
        );


        retryCtx.supplyStatus(
                ss ->
                        test.thenCompose(operation -> OperationTray.fetchOperation(operation, 1))
        ).join();

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
     * Check fetch result
     *
     * @throws ExecutionException
     * @throws InterruptedException
     */
    @Test
    public void fetchScript() throws ExecutionException, InterruptedException {
        Result<ScriptingProtos.ExecuteYqlResult> result0 = retryCtx.supplyResult(session -> session.executeScriptYql(""
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
                        + ")"
                )
        ).join();

        ExecuteScriptSettings executeScriptSettings = tech.ydb.query.settings.ExecuteScriptSettings.newBuilder()
                //  .withExecMode(QueryExecMode.EXECUTE)
                .build();

        CompletableFuture<Operation<Status>> s =
                retryCtx.supplyOperation(querySession -> querySession.executeScript(""
                                + "DECLARE $values AS List<Struct<"
                                + "  series_id: Uint64,"
                                + "  season_id: Uint64,"
                                + "  title: Text,"
                                + "  first_aired: Date,"
                                + "  last_aired: Date"
                                + ">>;"
                                + "DECLARE $values1 AS List<Struct<" +
                                "                        series_id: Uint64," +
                                "                        title: Text," +
                                "                        series_info: Text," +
                                "                        release_date: Date" +
                                "                        >>;"
                                + "UPSERT INTO seasons SELECT * FROM AS_TABLE($values);"
                                + "UPSERT INTO series SELECT * FROM AS_TABLE($values1);" +
                                "SELECT series_id FROM seasons where series_id = 1 order by series_id;",
                        Params.of("$values", seasonsData, "$values1", seriesData), executeScriptSettings));

        retryCtx.supplyStatus(
                ss ->
                        s.thenCompose(operation -> OperationTray.fetchOperation(operation, 1))
        ).join();


        FetchScriptSettings fetchScriptSettings = FetchScriptSettings.newBuilder()
                .setRowsLimit(2)
                .setSetResultSetIndex(0)
                .withEOperationId(s.get().getId())
                .withFetchToken("")
                .build();


        Result<YdbQuery.FetchScriptResultsResponse> test = retryCtx.supplyResult(
                session -> session.fetchScriptResults(""
                                + "SELECT series_id FROM seasons;",
                        Params.empty(), fetchScriptSettings)
        ).join();

        tech.ydb.proto.ValueProtos.ResultSet resultSet = test.getValue().getResultSet();


        Assert.assertEquals(2, resultSet.getRowsCount());
        ResultSetReader reader = ProtoValueReaders.forResultSet(resultSet);
        reader.next();
        Assert.assertEquals(1, reader.getColumn(0).getUint64());
        reader.next();
        Assert.assertEquals(1, reader.getColumn(0).getUint64());
    }
}
