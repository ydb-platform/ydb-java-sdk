package tech.ydb.query;

import java.time.Instant;
import java.time.LocalDate;
import java.time.Month;
import java.util.Arrays;
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
import tech.ydb.core.Status;
import tech.ydb.core.grpc.GrpcReadStream;
import tech.ydb.table.SessionRetryContext;
import tech.ydb.table.TableClient;
import tech.ydb.table.description.TableDescription;
import tech.ydb.table.query.DataQueryResult;
import tech.ydb.table.query.Params;
import tech.ydb.table.result.ResultSetReader;
import tech.ydb.table.settings.BulkUpsertSettings;
import tech.ydb.table.settings.ExecuteScanQuerySettings;
import tech.ydb.table.transaction.TableTransaction;
import tech.ydb.table.transaction.TxControl;
import tech.ydb.table.values.ListType;
import tech.ydb.table.values.ListValue;
import tech.ydb.table.values.PrimitiveType;
import tech.ydb.table.values.PrimitiveValue;
import tech.ydb.table.values.StructType;
import tech.ydb.test.junit4.GrpcTransportRule;

/**
 *
 * @author Alexandr Gorshenin <alexandr268@ydb.tech>
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TableExampleTest {
    @ClassRule
    public final static GrpcTransportRule ydbRule = new GrpcTransportRule();

    private static TableClient client;
    private static SessionRetryContext retryCtx;

    @BeforeClass
    public static void init() {
        client = QueryClient.newTableClient(ydbRule)
                .sessionPoolSize(0, 5)
                .build();
        retryCtx = SessionRetryContext.create(client).build();
    }

    @AfterClass
    public static void clean() {
        retryCtx.supplyStatus(session -> session.dropTable(ydbRule.getDatabase() + "/episodes"))
                .join();
        retryCtx.supplyStatus(session -> session.dropTable(ydbRule.getDatabase() + "/seasons"))
                .join();
        retryCtx.supplyStatus(session -> session.dropTable(ydbRule.getDatabase() + "/series"))
                .join();

        client.close();
    }

    @Test
    public void step01_createTables() {
        TableDescription seriesTable = TableDescription.newBuilder()
            .addNonnullColumn("series_id", PrimitiveType.Uint64)
            .addNullableColumn("title", PrimitiveType.Text)
            .addNullableColumn("series_info", PrimitiveType.Text)
            .addNullableColumn("release_date", PrimitiveType.Date)
            .setPrimaryKey("series_id")
            .build();

        retryCtx.supplyStatus(session -> session.createTable(ydbRule.getDatabase() + "/series", seriesTable))
                .join().expectSuccess("Can't create table /series");

        TableDescription seasonsTable = TableDescription.newBuilder()
            .addNonnullColumn("series_id", PrimitiveType.Uint64)
            .addNonnullColumn("season_id", PrimitiveType.Uint64)
            .addNullableColumn("title", PrimitiveType.Text)
            .addNullableColumn("first_aired", PrimitiveType.Date)
            .addNullableColumn("last_aired", PrimitiveType.Date)
            .setPrimaryKeys("series_id", "season_id")
            .build();

        retryCtx.supplyStatus(session -> session.createTable(ydbRule.getDatabase() + "/seasons", seasonsTable))
                .join().expectSuccess("Can't create table /seasons");

        TableDescription episodesTable = TableDescription.newBuilder()
            .addNonnullColumn("series_id", PrimitiveType.Uint64)
            .addNonnullColumn("season_id", PrimitiveType.Uint64)
            .addNonnullColumn("episode_id", PrimitiveType.Uint64)
            .addNullableColumn("title", PrimitiveType.Text)
            .addNullableColumn("air_date", PrimitiveType.Date)
            .setPrimaryKeys("series_id", "season_id", "episode_id")
            .build();

        retryCtx.supplyStatus(session -> session.createTable(ydbRule.getDatabase() + "/episodes", episodesTable))
                .join().expectSuccess("Can't create table /episodes");
    }

    @Test
    public void step02_describeTables() {
        TableDescription series = retryCtx.supplyResult(
                session -> session.describeTable(ydbRule.getDatabase() + "/series")
        ).join().getValue();

        Assert.assertEquals(Arrays.asList("series_id"), series.getPrimaryKeys());
        Assert.assertEquals(4, series.getColumns().size());
        Assert.assertEquals("series_id", series.getColumns().get(0).getName());
        Assert.assertEquals("title", series.getColumns().get(1).getName());
        Assert.assertEquals("series_info", series.getColumns().get(2).getName());
        Assert.assertEquals("release_date", series.getColumns().get(3).getName());

        TableDescription seasons = retryCtx.supplyResult(
                session -> session.describeTable(ydbRule.getDatabase() + "/seasons")
        ).join().getValue();

        Assert.assertEquals(Arrays.asList("series_id", "season_id"), seasons.getPrimaryKeys());
        Assert.assertEquals(5, seasons.getColumns().size());
        Assert.assertEquals("series_id", seasons.getColumns().get(0).getName());
        Assert.assertEquals("season_id", seasons.getColumns().get(1).getName());
        Assert.assertEquals("title", seasons.getColumns().get(2).getName());
        Assert.assertEquals("first_aired", seasons.getColumns().get(3).getName());
        Assert.assertEquals("last_aired", seasons.getColumns().get(4).getName());

        TableDescription episodes = retryCtx.supplyResult(
                session -> session.describeTable(ydbRule.getDatabase() + "/episodes")
        ).join().getValue();

        Assert.assertEquals(Arrays.asList("series_id", "season_id", "episode_id"), episodes.getPrimaryKeys());
        Assert.assertEquals(5, episodes.getColumns().size());
        Assert.assertEquals("series_id", seasons.getColumns().get(0).getName());
        Assert.assertEquals("season_id", seasons.getColumns().get(1).getName());
        Assert.assertEquals("episode_id", episodes.getColumns().get(2).getName());
        Assert.assertEquals("title", episodes.getColumns().get(3).getName());
        Assert.assertEquals("air_date", episodes.getColumns().get(4).getName());
    }

    @Test
    public void step03_upsertTablesData() {
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
                        "release_date", PrimitiveValue.newDate(series.releaseDate()),
                        "series_info", PrimitiveValue.newText(series.seriesInfo())
                )).collect(Collectors.toList())
        );
        // Upsert list of series to table
        retryCtx.supplyStatus(session -> session.executeBulkUpsert(
                ydbRule.getDatabase() + "/series", seriesData, new BulkUpsertSettings()
        )).join().expectSuccess("bulk upsert problem");


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
        // Upsert list of series to seasons
        retryCtx.supplyStatus(session -> session.executeBulkUpsert(
                ydbRule.getDatabase() + "/seasons", seasonsData, new BulkUpsertSettings()
        )).join().expectSuccess("bulk upsert problem");


        // Create type for struct of episode
        StructType episodeType = StructType.of(
                "series_id", PrimitiveType.Uint64,
                "season_id", PrimitiveType.Uint64,
                "episode_id", PrimitiveType.Uint64,
                "title", PrimitiveType.Text,
                "air_date", PrimitiveType.Date
        );
        // Create and fill list of episodes
        ListValue episodesData = ListType.of(episodeType).newValue(
                TestExampleData.EPISODES.stream().map(episode -> episodeType.newValue(
                        "series_id", PrimitiveValue.newUint64(episode.seriesID()),
                        "season_id", PrimitiveValue.newUint64(episode.seasonID()),
                        "episode_id", PrimitiveValue.newUint64(episode.episodeID()),
                        "title", PrimitiveValue.newText(episode.title()),
                        "air_date", PrimitiveValue.newDate(episode.airDate())
                )).collect(Collectors.toList())
        );

        // Upsert list of series to episodes
        retryCtx.supplyStatus(session -> session.executeBulkUpsert(
                ydbRule.getDatabase() + "/episodes", episodesData, new BulkUpsertSettings()
        )).join().expectSuccess("bulk upsert problem");
    }

    @Test
    public void step04_upsertSimple() {
        String query
                = "UPSERT INTO episodes (series_id, season_id, episode_id, title) "
                + "VALUES (2, 6, 1, \"TBD\");";

        // Begin new transaction with SerializableRW mode
        TxControl<?> txControl = TxControl.serializableRw().setCommitTx(true);

        // Executes data query with specified transaction control settings.
        retryCtx.supplyResult(session -> session.executeDataQuery(query, txControl))
            .join().getValue();
    }

    @Test
    public void step05_selectSimple() {
        String query
                = "SELECT series_id, title, release_date "
                + "FROM series WHERE series_id = 1;";

        // Begin new transaction with SerializableRW mode
        TxControl<?> txControl = TxControl.serializableRw().setCommitTx(true);

        // Executes data query with specified transaction control settings.
        DataQueryResult result = retryCtx.supplyResult(session -> session.executeDataQuery(query, txControl))
                .join().getValue();

        ResultSetReader rs = result.getResultSet(0);

        Assert.assertTrue(rs.next());
        Assert.assertEquals(1, rs.getColumn("series_id").getUint64());
        Assert.assertEquals("IT Crowd", rs.getColumn("title").getText());
        Assert.assertEquals(LocalDate.of(2006, Month.FEBRUARY, 3), rs.getColumn("release_date").getDate());

        Assert.assertFalse(rs.next());
    }

    @Test
    public void step06_selectWithParams() {
        String query
                = "DECLARE $seriesId AS Uint64; "
                + "DECLARE $seasonId AS Uint64; "
                + "SELECT sa.title AS season_title, sr.title AS series_title "
                + "FROM seasons AS sa INNER JOIN series AS sr ON sa.series_id = sr.series_id "
                + "WHERE sa.series_id = $seriesId AND sa.season_id = $seasonId";

        // Begin new transaction with SerializableRW mode
        tech.ydb.table.transaction.TxControl<?> txControl = tech.ydb.table.transaction.TxControl.serializableRw().setCommitTx(true);

        // Type of parameter values should be exactly the same as in DECLARE statements.
        Params params = Params.of(
                "$seriesId", PrimitiveValue.newUint64(1),
                "$seasonId", PrimitiveValue.newUint64(2)
        );

        DataQueryResult result = retryCtx.supplyResult(session -> session.executeDataQuery(query, txControl, params))
                .join().getValue();

        ResultSetReader rs = result.getResultSet(0);

        Assert.assertTrue(rs.next());
        Assert.assertEquals("IT Crowd", rs.getColumn("series_title").getText());
        Assert.assertEquals("Season 2", rs.getColumn("season_title").getText());

        Assert.assertFalse(rs.next());
    }

    @Test
    public void step07_scanQueryWithParams() {
        String query
                = "DECLARE $seriesId AS Uint64; "
                + "DECLARE $seasonId AS Uint64; "
                + "SELECT ep.title AS episode_title, sa.title AS season_title, sr.title AS series_title "
                + "FROM episodes AS ep "
                + "JOIN seasons AS sa ON sa.season_id = ep.season_id "
                + "JOIN series AS sr ON sr.series_id = sa.series_id "
                + "WHERE sa.series_id = $seriesId AND sa.season_id = $seasonId;";

        // Type of parameter values should be exactly the same as in DECLARE statements.
        Params params = Params.of(
                "$seriesId", PrimitiveValue.newUint64(2),
                "$seasonId", PrimitiveValue.newUint64(1)
        );

        retryCtx.supplyStatus(session -> {
            ExecuteScanQuerySettings settings = ExecuteScanQuerySettings.newBuilder().build();
            GrpcReadStream<ResultSetReader> scan = session.executeScanQuery(query, params, settings);
            return scan.start(rs -> {
                Assert.assertTrue(rs.next());
            });
        }).join().expectSuccess("scan query problem");
    }

    @Test
    public void step08_multiStepTransaction() {
        retryCtx.supplyStatus(session -> {
            TableTransaction transaction = session.createNewTransaction(TxMode.SERIALIZABLE_RW);
            String query1
                    = "DECLARE $seriesId AS Uint64; "
                    + "DECLARE $seasonId AS Uint64; "
                    + "SELECT MIN(first_aired) AS from_date FROM seasons "
                    + "WHERE series_id = $seriesId AND season_id = $seasonId;";

            // Execute first query to get the required values to the client.
            // Transaction control settings don't set CommitTx flag to keep transaction active
            // after query execution.
            DataQueryResult res1 = transaction.executeDataQuery(query1, Params.of(
                    "$seriesId", PrimitiveValue.newUint64(2),
                    "$seasonId", PrimitiveValue.newUint64(5)
            )).join().getValue();

            // Perform some client logic on returned values
            ResultSetReader resultSet = res1.getResultSet(0);
            if (!resultSet.next()) {
                throw new RuntimeException("not found first_aired");
            }
            LocalDate fromDate = resultSet.getColumn("from_date").getDate();
            LocalDate toDate = fromDate.plusDays(15);

            // Get active transaction id
            Assert.assertNotNull(transaction.getId());

            // Construct next query based on the results of client logic
            String query2
                    = "DECLARE $seriesId AS Uint64;"
                    + "DECLARE $fromDate AS Date;"
                    + "DECLARE $toDate AS Date;"
                    + "SELECT season_id, episode_id, title, air_date FROM episodes "
                    + "WHERE series_id = $seriesId AND air_date >= $fromDate AND air_date <= $toDate;";

            // Execute second query.
            // Transaction control settings continues active transaction (tx) and
            // commits it at the end of second query execution.
            DataQueryResult res2 = transaction.executeDataQueryAndCommit(query2, Params.of(
                "$seriesId", PrimitiveValue.newUint64(2),
                "$fromDate", PrimitiveValue.newDate(fromDate),
                "$toDate", PrimitiveValue.newDate(toDate)
            )).join().getValue();

            ResultSetReader rs = res2.getResultSet(0);
            Assert.assertTrue(rs.next());

            return CompletableFuture.completedFuture(Status.SUCCESS);
        }).join().expectSuccess("multistep transaction problem");
    }

    @Test
    public void step09_tclTransaction() {
        retryCtx.supplyStatus(session -> {
            // Create new transaction.
            // It is not active and has no id until any query is executed on it
            TableTransaction transaction = session.createNewTransaction(TxMode.SERIALIZABLE_RW);

            String query
                    = "DECLARE $airDate AS Date; "
                    + "UPDATE episodes SET air_date = $airDate WHERE title = \"TBD\";";

            Params params = Params.of("$airDate", PrimitiveValue.newDate(Instant.now()));

            // Execute data query on new transaction.
            // Transaction will be created on server and become active on client
            // Query will be executed on it, but transaction will not be committed
            DataQueryResult result = transaction.executeDataQuery(query, params)
                .join().getValue();

            Assert.assertNotNull(result.getTxId());

            // Commit active transaction (tx)
            return transaction.commit();
        }).join().expectSuccess("tcl transaction problem");
    }
}
