package tech.ydb.table.integration;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.Nonnull;

import tech.ydb.table.SessionRetryContext;
import tech.ydb.table.description.TableDescription;
import tech.ydb.table.impl.SimpleTableClient;
import tech.ydb.table.integration.data.ExtractablePrimaryKey;
import tech.ydb.table.integration.data.SeriesData;
import tech.ydb.table.integration.data.SeriesData.Season;
import tech.ydb.table.integration.data.SeriesData.Series;
import tech.ydb.table.integration.data.SeriesData.TablesData;
import tech.ydb.table.query.Params;
import tech.ydb.table.result.ResultSetReader;
import tech.ydb.table.rpc.grpc.GrpcTableRpc;
import tech.ydb.table.settings.BulkUpsertSettings;
import tech.ydb.table.settings.ExecuteDataQuerySettings;
import tech.ydb.table.settings.ReadRowsSettings;
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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TableQueryTest {
    @ClassRule
    public static final GrpcTransportRule YDB_TRANSPORT = new GrpcTransportRule();
    private static final SessionRetryContext CTX = SessionRetryContext.create(SimpleTableClient.newClient(
            GrpcTableRpc.useTransport(YDB_TRANSPORT)
    ).build()).build();
    private static final Duration DURATION_FAIL_CONNECTION = Duration.ofSeconds(5);
    private static final String TABLE_PREFIX = "query_";
    private static final Random RANDOM = new Random();
    private static final TableCustomizer TABLE_CUSTOMIZER = new TableCustomizer();


    @BeforeClass
    public static void prepare() {
        TABLE_CUSTOMIZER.createTables();
        TABLE_CUSTOMIZER.upsertTablesData();
    }

    @AfterClass
    public static void drop() {
        TABLE_CUSTOMIZER.dropTables();
    }

    @Test
//    @Ignore
    public void testReadRowsRandom() {
        for (int i = 0; i < 20; i++) {
            final TablesData table = TablesData.values()[RANDOM.nextInt(2)];
            final List<String> columns =
                    table.getColumns().subList(0, 1 + RANDOM.nextInt(table.getColumns().size() - 1));
            final int sizeOfData = table.getSampleData().size();
            final List<? extends ExtractablePrimaryKey<Long>> keys = table.getSampleData()
                    .subList(RANDOM.nextInt(sizeOfData / 2),
                            (sizeOfData / 2) + RANDOM.nextInt(sizeOfData - sizeOfData / 2));
            System.out.println(collectResultSetReader(select(columns, table.getName(), keys)));
            System.out.println(collectResultSetReader(readRows(columns, table.getName(), keys)));
            assertTrue(isEqualResultSetReaders(
                    select(columns, table.getName(), keys),
                    readRows(columns, table.getName(), keys)
            ));
        }
    }

    @Test
//    @Ignore
    public void testReadRowsSimple() {
        final int numberOfSeries = 3;
        List<SeriesData.Series> series = Stream.generate(() ->
                        SeriesData.SERIES.get(RANDOM.nextInt(SeriesData.SERIES.size())))
                .limit(numberOfSeries)
                .collect(Collectors.toList());
        final ResultSetReader rsr = readRows(null, "series", series);

        for (int i = 0; i < numberOfSeries; i++) {
            assertTrue(rsr.next());
            final Series oneSeries = new Series(
                    rsr.getColumn("series_id").getUint64(),
                    rsr.getColumn("title").getText(),
                    rsr.getColumn("series_info").getText()
            );
            Assert.assertTrue(series.contains(oneSeries));
        }
        assertFalse(rsr.next());
    }

    @Test
//    @Ignore
    public void testReadRowsComplexKey() {
        final int numberOfSeasons = 3;
        List<SeriesData.Season> seasons = Stream.generate(() ->
                SeriesData.SEASONS.get(RANDOM.nextInt(SeriesData.SEASONS.size())))
                .limit(numberOfSeasons)
                .collect(Collectors.toList());

        final ResultSetReader rsr = readRows(null, "seasons", seasons);

        for (int i = 0; i < numberOfSeasons; i++) {
            assertTrue(rsr.next());
            final Season season = new Season(
                    rsr.getColumn("series_id").getUint64(),
                    rsr.getColumn("season_id").getUint64(),
                    rsr.getColumn("title").getText()
            );
            Assert.assertTrue(seasons.contains(season));
        }
        assertFalse(rsr.next());
    }

    @Test
//    @Ignore
    public void testReadRowsFail() {
        assertThrows("Empty list of keys",
                java.util.concurrent.CompletionException.class, () -> CTX.supplyResult(session -> session.readRows(
                        getPath("seasons"), Collections.emptyList(),
                        new ReadRowsSettings(null, DURATION_FAIL_CONNECTION))).join().getValue());
    }

    @Nonnull
    private static String getPath(String tablePostfix) {
        return YDB_TRANSPORT.getDatabase() + "/" + TABLE_PREFIX + tablePostfix;
    }

    @Nonnull
    private String getTableName(String tablePostfix) {
        return TABLE_PREFIX + tablePostfix;
    }

    private ResultSetReader select(List<String> columns, String table,
                                   @Nonnull List<? extends ExtractablePrimaryKey<Long>> listOfKeys) {
        final String readyKeys = listOfKeys.stream().map(ExtractablePrimaryKey::getPrimaryKey)
                .map(keys -> keys.stream().collect(StringBuilder::new,
                        (acc, val) -> acc.append(acc.length() == 0 ? "" : " and ").append(val.getFirst()).append("=")
                                .append(Long.toUnsignedString(val.getSecond())),
                        (l, r) -> l.append(l.length() == 0 ? "" : " and ").append(r)).toString())
                .map(StringBuilder::new)
                .reduce((l, r) -> surroundWithBrackets(l).append(" or ").append(surroundWithBrackets(r))).get()
                .toString();

        return CTX.supplyResult(session -> session.executeDataQuery(
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
        return CTX.supplyResult(session ->
                session.readRows(getPath(table),
                        structuredKeys,
                        new ReadRowsSettings(columnNames, DURATION_FAIL_CONNECTION))).join().getValue()
                .getResultSetReader();
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

    private static class TableCustomizer {
        private void createTables() {
            TableDescription seriesTable = TableDescription.newBuilder()
                    .addNonnullColumn("series_id", PrimitiveType.Uint64)
                    .addNullableColumn("title", PrimitiveType.Text)
                    .addNullableColumn("series_info", PrimitiveType.Text)
                    .setPrimaryKey("series_id")
                    .build();

            CTX.supplyStatus(session -> session.createTable(getPath("series"), seriesTable))
                    .join().expectSuccess("Can't create table " + getPath("series"));

            TableDescription seasonsTable = TableDescription.newBuilder()
                    .addNonnullColumn("series_id", PrimitiveType.Uint64)
                    .addNonnullColumn("season_id", PrimitiveType.Uint64)
                    .addNullableColumn("title", PrimitiveType.Text)
                    .setPrimaryKeys("series_id", "season_id")
                    .build();

            CTX.supplyStatus(session -> session.createTable(getPath("seasons"), seasonsTable))
                    .join().expectSuccess("Can't create table " + getPath("seasons"));
        }

        private void upsertTablesData() {
            StructType seriesType = StructType.of(
                    "series_id", PrimitiveType.Uint64,
                    "title", PrimitiveType.Text,
                    "series_info", PrimitiveType.Text
            );
            ListValue seriesData = ListType.of(seriesType).newValue(
                    SeriesData.SERIES.stream().map(series -> seriesType.newValue(
                            "series_id", PrimitiveValue.newUint64(series.seriesID()),
                            "title", PrimitiveValue.newText(series.title()),
                            "series_info", PrimitiveValue.newText(series.seriesInfo())
                    )).collect(Collectors.toList())
            );
            CTX.supplyStatus(session -> session.executeBulkUpsert(
                    getPath("series"), seriesData, new BulkUpsertSettings()
            )).join().expectSuccess("bulk upsert problem");

            StructType seasonType = StructType.of(
                    "series_id", PrimitiveType.Uint64,
                    "season_id", PrimitiveType.Uint64,
                    "title", PrimitiveType.Text
            );
            ListValue seasonsData = ListType.of(seasonType).newValue(
                    SeriesData.SEASONS.stream().map(season -> seasonType.newValue(
                            "series_id", PrimitiveValue.newUint64(season.seriesID()),
                            "season_id", PrimitiveValue.newUint64(season.seasonID()),
                            "title", PrimitiveValue.newText(season.title())
                    )).collect(Collectors.toList())
            );
            CTX.supplyStatus(session -> session.executeBulkUpsert(
                    getPath("seasons"), seasonsData, new BulkUpsertSettings()
            )).join().expectSuccess("bulk upsert problem");
        }

        private void dropTables() {
            CTX.supplyStatus(session -> session.dropTable(getPath("seasons")))
                    .join().expectSuccess("drop table /seasons problem");
            CTX.supplyStatus(session -> session.dropTable(getPath("series")))
                    .join().expectSuccess("drop table /series problem");
        }
    }
}
