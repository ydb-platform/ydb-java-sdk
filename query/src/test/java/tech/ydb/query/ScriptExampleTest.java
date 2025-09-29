package tech.ydb.query;

import java.time.Instant;
import java.time.LocalDate;
import java.time.Month;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import tech.ydb.common.transaction.TxMode;
import tech.ydb.core.Result;
import tech.ydb.core.Status;
import tech.ydb.proto.OperationProtos;
import tech.ydb.query.result.QueryInfo;
import tech.ydb.query.settings.ExecuteScriptSettings;
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
 * @author Alexandr Gorshenin <alexandr268@ydb.tech>
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class ScriptExampleTest {
    @ClassRule
    public final static GrpcTransportRule ydbRule = new GrpcTransportRule();

    private static QueryClient client;
    private static SessionRetryContext retryCtx;

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
        Result<QueryInfo> t1 = retryCtx.supplyResult(session -> session.createQuery("DROP TABLE series;", TxMode.NONE).execute())
                .join();
        Result<QueryInfo> t2 = retryCtx.supplyResult(session -> session.createQuery("DROP TABLE seasons;", TxMode.NONE).execute())
                .join();

        client.close();
    }

    /**
     * Simple test that check script without parameters execute without errors
     */
    @Test
    public void createScript() {

        Result<OperationProtos.Operation> operationResult = retryCtx.supplyResult(session -> session.executeScript(""
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

        Assert.assertTrue(operationResult.isSuccess());

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

        String query1
                = "SELECT series_id, season_id, title "
                + "FROM seasons WHERE series_id = 1";

        // Executes data query with specified transaction control settings.
        QueryReader result1 = retryCtx.supplyResult(
                session -> QueryReader.readFrom(session.createQuery(query, TxMode.SERIALIZABLE_RW))
        ).join().getValue();

        ResultSetReader rs1 = result.getResultSet(0);

        // Check that table exists and contains no data
        Assert.assertFalse(rs1.next());
    }


    /**
     * Simple test that check script without parameters execute without errors
     */
    @Test
    public void createScriptShouldFail() {
        Result<OperationProtos.Operation> operationResult = retryCtx.supplyResult(session -> session.executeScript(""
                        + "CREATE TABLE series ("
                        + "  series_id UInt64,"
                        + "  title Text,"
                        + "  series_info Text,"
                        + "  release_date Date,"
                        + "  PRIMARY KEY(series_id)"
                        + ");"
                        + ""
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

        Assert.assertTrue(operationResult.isSuccess());

        String query
                = "SELECT series_id, title, release_date "
                + "FROM series WHERE series_id = 1";

        // Executes data query with specified transaction control settings.
        Result<QueryReader> result = retryCtx.supplyResult(
                session -> QueryReader.readFrom(session.createQuery(query, TxMode.SERIALIZABLE_RW))
        ).join();


        // Check that table exists and contains no data
        Assert.assertFalse(result.isSuccess());

        String query1
                = "SELECT series_id, season_id, title "
                + "FROM seasons WHERE series_id = 1";

        Result<QueryReader> result1 = retryCtx.supplyResult(
                session -> QueryReader.readFrom(session.createQuery(query, TxMode.SERIALIZABLE_RW))
        ).join();

        Assert.assertFalse(result1.isSuccess());
    }

    @Test
    public void createInsertScript() {
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

        ExecuteScriptSettings executeScriptSettings = tech.ydb.query.settings.ExecuteScriptSettings.newBuilder()
                //  .withExecMode(QueryExecMode.EXECUTE)
                .build();

        Result<OperationProtos.Operation> operationResult = retryCtx.supplyResult(session -> session.executeScript(""
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

        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
/*

        retryCtx.supplyResult(session -> session.createQuery(""
                        + "DECLARE $values AS List<Struct<"
                        + "  series_id: Uint64,"
                        + "  season_id: Uint64,"
                        + "  title: Text,"
                        + "  first_aired: Date,"
                        + "  last_aired: Date"
                        + ">>;"
                        + "UPSERT INTO seasons SELECT * FROM AS_TABLE($values)",
                TxMode.SERIALIZABLE_RW,
                Params.of("$values", seasonsData)
        ).execute()).join().getStatus().expectSuccess("upsert problem");*/

        Result<OperationProtos.Operation> t1 = retryCtx.supplyResult(session -> session.executeScript(""
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
        ).join();
        t1.getValue();

    /*    retryCtx.supplyResult(session -> session.executeScript(""+
                        "DECLARE $values1 AS List<Struct<" +
                        "                        series_id: Uint64," +
                        "                        season_id: Uint64," +
                        "                        title: Text," +
                        "                        first_aired: Date," +
                        "                        last_aired: Date" +
                        "                        >>;"
                        + "UPSERT INTO seasons SELECT * FROM AS_TABLE($values1);"
                        /*+ "UPSERT INTO series SELECT * FROM AS_TABLE($values1);",
                Params.of("$values1", seasonsData), executeScriptSettings)
        ).join().getStatus().expectSuccess("upsert problem");
*/


        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

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
     //   Assert.assertEquals("IT Crowd", rs.getColumn("title").getText());
     //   Assert.assertEquals(LocalDate.of(2006, Month.FEBRUARY, 3), rs.getColumn("release_date").getDate());




        operationResult.getValue();
    }
}
