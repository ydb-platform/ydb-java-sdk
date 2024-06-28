package tech.ydb.table.integration;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import com.google.common.hash.Hashing;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;

import tech.ydb.table.SessionRetryContext;
import tech.ydb.table.description.TableDescription;
import tech.ydb.table.impl.ImplicitSession;
import tech.ydb.table.impl.SimpleTableClient;
import tech.ydb.table.query.Params;
import tech.ydb.table.result.ResultSetReader;
import tech.ydb.table.rpc.grpc.GrpcTableRpc;
import tech.ydb.table.settings.ExecuteScanQuerySettings;
import tech.ydb.table.settings.ReadRowsSettings;
import tech.ydb.table.settings.ReadTableSettings;
import tech.ydb.table.values.ListType;
import tech.ydb.table.values.ListValue;
import tech.ydb.table.values.PrimitiveType;
import tech.ydb.table.values.PrimitiveValue;
import tech.ydb.table.values.StructType;
import tech.ydb.table.values.StructValue;
import tech.ydb.table.values.TupleValue;
import tech.ydb.table.values.VoidValue;
import tech.ydb.test.junit4.GrpcTransportRule;

/**
 *
 * @author Aleksandr Gorshenin
 */
public class ImplicitSessionTest {
    @ClassRule
    public final static GrpcTransportRule YDB = new GrpcTransportRule();

    private final static String TABLE_NAME = "implicit_session_test";
    private final static Instant INSTANT = Instant.ofEpochMilli(1585932011123l); // Friday, April 3, 2020 4:40:11.123 PM
    private final static StructType TYPE = StructType.of(
            "id1", PrimitiveType.Text,
            "id2", PrimitiveType.Uint64,
            "payload", PrimitiveType.Bytes,
            "created", PrimitiveType.Timestamp
    );

    private final SimpleTableClient tableClient = SimpleTableClient.newClient(GrpcTableRpc.useTransport(YDB)).build();

    private final SessionRetryContext implicitCtx = SessionRetryContext.create(ImplicitSession.of(YDB)).build();

    @Before
    public void createTable() {
        String tablePath = YDB.getDatabase() + "/" + TABLE_NAME;

        TableDescription tableDescription = TableDescription.newBuilder()
                .addNonnullColumn("id1", PrimitiveType.Text)
                .addNonnullColumn("id2", PrimitiveType.Uint64)
                .addNullableColumn("payload", PrimitiveType.Bytes)
                .addNullableColumn("created", PrimitiveType.Timestamp)
                .setPrimaryKeys("id1", "id2")
                .build();

        SessionRetryContext.create(tableClient).build()
                .supplyStatus(session -> session.createTable(tablePath, tableDescription))
                .join().expectSuccess("create table error");
    }

    @After
    public void dropTable() {
        String tablePath = YDB.getDatabase() + "/" + TABLE_NAME;
        SessionRetryContext.create(tableClient).build()
                .supplyStatus(session -> session.dropTable(tablePath)).join().expectSuccess("drop table error");
    }

    private String hash(String text) {
        return Hashing.sha256().hashBytes(text.getBytes()).toString();
    }

    private byte[] createPayload(int idx, int type) {
        if (type == 0) {
            return null;
        }

        int v = (31 * idx) % type;
        byte[] bytes = new byte[v];
        for (int i = 0; i < bytes.length; i += 1) {
            v = 31 * v + 7;
            bytes[i] = (byte) v;
        }
        return bytes;
    }

    private ListValue generateBulk(int startInclusive, int endExclusive) {
        return ListType.of(TYPE).newValue(IntStream.range(startInclusive, endExclusive).mapToObj(idx -> {
            byte[] payload = createPayload(idx, idx % 7);
            return TYPE.newValue(
                "id1", PrimitiveValue.newText(hash("hashed_id_" + idx)),
                "id2", PrimitiveValue.newUint64(idx),
                "payload", payload == null ? VoidValue.of() : PrimitiveValue.newBytes(payload),
                "created", PrimitiveValue.newTimestamp(INSTANT.plusSeconds(idx))
            );
        }).collect(Collectors.toList()));
    }

    private StructValue keyStruct(int idx) {
        return StructValue.of(
                "id2", PrimitiveValue.newUint64(idx),
                "id1", PrimitiveValue.newText(hash("hashed_id_" + idx))
        );
    }

    private TupleValue keyTuple(int idx) {
        return TupleValue.of(
                PrimitiveValue.newText(hash("hashed_id_" + idx)).makeOptional(),
                PrimitiveValue.newUint64(idx).makeOptional()
        );
    }

    @Test
    public void implicitSessionTest() {
        String tablePath = YDB.getDatabase() + "/" + TABLE_NAME;

        // Create type for struct of series
        // Create and fill list of series
        ListValue bulk1 = generateBulk(0, 1000);
        ListValue bulk2 = generateBulk(1000, 2000);
        ListValue bulk3 = generateBulk(2000, 3000);

        // Bulk Upsert
        implicitCtx.supplyStatus(session -> session.executeBulkUpsert(tablePath, bulk1))
                .join().expectSuccess("Cannot execute bulk upsert");
        implicitCtx.supplyStatus(session -> session.executeBulkUpsert(tablePath, bulk2))
                .join().expectSuccess("Cannot execute bulk upsert");
        implicitCtx.supplyStatus(session -> session.executeBulkUpsert(tablePath, bulk3))
                .join().expectSuccess("Cannot execute bulk upsert");

        // Read Rows
        ResultSetReader readedRows = implicitCtx.supplyResult(
                session -> session.readRows(tablePath, ReadRowsSettings.newBuilder()
                        .addKeys(keyStruct(546), keyStruct(1123), keyStruct(2299))
                        .addColumns("id2", "payload")
                        .build())
        ).join().getValue().getResultSetReader();

        Assert.assertTrue(readedRows.next());
        Assert.assertEquals(546, readedRows.getColumn("id2").getUint64());
        // TODO: check readRows method with nullable results
        // Assert.assertFalse(readedRows.getColumn("payload").isOptionalItemPresent());

        Assert.assertTrue(readedRows.next());
        Assert.assertEquals(1123, readedRows.getColumn("id2").getUint64());
        Assert.assertArrayEquals(createPayload(1123, 1123 % 7), readedRows.getColumn("payload").getBytes());

        Assert.assertTrue(readedRows.next());
        Assert.assertEquals(2299, readedRows.getColumn("id2").getUint64());
        Assert.assertArrayEquals(createPayload(2299, 2299 % 7), readedRows.getColumn("payload").getBytes());

        // Read Table
        final List<Long> readedTable = new ArrayList<>();
        implicitCtx.supplyStatus(session -> {
            readedTable.clear();
            ReadTableSettings settings = ReadTableSettings.newBuilder()
                    .batchLimitRows(100)
                    .fromKeyInclusive(keyTuple(234))
                    .toKeyExclusive(keyTuple(2300))
                    .orderedRead(true)
                    .columns("id1", "id2", "created")
                    .build();
            return session.executeReadTable(tablePath, settings).start(part -> {
                ResultSetReader rs = part.getResultSetReader();
                while (rs.next()) {
                    long idx = rs.getColumn("id2").getUint64();
                    readedTable.add(idx);
                    Assert.assertEquals(INSTANT.plusSeconds(idx), rs.getColumn("created").getTimestamp());
                }
            });
        }).join().expectSuccess("cannot read table");

        Assert.assertFalse(readedTable.isEmpty());
        Assert.assertEquals(Long.valueOf(234l), readedTable.get(0));

        // Scan Query
        final List<Integer> scanedTable = new ArrayList<>();
        implicitCtx.supplyStatus(session -> {
            scanedTable.clear();
            return session.executeScanQuery(
                    "DECLARE $p1 AS UInt64;\n" +
                    "DECLARE $p2 AS UInt64;\n" +
                    "SELECT id1, id2, payload FROM " + TABLE_NAME + " WHERE id2 >= $p1 AND id2 < $p2 ORDER BY id2",
                    Params.of("$p1", PrimitiveValue.newUint64(234), "$p2", PrimitiveValue.newUint64(2300)),
                    ExecuteScanQuerySettings.newBuilder().build()
            ).start(rs -> {
                while (rs.next()) {
                    int idx = (int) rs.getColumn("id2").getUint64();
                    scanedTable.add(idx);

                    byte[] expected = createPayload(idx, idx % 7);
                    if (expected != null) {
                        Assert.assertArrayEquals(expected, rs.getColumn("payload").getBytes());
                    } else {
                        Assert.assertFalse(rs.getColumn("payload").isOptionalItemPresent());
                    }
                }
            });
        }).join().expectSuccess("cannot scan table");

        Assert.assertEquals(2066, scanedTable.size());
        Assert.assertEquals(Integer.valueOf(234), scanedTable.get(0));
        Assert.assertEquals(Integer.valueOf(2299), scanedTable.get(2065));
    }
}
