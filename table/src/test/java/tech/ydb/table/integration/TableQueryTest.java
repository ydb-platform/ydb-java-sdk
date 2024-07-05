package tech.ydb.table.integration;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.annotation.Nonnull;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import tech.ydb.core.StatusCode;
import tech.ydb.table.SessionRetryContext;
import tech.ydb.table.description.TableDescription;
import tech.ydb.table.impl.SimpleTableClient;
import tech.ydb.table.result.ResultSetReader;
import tech.ydb.table.rpc.grpc.GrpcTableRpc;
import tech.ydb.table.settings.BulkUpsertSettings;
import tech.ydb.table.settings.ReadRowsSettings;
import tech.ydb.table.values.ListType;
import tech.ydb.table.values.PrimitiveType;
import tech.ydb.table.values.PrimitiveValue;
import tech.ydb.table.values.StructValue;
import tech.ydb.test.junit4.GrpcTransportRule;


public class TableQueryTest {
    @ClassRule
    public static final GrpcTransportRule YDB_TRANSPORT = new GrpcTransportRule();
    private static final SessionRetryContext CTX = SessionRetryContext.create(SimpleTableClient.newClient(
            GrpcTableRpc.useTransport(YDB_TRANSPORT)
    ).build()).build();
    private static final String TABLE_PREFIX = "query_";


    /**
     * Create tables `series` and `seasons` and fill them with data
     */
    @BeforeClass
    public static void prepare() {
        final List<StructValue> series = Arrays.asList(
                StructValue.of(
                        "series_id", PrimitiveValue.newUint64(1),
                        "title", PrimitiveValue.newText("Once I rose above the noise and confusion"),
                        "series_info", PrimitiveValue.newText("Carry on my wayward son")
                ), StructValue.of("series_id", PrimitiveValue.newUint64(2),
                        "title", PrimitiveValue.newText("There'll be peace when you are done"),
                        "series_info", PrimitiveValue.newText("Lay your weary head to rest")
                ), StructValue.of("series_id", PrimitiveValue.newUint64(3),
                        "title", PrimitiveValue.newText("Just to get a glimpse beyond this illusion"),
                        "series_info", PrimitiveValue.newText("I was soaring ever higher")
                )
        );
        final List<StructValue> seasons = Arrays.asList(
                StructValue.of(
                        "series_id", PrimitiveValue.newUint64(1),
                        "season_id", PrimitiveValue.newUint64(1),
                        "title", PrimitiveValue.newText("But I flew too high")
                ), StructValue.of(
                        "series_id", PrimitiveValue.newUint64(2),
                        "season_id", PrimitiveValue.newUint64(2),
                        "title", PrimitiveValue.newText("Though my eyes could see, I still was a blind man")
                ), StructValue.of(
                        "series_id", PrimitiveValue.newUint64(2),
                        "season_id", PrimitiveValue.newUint64(3),
                        "title", PrimitiveValue.newText("Though my mind could think, I still was a mad man")
                ), StructValue.of(
                        "series_id", PrimitiveValue.newUint64(2),
                        "season_id", PrimitiveValue.newUint64(4),
                        "title", PrimitiveValue.newText("I hear the voices when I'm dreaming")
                )
        );

        CTX.supplyStatus(session -> session.createTable(getPath("series"), TableDescription.newBuilder()
                                .addNonnullColumn("series_id", PrimitiveType.Uint64)
                                .addNullableColumn("title", PrimitiveType.Text)
                                .addNullableColumn("series_info", PrimitiveType.Text)
                                .setPrimaryKey("series_id")
                                .build()))
                .join().expectSuccess("Can't create table " + getPath("series"));

        CTX.supplyStatus(session -> session.createTable(getPath("seasons"), TableDescription.newBuilder()
                        .addNonnullColumn("series_id", PrimitiveType.Uint64)
                        .addNonnullColumn("season_id", PrimitiveType.Uint64)
                        .addNullableColumn("title", PrimitiveType.Text)
                        .setPrimaryKeys("series_id", "season_id")
                        .build()))
                .join().expectSuccess("Can't create table " + getPath("seasons"));

        CTX.supplyStatus(session -> session.executeBulkUpsert(getPath("series"),
                        ListType.of(series.get(0).getType()).newValue(series), new BulkUpsertSettings()))
                .join().expectSuccess("bulk upsert problem in table " + getPath("series"));

        CTX.supplyStatus(session -> session.executeBulkUpsert(getPath("seasons"),
                        ListType.of(seasons.get(0).getType()).newValue(seasons), new BulkUpsertSettings()))
                .join().expectSuccess("bulk upsert problem in table " + getPath("seasons"));
    }

    @Nonnull
    private static String getPath(String tablePostfix) {
        return YDB_TRANSPORT.getDatabase() + "/" + getTableName(tablePostfix);
    }

    @Nonnull
    private static String getTableName(String tablePostfix) {
        return TABLE_PREFIX + tablePostfix;
    }


    @Test
    public void testReadRowsComplexKey() {
        final List<StructValue> seasonKeys = Arrays.asList(
                StructValue.of(
                        "series_id", PrimitiveValue.newUint64(1),
                        "season_id", PrimitiveValue.newUint64(1)
                ), StructValue.of(
                        "series_id", PrimitiveValue.newUint64(2),
                        "season_id", PrimitiveValue.newUint64(3)
                )
        );

        final ResultSetReader rsr = CTX.supplyResult(session ->
                session.readRows(getPath("seasons"),
                        ReadRowsSettings.newBuilder()
                                .addColumns(Collections.emptyList())
                                .addKeys(seasonKeys)
                                .build()
                )
        ).join().getValue().getResultSetReader();

        Assert.assertTrue(rsr.next());
        Assert.assertEquals(1, rsr.getColumn("series_id").getUint64());
        Assert.assertEquals(1, rsr.getColumn("season_id").getUint64());
        Assert.assertEquals("But I flew too high", rsr.getColumn("title").getText());
        Assert.assertTrue(rsr.next());
        Assert.assertEquals(2, rsr.getColumn("series_id").getUint64());
        Assert.assertEquals(3, rsr.getColumn("season_id").getUint64());
        Assert.assertEquals("Though my mind could think, I still was a mad man", rsr.getColumn("title").getText());
        Assert.assertFalse(rsr.next());
    }

    /**
     * Empty keys parameter make result be an empty ResultSet
     */
    @Test
    public void testReadRowsEmptyKeys() {
        final StatusCode responseStatusCode = CTX.supplyResult(session ->
                session.readRows(getPath("series"),
                        ReadRowsSettings.newBuilder()
                                .addColumns("series_id", "title")
                                .build()
                )
        ).join().getStatus().getCode();
        Assert.assertEquals(StatusCode.BAD_REQUEST, responseStatusCode);
    }

    /**
     * Empty columns parameter is equivalent to list all columns
     */
    @Test
    public void testReadRowsEmptyColumns() {
        final ResultSetReader rsr = CTX.supplyResult(session ->
                session.readRows(getPath("series"),
                        ReadRowsSettings.newBuilder()
                                .addKey(StructValue.of("series_id", PrimitiveValue.newUint64(1)))
                                .addKey(StructValue.of("series_id", PrimitiveValue.newUint64(2)))
                                .build()
                )
        ).join().getValue().getResultSetReader();

        Assert.assertTrue(rsr.next());
        Assert.assertEquals(1, rsr.getColumn("series_id").getUint64());
        Assert.assertEquals("Once I rose above the noise and confusion", rsr.getColumn("title").getText());
        Assert.assertEquals("Carry on my wayward son", rsr.getColumn("series_info").getText());
        Assert.assertTrue(rsr.next());
        Assert.assertEquals(2, rsr.getColumn("series_id").getUint64());
        Assert.assertEquals("There'll be peace when you are done", rsr.getColumn("title").getText());
        Assert.assertEquals("Lay your weary head to rest", rsr.getColumn("series_info").getText());
        Assert.assertFalse(rsr.next());
    }

    @Test(expected = NullPointerException.class)
    public void testReadRowsNullColumns() {
        ReadRowsSettings.newBuilder().addKey(null).addKey(null).build();
    }

    @Test(expected = NullPointerException.class)
    public void testReadRowsNullKeys() {
        ReadRowsSettings.newBuilder().addKey(null).addKey(null).build();
    }
}
