package tech.ydb.table.integration;

import java.lang.reflect.Method;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;

import tech.ydb.table.SessionRetryContext;
import tech.ydb.table.description.TableDescription;
import tech.ydb.table.impl.SimpleTableClient;
import tech.ydb.table.integration.data.ExtractablePrimaryKey;
import tech.ydb.table.integration.data.SeriesData;
import tech.ydb.table.integration.data.SeriesData.TablesData;
import tech.ydb.table.query.Params;
import tech.ydb.table.result.ResultSetReader;
import tech.ydb.table.rpc.grpc.GrpcTableRpc;
import tech.ydb.table.settings.BulkUpsertSettings;
import tech.ydb.table.settings.ExecuteDataQuerySettings;
import tech.ydb.table.transaction.TxControl;
import tech.ydb.table.utils.Pair;
import tech.ydb.table.values.ListType;
import tech.ydb.table.values.ListValue;
import tech.ydb.table.values.OptionalType;
import tech.ydb.table.values.OptionalValue;
import tech.ydb.table.values.PrimitiveType;
import tech.ydb.table.values.PrimitiveValue;
import tech.ydb.table.values.StructType;
import tech.ydb.table.values.StructValue;
import tech.ydb.table.values.Value;
import tech.ydb.test.junit4.GrpcTransportRule;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import org.junit.ClassRule;
import org.junit.Test;

public class TableQueryTest {
    @ClassRule
    public static final GrpcTransportRule YDB_TRANSPORT = new GrpcTransportRule();
    private static final Duration DURATION_FAIL_CONNECTION = Duration.ofSeconds(5);
    private static final String TABLE_PREFIX = "query_";
    private final SimpleTableClient tableClient = SimpleTableClient.newClient(
            GrpcTableRpc.useTransport(YDB_TRANSPORT)
    ).build();
    private final SessionRetryContext ctx = SessionRetryContext.create(tableClient).build();
    private final String database = YDB_TRANSPORT.getDatabase();
    private final Random random = new Random();
    private final TableCustomizer tableCustomizer = new TableCustomizer();

    @Nonnull
    private String getPath(String tablePostfix) {
        return database + "/" + TABLE_PREFIX + tablePostfix;
    }

    @Nonnull
    private String getTableName(String tablePostfix) {
        return TABLE_PREFIX + tablePostfix;
    }

    @Test
    public void testReadRowsRandom() {
        tableCustomizer.prepare();
        for (int i = 0; i < 20; i++) {
            final TablesData table = TablesData.values()[random.nextInt(3)];
            final List<String> columns =
                    table.getColumns().subList(0, 1 + random.nextInt(table.getColumns().size() - 1));
            final int sizeOfData = table.getSampleData().size();
            final List<? extends ExtractablePrimaryKey<Long>> keys = table.getSampleData()
                    .subList(random.nextInt(sizeOfData / 2),
                            (sizeOfData / 2) + random.nextInt(sizeOfData - sizeOfData / 2));

            System.out.println("select: " + collectResultSetReader(select(columns, table.getName(), keys)));
            System.out.println("readRows: " + collectResultSetReader(readRows(columns, table.getName(), keys)));
            assertTrue(isEqualResultSetReaders(
                    select(columns, table.getName(), keys),
                    readRows(columns, table.getName(), keys)
            ));
        }
        tableCustomizer.cleanAfter();
    }

    @Test
    public void testReadRowsSimple() {
        tableCustomizer.prepare();
        ResultSetReader rsr = ctx.supplyResult(session -> session.readRows(
                getPath("episodes"),
                Arrays.asList(
                        StructValue.of("series_id", PrimitiveValue.newUint64(1),
                                "season_id", PrimitiveValue.newUint64(2),
                                "episode_id", PrimitiveValue.newUint64(3)),
                        StructValue.of("series_id", PrimitiveValue.newUint64(2),
                                "season_id", PrimitiveValue.newUint64(4),
                                "episode_id", PrimitiveValue.newUint64(3))
                ),
                null,
                DURATION_FAIL_CONNECTION)).join().getValue();
        assertTrue(rsr.next());
        assertEquals(rsr.getColumnCount(), 5);
        assertEquals(rsr.getColumn("series_id").getUint64(), 1L);
        assertEquals(rsr.getColumn("season_id").getUint64(), 2L);
        assertEquals(rsr.getColumn("episode_id").getUint64(), 3L);
        assertEquals(rsr.getColumn("title").getText(), "Moss and the German");
        assertEquals(rsr.getColumn("air_date").getDate(),
                SeriesData.date("2007-09-07").atZone(OffsetDateTime.now().getOffset()).toLocalDate());
        assertTrue(rsr.next());
        assertEquals(rsr.getColumn("series_id").getUint64(), 2L);
        assertEquals(rsr.getColumn("season_id").getUint64(), 4L);
        assertEquals(rsr.getColumn("episode_id").getUint64(), 3L);
        assertEquals(rsr.getColumn("title").getText(), "Intellectual Property");
        assertEquals(rsr.getColumn("air_date").getDate(),
                SeriesData.date("2017-05-07").atZone(OffsetDateTime.now().getOffset()).toLocalDate());
        assertFalse(rsr.next());
        tableCustomizer.cleanAfter();
    }

    @Test
    public void testReadRowsFail() {
        tableCustomizer.prepare();
        assertThrows("Empty list of keys",
                java.util.concurrent.CompletionException.class, () -> ctx.supplyResult(session -> session.readRows(
                        getPath("episodes"), Collections.emptyList(),
                        null,
                        DURATION_FAIL_CONNECTION)).join().getValue());
        tableCustomizer.cleanAfter();
    }

    private ResultSetReader select(List<String> columns, String table,
                                   @Nonnull List<? extends ExtractablePrimaryKey<Long>> listOfKeys) {
        final String readyKeys = listOfKeys.stream().map(ExtractablePrimaryKey::getPrimaryKey)
                .map(keys -> keys.stream().collect(StringBuilder::new,
                        (acc, val) -> acc.append(acc.length() == 0 ? "" : " and ").append(val.getFirst()).append("=")
                                .append(val.getSecond()),
                        (l, r) -> l.append(l.length() == 0 ? "" : " and ").append(r)).toString())
                .map(StringBuilder::new)
                .reduce((l, r) -> surroundWithBrackets(l).append(" or ").append(surroundWithBrackets(r))).get()
                .toString();

        return ctx.supplyResult(session -> session.executeDataQuery(
                "select " + columns.stream().reduce((l, r) -> l + ", " + r).get() + " from " + getTableName(table) +
                        " where " +
                        readyKeys,
                TxControl.serializableRw(),
                Params.empty(),
                new ExecuteDataQuerySettings().setTimeout(DURATION_FAIL_CONNECTION)
        )).join().getValue().getResultSet(0);
    }

    @Nonnull
    private StringBuilder surroundWithBrackets(StringBuilder sb) {
        return new StringBuilder("(").append(sb).append(')');
    }

    @Nonnull
    private ResultSetReader readRows(List<String> columnNames, String table,
                                     List<? extends ExtractablePrimaryKey<Long>> listOfKeys) {
        final List<StructValue> structuredKeys = listOfKeys.stream().map(ExtractablePrimaryKey::getPrimaryKey)
                .map(listOfPairs -> {
                    Map<String, Value<?>> map = new HashMap<>();
                    for (Pair<String, Long> pair : listOfPairs) {
                        map.put(pair.getFirst(), PrimitiveValue.newUint64(pair.getSecond()));
                    }
                    return StructValue.of(map);
                }).collect(Collectors.toList());
        return ctx.supplyResult(session ->
                session.readRows(getPath(table),
                        structuredKeys,
                        columnNames,
                        DURATION_FAIL_CONNECTION)).join().getValue();
    }

    private boolean isEqualResultSetReaders(ResultSetReader left, ResultSetReader right) {
        return collectResultSetReader(left).equals(collectResultSetReader(right));
    }

    @Nonnull
    private HashMap<List<Value<?>>, Integer> collectResultSetReader(ResultSetReader rsr) {
        HashMap<List<Value<?>>, Integer> map = new HashMap<>();
        while (rsr.next()) {
            List<Value<?>> values = new ArrayList<>();
            for (int i = rsr.getColumnCount() - 1; i >= 0; i--) {
                if (rsr.getColumn(i).getType() instanceof OptionalType) {
                    values.add(((OptionalValue) rsr.getColumn(i).getValue()).get());
                } else {
                    values.add(rsr.getColumn(i).getValue());
                }
            }
            map.compute(values, (k, v) -> v == null ? 0 : ++v);
        }
        return map;
    }

    private class TableCustomizer {
        private final AtomicInteger preparationState = new AtomicInteger(-1);
        private final AtomicInteger droppingState;

        TableCustomizer() {
            int countMethods = 0;
            for (Method m : TableQueryTest.class.getMethods()) {
                if (m.isAnnotationPresent(Test.class)) {
                    countMethods++;
                }
            }
            this.droppingState = new AtomicInteger(countMethods);
        }

        public void prepare() {
            if (preparationState.get() != 1) {
                if (preparationState.get() == 0) {
                    while (preparationState.get() != 1) {
                        preparationState.get(); // for avoiding optimization by compiler
                    }
                } else {
                    createTables();
                    upsertTablesData();
                    preparationState.set(1);
                }
            }
        }

        public void cleanAfter() {
            droppingState.decrementAndGet();
            if (droppingState.get() == 0) {
                dropTables();
            }
        }

        private void createTables() {
            TableDescription seriesTable = TableDescription.newBuilder()
                    .addNonnullColumn("series_id", PrimitiveType.Uint64)
                    .addNullableColumn("title", PrimitiveType.Text)
                    .addNullableColumn("series_info", PrimitiveType.Text)
                    .addNullableColumn("release_date", PrimitiveType.Date)
                    .setPrimaryKey("series_id")
                    .build();

            ctx.supplyStatus(session -> session.createTable(getPath("series"), seriesTable))
                    .join().expectSuccess("Can't create table " + getPath("series"));

            TableDescription seasonsTable = TableDescription.newBuilder()
                    .addNonnullColumn("series_id", PrimitiveType.Uint64)
                    .addNonnullColumn("season_id", PrimitiveType.Uint64)
                    .addNullableColumn("title", PrimitiveType.Text)
                    .addNullableColumn("first_aired", PrimitiveType.Date)
                    .addNullableColumn("last_aired", PrimitiveType.Date)
                    .setPrimaryKeys("series_id", "season_id")
                    .build();

            ctx.supplyStatus(session -> session.createTable(getPath("seasons"), seasonsTable))
                    .join().expectSuccess("Can't create table " + getPath("seasons"));

            TableDescription episodesTable = TableDescription.newBuilder()
                    .addNonnullColumn("series_id", PrimitiveType.Uint64)
                    .addNonnullColumn("season_id", PrimitiveType.Uint64)
                    .addNonnullColumn("episode_id", PrimitiveType.Uint64)
                    .addNullableColumn("title", PrimitiveType.Text)
                    .addNullableColumn("air_date", PrimitiveType.Date)
                    .setPrimaryKeys("series_id", "season_id", "episode_id")
                    .build();

            ctx.supplyStatus(session -> session.createTable(getPath("episodes"), episodesTable))
                    .join().expectSuccess("Can't create table " + getPath("episodes"));
        }

        private void upsertTablesData() {
            // Create type for struct of series
            StructType seriesType = StructType.of(
                    "series_id", PrimitiveType.Uint64,
                    "title", PrimitiveType.Text,
                    "release_date", PrimitiveType.Date,
                    "series_info", PrimitiveType.Text
            );
            // Create and fill list of series
            ListValue seriesData = ListType.of(seriesType).newValue(
                    SeriesData.SERIES.stream().map(series -> seriesType.newValue(
                            "series_id", PrimitiveValue.newUint64(series.seriesID()),
                            "title", PrimitiveValue.newText(series.title()),
                            "release_date", PrimitiveValue.newDate(series.releaseDate()),
                            "series_info", PrimitiveValue.newText(series.seriesInfo())
                    )).collect(Collectors.toList())
            );
            // Upsert list of series to table
            ctx.supplyStatus(session -> session.executeBulkUpsert(
                    getPath("series"), seriesData, new BulkUpsertSettings()
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
                    SeriesData.SEASONS.stream().map(season -> seasonType.newValue(
                            "series_id", PrimitiveValue.newUint64(season.seriesID()),
                            "season_id", PrimitiveValue.newUint64(season.seasonID()),
                            "title", PrimitiveValue.newText(season.title()),
                            "first_aired", PrimitiveValue.newDate(season.firstAired()),
                            "last_aired", PrimitiveValue.newDate(season.lastAired())
                    )).collect(Collectors.toList())
            );
            // Upsert list of series to seasons
            ctx.supplyStatus(session -> session.executeBulkUpsert(
                    getPath("seasons"), seasonsData, new BulkUpsertSettings()
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
                    SeriesData.EPISODES.stream().map(episode -> episodeType.newValue(
                            "series_id", PrimitiveValue.newUint64(episode.seriesID()),
                            "season_id", PrimitiveValue.newUint64(episode.seasonID()),
                            "episode_id", PrimitiveValue.newUint64(episode.episodeID()),
                            "title", PrimitiveValue.newText(episode.title()),
                            "air_date", PrimitiveValue.newDate(episode.airDate())
                    )).collect(Collectors.toList())
            );

            // Upsert list of series to episodes
            ctx.supplyStatus(session -> session.executeBulkUpsert(
                    getPath("episodes"), episodesData, new BulkUpsertSettings()
            )).join().expectSuccess("bulk upsert problem");
        }

        private void dropTables() {
            ctx.supplyStatus(session -> session.dropTable(getPath("episodes")))
                    .join().expectSuccess("drop table /episodes problem");
            ctx.supplyStatus(session -> session.dropTable(getPath("seasons")))
                    .join().expectSuccess("drop table /seasons problem");
            ctx.supplyStatus(session -> session.dropTable(getPath("series")))
                    .join().expectSuccess("drop table /series problem");
        }
    }
}
