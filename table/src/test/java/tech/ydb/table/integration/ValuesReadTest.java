package tech.ydb.table.integration;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.function.ThrowingRunnable;

import tech.ydb.core.Issue;
import tech.ydb.core.Status;
import tech.ydb.core.StatusCode;
import tech.ydb.table.SessionRetryContext;
import tech.ydb.table.impl.SimpleTableClient;
import tech.ydb.table.query.DataQueryResult;
import tech.ydb.table.query.Params;
import tech.ydb.table.result.ResultSetReader;
import tech.ydb.table.result.ValueReader;
import tech.ydb.table.rpc.grpc.GrpcTableRpc;
import tech.ydb.table.transaction.TxControl;
import tech.ydb.table.values.DecimalType;
import tech.ydb.table.values.DecimalValue;
import tech.ydb.table.values.ListValue;
import tech.ydb.table.values.NullType;
import tech.ydb.table.values.NullValue;
import tech.ydb.table.values.PrimitiveType;
import tech.ydb.table.values.PrimitiveValue;
import tech.ydb.table.values.StructValue;
import tech.ydb.table.values.Type;
import tech.ydb.table.values.Value;
import tech.ydb.test.junit4.GrpcTransportRule;

/**
 *
 * @author Aleksandr Gorshenin
 */
public class ValuesReadTest {
    @ClassRule
    public static final GrpcTransportRule YDB_TRANSPORT = new GrpcTransportRule();
    private static final SessionRetryContext CTX = SessionRetryContext.create(SimpleTableClient.newClient(
            GrpcTableRpc.useTransport(YDB_TRANSPORT)
    ).build()).build();

    @Test
    public void nullReadTest() {
        DataQueryResult result = CTX.supplyResult(
                s -> s.executeDataQuery("SELECT '1' AS p1, 123 AS p2, NULL AS p3", TxControl.serializableRw())
        ).join().getValue();

        Assert.assertEquals(1, result.getResultSetCount());

        ResultSetReader rs = result.getResultSet(0);
        Assert.assertTrue(rs.next());

        ValueReader p1 = rs.getColumn("p1");
        ValueReader p2 = rs.getColumn("p2");
        ValueReader p3 = rs.getColumn("p3");

        Assert.assertNotNull(p1);
        Assert.assertNotNull(p2);
        Assert.assertNotNull(p3);

        Assert.assertSame(PrimitiveType.Bytes, p1.getType());
        Assert.assertSame(PrimitiveType.Int32, p2.getType());
        Assert.assertSame(NullType.of(), p3.getType());

        Assert.assertArrayEquals(new byte[]{'1'}, p1.getBytes());
        Assert.assertEquals(123, p2.getInt32());
        Assert.assertSame(NullValue.of(), p3.getValue());
    }

    private void assertIllegalStateException(String message, ThrowingRunnable runnable) {
        IllegalStateException ex = Assert.assertThrows(IllegalStateException.class, runnable);
        Assert.assertEquals(message, ex.getMessage());
    }

    private void assertNullPointerException(String message, ThrowingRunnable runnable) {
        NullPointerException ex = Assert.assertThrows(NullPointerException.class, runnable);
        Assert.assertEquals(message, ex.getMessage());
    }

    @Test
    public void innerNullsTest() {
        DataQueryResult result = CTX.supplyResult(
                s -> s.executeDataQuery("SELECT "
                        + "123ul AS p1, Just(123ul) AS p2, Just(Just(123ul)) AS p3, "
                        + "Nothing(UInt64?) AS p4, Just(Nothing(UInt64?)) AS p5", TxControl.snapshotRo())
        ).join().getValue();

        Assert.assertEquals(1, result.getResultSetCount());

        ResultSetReader rs = result.getResultSet(0);
        Assert.assertTrue(rs.next());

        ValueReader p1 = rs.getColumn("p1");
        ValueReader p2 = rs.getColumn("p2");
        ValueReader p3 = rs.getColumn("p3");
        ValueReader p4 = rs.getColumn("p4");
        ValueReader p5 = rs.getColumn("p5");

        Assert.assertNotNull(p1);
        Assert.assertNotNull(p2);
        Assert.assertNotNull(p3);
        Assert.assertNotNull(p4);
        Assert.assertNotNull(p5);

        Assert.assertEquals(PrimitiveType.Uint64, p1.getType());
        Assert.assertEquals(PrimitiveType.Uint64.makeOptional(), p2.getType());
        Assert.assertEquals(PrimitiveType.Uint64.makeOptional().makeOptional(), p3.getType());
        Assert.assertEquals(PrimitiveType.Uint64.makeOptional(), p4.getType());
        Assert.assertEquals(PrimitiveType.Uint64.makeOptional().makeOptional(), p5.getType());

        assertIllegalStateException("cannot call isOptionalItemPresent, actual type: Uint64",
                p1::isOptionalItemPresent);
        Assert.assertTrue(p2.isOptionalItemPresent());
        Assert.assertTrue(p3.isOptionalItemPresent());
        Assert.assertFalse(p4.isOptionalItemPresent());

        // Inner NULL
        Assert.assertTrue(p5.isOptionalItemPresent());
        Assert.assertFalse(p5.getOptionalItem().isOptionalItemPresent());

        Assert.assertEquals(123l, p1.getUint64());
        Assert.assertEquals(123l, p2.getUint64());
        Assert.assertEquals(123l, p3.getUint64());
        assertNullPointerException("cannot call getUint64 for NULL value", p4::getUint64);
        assertNullPointerException("cannot call getUint64 for NULL value", p5::getUint64);
    }

    @Test
    public void tzDatesTest() {
        DataQueryResult result = CTX.supplyResult(s -> s.executeDataQuery("SELECT "
                + "AddTimezone(Timestamp('2026-10-29T04:23:45.987654Z'), 'Europe/Warsaw') as p1,"
                + "AddTimezone(Timestamp('2026-10-29T04:23:45.987654Z'), 'Canada/Pacific') as p2,"
                + "AddTimezone(Datetime('2021-10-10T01:23:45Z'), 'Europe/Lisbon') as p3,"
                + "AddTimezone(Datetime('2021-10-10T01:23:45Z'), 'America/Vancouver') as p4,"
                + "AddTimezone(Date('2005-01-01'), 'Asia/Macau') as p5,"
                + "AddTimezone(Date('2005-01-01'), 'Pacific/Niue') as p6"
                ,
                TxControl.snapshotRo()
        )).join().getValue();

        Assert.assertEquals(1, result.getResultSetCount());
        ResultSetReader rs = result.getResultSet(0);
        Assert.assertTrue(rs.next());

        ValueReader p1 = rs.getColumn("p1");
        ValueReader p2 = rs.getColumn("p2");
        ValueReader p3 = rs.getColumn("p3");
        ValueReader p4 = rs.getColumn("p4");
        ValueReader p5 = rs.getColumn("p5");
        ValueReader p6 = rs.getColumn("p6");

        Assert.assertNotNull(p1);
        Assert.assertNotNull(p2);
        Assert.assertNotNull(p3);
        Assert.assertNotNull(p4);
        Assert.assertNotNull(p5);
        Assert.assertNotNull(p6);

        Assert.assertSame(Type.Kind.OPTIONAL, p1.getType().getKind());
        Assert.assertSame(Type.Kind.OPTIONAL, p2.getType().getKind());
        Assert.assertSame(Type.Kind.OPTIONAL, p3.getType().getKind());
        Assert.assertSame(Type.Kind.OPTIONAL, p4.getType().getKind());
        Assert.assertSame(Type.Kind.OPTIONAL, p5.getType().getKind());
        Assert.assertSame(Type.Kind.OPTIONAL, p6.getType().getKind());

        Assert.assertSame(PrimitiveType.TzTimestamp, p1.getType().unwrapOptional());
        Assert.assertSame(PrimitiveType.TzTimestamp, p2.getType().unwrapOptional());
        Assert.assertSame(PrimitiveType.TzDatetime, p3.getType().unwrapOptional());
        Assert.assertSame(PrimitiveType.TzDatetime, p4.getType().unwrapOptional());
        Assert.assertSame(PrimitiveType.TzDate, p5.getType().unwrapOptional());
        Assert.assertSame(PrimitiveType.TzDate, p6.getType().unwrapOptional());

        Assert.assertEquals("2026-10-29T05:23:45.987654+01:00[Europe/Warsaw]", p1.getTzTimestamp().toString());
        Assert.assertEquals("2026-10-28T21:23:45.987654-07:00[Canada/Pacific]", p2.getTzTimestamp().toString());
        Assert.assertEquals("2021-10-10T02:23:45+01:00[Europe/Lisbon]", p3.getTzDatetime().toString());
        Assert.assertEquals("2021-10-09T18:23:45-07:00[America/Vancouver]", p4.getTzDatetime().toString());
        Assert.assertEquals("2005-01-02T00:00+08:00[Asia/Macau]", p5.getTzDate().toString());
        Assert.assertEquals("2005-01-01T00:00-11:00[Pacific/Niue]", p6.getTzDate().toString());
    }

    @Test
    @SuppressWarnings("deprecation")
    public void uuidReadTest() {
        DataQueryResult result = CTX.supplyResult(
                s -> s.executeDataQuery("SELECT "
                                + "CAST('123e4567-e89b-12d3-a456-426614174000' AS UUID) AS p1,"
                                + "CAST('2d9e498b-b746-9cfb-084d-de4e1cb4736e' AS UUID) AS p2;",
                        TxControl.serializableRw()
                )
        ).join().getValue();

        Assert.assertEquals(1, result.getResultSetCount());

        ResultSetReader rs = result.getResultSet(0);
        Assert.assertTrue(rs.next());

        ValueReader p1 = rs.getColumn("p1");
        Assert.assertNotNull(p1);

        Assert.assertSame(Type.Kind.OPTIONAL, p1.getType().getKind());
        Assert.assertSame(PrimitiveType.Uuid, p1.getType().unwrapOptional());

        Assert.assertEquals(UUID.fromString("123e4567-e89b-12d3-a456-426614174000"), p1.getUuid());

        PrimitiveValue v1 = p1.getValue().asOptional().get().asData();
        Assert.assertEquals("123e4567-e89b-12d3-a456-426614174000", v1.getUuidString());
        Assert.assertEquals(0x12d3e89b123e4567L, v1.getUuidLow());
        Assert.assertEquals(0x00401714664256a4L, v1.getUuidHigh());

        ValueReader p2 = rs.getColumn("p2");
        Assert.assertNotNull(p2);

        Assert.assertSame(Type.Kind.OPTIONAL, p2.getType().getKind());
        Assert.assertSame(PrimitiveType.Uuid, p2.getType().unwrapOptional());

        Assert.assertEquals(UUID.fromString("2d9e498b-b746-9cfb-084d-de4e1cb4736e"), p2.getUuid());

        PrimitiveValue v2 = p2.getValue().asOptional().get().asData();
        Assert.assertEquals("2d9e498b-b746-9cfb-084d-de4e1cb4736e", v2.getUuidString());
        Assert.assertEquals(0x9cfbb7462d9e498bL, v2.getUuidLow());
        Assert.assertEquals(0x6e73b41c4ede4d08L, v2.getUuidHigh());
    }

    @Test
    public void uuidSortTest() {
        String[] sorted = new String[] {
            "00000000-0000-0000-0000-000000000001",
            "00000000-0000-0000-0000-000000000010",
            "00000000-0000-0000-0000-000000000100",
            "00000000-0000-0000-0000-000000001000",
            "00000000-0000-0000-0000-000000010000",
            "00000000-0000-0000-0000-000000100000",
            "00000000-0000-0000-0000-000001000000",
            "00000000-0000-0000-0000-000010000000",
            "00000000-0000-0000-0000-000100000000",
            "00000000-0000-0000-0000-001000000000",
            "00000000-0000-0000-0000-010000000000",
            "00000000-0000-0000-0000-100000000000",

            "00000000-0000-0000-0001-000000000000",
            "00000000-0000-0000-0010-000000000000",
            "00000000-0000-0000-0100-000000000000",
            "00000000-0000-0000-1000-000000000000",

            "00000000-0000-0100-0000-000000000000",
            "00000000-0000-1000-0000-000000000000",
            "00000000-0000-0001-0000-000000000000",
            "00000000-0000-0010-0000-000000000000",

            "00000000-0100-0000-0000-000000000000",
            "00000000-1000-0000-0000-000000000000",
            "00000000-0001-0000-0000-000000000000",
            "00000000-0010-0000-0000-000000000000",

            "01000000-0000-0000-0000-000000000000",
            "10000000-0000-0000-0000-000000000000",
            "00010000-0000-0000-0000-000000000000",
            "00100000-0000-0000-0000-000000000000",
            "00000100-0000-0000-0000-000000000000",
            "00001000-0000-0000-0000-000000000000",
            "00000001-0000-0000-0000-000000000000",
            "00000010-0000-0000-0000-000000000000",
        };

        StructValue[] sv = new StructValue[sorted.length];
        for (int idx = 0; idx < sorted.length; idx++) {
            sv[idx] = StructValue.of("uuid", PrimitiveValue.newUuid(sorted[idx]));
        }
        ListValue list = ListValue.of(sv);

        DataQueryResult result = CTX.supplyResult(s -> s.executeDataQuery(""
                + "DECLARE $input AS List<Struct<uuid: UUID>>;"
                + "SELECT uuid FROM AS_TABLE($input) ORDER BY uuid ASC;"
                + "SELECT uuid FROM AS_TABLE($input) ORDER BY uuid DESC;",
                TxControl.snapshotRo(), Params.of("$input", list)
        )).join().getValue();

        Assert.assertEquals(2, result.getResultSetCount());
        ResultSetReader rs1 = result.getResultSet(0);
        ResultSetReader rs2 = result.getResultSet(1);

        Value<?> p1 = null;
        Value<?> p2 = null;
        for (int idx = 0; idx < sorted.length; idx++) {
            Assert.assertTrue(rs1.next());
            Assert.assertTrue(rs2.next());

            Assert.assertEquals(UUID.fromString(sorted[idx]), rs1.getColumn(0).getUuid());
            Assert.assertEquals(UUID.fromString(sorted[sorted.length - 1 - idx]), rs2.getColumn(0).getUuid());

            Value<?> v1 = rs1.getColumn(0).getValue();
            Value<?> v2 = rs2.getColumn(0).getValue();

            if (idx != 0) {
                Assert.assertTrue("" + v1 + " > " + p1, v1.compareTo(p1) > 0);
                Assert.assertTrue("" + v2 + " < " + p2, v2.compareTo(p2) < 0);
            }

            p1 = v1;
            p2 = v2;
        }

        Assert.assertFalse(rs1.next());
        Assert.assertFalse(rs2.next());
    }

    private void assertTimestamp(ValueReader vr, boolean optional, Instant expected) {
        Assert.assertNotNull(vr);
        if (optional) {
            Assert.assertSame(Type.Kind.OPTIONAL, vr.getType().getKind());
            Assert.assertSame(PrimitiveType.Timestamp, vr.getType().unwrapOptional());
        } else {
            Assert.assertSame(PrimitiveType.Timestamp, vr.getType());
        }

        Assert.assertEquals(expected, vr.getTimestamp());
    }

    @Test
    public void timestampReadTest() {
        DataQueryResult result = CTX.supplyResult(
                s -> s.executeDataQuery("SELECT "
                                + "DateTime::MakeTimestamp(DateTime::FromMilliseconds(0ul)) as t1,"
                                + "DateTime::MakeTimestamp(DateTime::FromMicroseconds(1000ul)) as t2,"
                                + "DateTime::MakeTimestamp(DateTime::FromMicroseconds(4291747199999999ul)) as t3,"
                                + "Timestamp('1970-01-01T00:00:00.000000Z') as t4,"
                                + "Timestamp('2105-12-31T23:59:59.999999Z') as t5;",
                        TxControl.serializableRw()
                )
        ).join().getValue();

        Assert.assertEquals(1, result.getResultSetCount());

        ResultSetReader rs = result.getResultSet(0);
        Assert.assertTrue(rs.next());

        assertTimestamp(rs.getColumn("t1"), true, Instant.EPOCH);
        assertTimestamp(rs.getColumn("t2"), true, Instant.EPOCH.plusMillis(1));
        assertTimestamp(rs.getColumn("t3"), true, Instant.parse("2105-12-31T23:59:59.999999Z"));
        assertTimestamp(rs.getColumn("t4"), false, Instant.ofEpochSecond(0, 0));
        assertTimestamp(rs.getColumn("t5"), false, Instant.ofEpochSecond(4291747199l, 999999000l));

        Status invalid = CTX.supplyResult(
                s -> s.executeDataQuery("SELECT "
                                + "Timestamp('1969-12-31T23:59:59.999999Z') as t6,"
                                + "Timestamp('2106-01-01T00:00:00.000000Z') as t7;",
                        TxControl.serializableRw()
                )
        ).join().getStatus();

        Assert.assertEquals(StatusCode.GENERIC_ERROR, invalid.getCode());
        Issue[] issues = invalid.getIssues();
        Assert.assertEquals(2, issues.length);
        Assert.assertEquals("Invalid value \"1969-12-31T23:59:59.999999Z\" for type Timestamp", issues[0].getMessage());
        Assert.assertEquals("Invalid value \"2106-01-01T00:00:00.000000Z\" for type Timestamp", issues[1].getMessage());

    }

    @Test
    public void decimalReadTest() {
        DataQueryResult result = CTX.supplyResult(
                s -> s.executeDataQuery("SELECT "
                                + "Decimal('9', 1, 0) AS d1, "
                                + "Decimal('-9', 1, 0) AS d2, "
                                + "Decimal('99999999999999999999999999999999999', 35, 0) AS d3, "
                                + "Decimal('-99999999999999999999999999999999999', 35, 0) AS d4, "
                                + "Decimal('9999999999999999999999999.9999999999', 35, 10) AS d5, "
                                + "Decimal('-9999999999999999999999999.9999999999', 35, 10) AS d6, "
                                + "Decimal('9.6', 1, 0) AS d7, "
                                + "Decimal('-9.6', 1, 0) AS d8, "
                                + "Decimal('99999999999999999999999999999999999.6', 35, 0) AS d9, "
                                + "Decimal('-99999999999999999999999999999999999.6', 35, 0) AS d10, "
                                + "Decimal('9999999999999999999999999.99999999996', 35, 10) AS d11, "
                                + "Decimal('-9999999999999999999999999.99999999996', 35, 10) AS d12;",
                        TxControl.serializableRw()
                )
        ).join().getValue();

        Assert.assertEquals(1, result.getResultSetCount());

        ResultSetReader rs = result.getResultSet(0);
        Assert.assertTrue(rs.next());

        DecimalValue d1 = rs.getColumn("d1").getDecimal();
        DecimalValue d2 = rs.getColumn("d2").getDecimal();
        DecimalValue d3 = rs.getColumn("d3").getDecimal();
        DecimalValue d4 = rs.getColumn("d4").getDecimal();
        DecimalValue d5 = rs.getColumn("d5").getDecimal();
        DecimalValue d6 = rs.getColumn("d6").getDecimal();
        DecimalValue d7 = rs.getColumn("d7").getDecimal();
        DecimalValue d8 = rs.getColumn("d8").getDecimal();
        DecimalValue d9 = rs.getColumn("d9").getDecimal();
        DecimalValue d10 = rs.getColumn("d10").getDecimal();
        DecimalValue d11 = rs.getColumn("d11").getDecimal();
        DecimalValue d12 = rs.getColumn("d12").getDecimal();

        Assert.assertEquals(DecimalType.of(1).newValue(9), d1);
        Assert.assertEquals(DecimalType.of(1).newValue(-9), d2);
        Assert.assertEquals(DecimalType.of(35).newValue("99999999999999999999999999999999999"), d3);
        Assert.assertEquals(DecimalType.of(35).newValue("-99999999999999999999999999999999999"), d4);
        Assert.assertEquals(DecimalType.of(35, 10).newValue("9999999999999999999999999.9999999999"), d5);
        Assert.assertEquals(DecimalType.of(35, 10).newValue("-9999999999999999999999999.9999999999"), d6);

        Assert.assertEquals(DecimalType.of(1).getInf(), d7);
        Assert.assertEquals(DecimalType.of(1).getNegInf(), d8);
        Assert.assertEquals(DecimalType.of(35).getInf(), d9);
        Assert.assertEquals(DecimalType.of(35).getNegInf(), d10);
        Assert.assertEquals(DecimalType.of(35, 10).getInf(), d11);
        Assert.assertEquals(DecimalType.of(35, 10).getNegInf(), d12);

        // All infinity values have the same high & low parts
        Assert.assertEquals(d7.getHigh(), d9.getHigh());
        Assert.assertEquals(d7.getHigh(), d11.getHigh());
        Assert.assertEquals(d7.getLow(), d9.getLow());
        Assert.assertEquals(d7.getLow(), d11.getLow());

        Assert.assertEquals(d8.getHigh(), d10.getHigh());
        Assert.assertEquals(d8.getHigh(), d12.getHigh());
        Assert.assertEquals(d8.getLow(), d10.getLow());
        Assert.assertEquals(d8.getLow(), d12.getLow());
    }

    @Test
    public void date32datetime64timestamp64interval64() {
        date32datetime64timestamp64interval64Assert(
                LocalDate.of(988, 2, 6),
                LocalDateTime.of(988, 2, 7, 12, 30, 0),
                Instant.parse("0998-06-02T12:30:00.678901Z"),
                Duration.parse("-PT2S")
        );

        date32datetime64timestamp64interval64Assert(
                LocalDate.now(),
                LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS),
                Instant.now().truncatedTo(ChronoUnit.MICROS),
                Duration.ZERO
        );

        DataQueryResult reader = CTX.supplyResult(
                s -> s.executeDataQuery("DECLARE $date32 AS Date32;\n" +
                        "DECLARE $datetime64 AS Datetime64;\n" +
                        "DECLARE $timestamp64 AS Timestamp64;\n" +
                        "DECLARE $interval64 AS Interval64;\n" +
                        "\n" +
                        "$date32=Date32('998-06-02'); \n" +
                        "$datetime64=Datetime64('0998-06-02T12:30:00Z');\n" +
                        "$timestamp64=Timestamp64('0998-06-02T12:30:00.678901Z');\n" +
                        "$interval64=Interval64('-PT2S');\n" +
                        "\n" +
                        "SELECT $date32, $datetime64, $timestamp64, $interval64;", TxControl.serializableRw())
        ).join().getValue();

        ResultSetReader resultSetReader = reader.getResultSet(0);

        Assert.assertTrue(resultSetReader.next());
        Assert.assertEquals(LocalDate.parse("0998-06-02"), resultSetReader.getColumn(0).getDate32());
        Assert.assertEquals(LocalDateTime.parse("0998-06-02T12:30:00"), resultSetReader.getColumn(1).getDatetime64());
        Assert.assertEquals(Instant.parse("0998-06-02T12:30:00.678901Z"), resultSetReader.getColumn(2).getTimestamp64());
        Assert.assertEquals(Duration.parse("-PT2S"), resultSetReader.getColumn(3).getInterval64());
    }

    @Test
    public void timestamp64ReadTest() {
        DataQueryResult result = CTX.supplyResult(
                s -> s.executeDataQuery("SELECT "
                                + "Timestamp64('-144169-01-01T00:00:00Z') as t1,"
                                + "Timestamp64('148107-12-31T23:59:59.999999Z') as t2;",
                        TxControl.serializableRw()
                )).join().getValue();

        Assert.assertEquals(1, result.getResultSetCount());

        ResultSetReader rs = result.getResultSet(0);
        Assert.assertTrue(rs.next());

        assertTimestamp64(rs.getColumn("t1"), Instant.ofEpochSecond(-4611669897600L));
        assertTimestamp64(rs.getColumn("t2"), Instant.ofEpochSecond(4611669811199L, 999999000));

        Status invalid = CTX.supplyResult(
                s -> s.executeDataQuery("SELECT "
                                + "Timestamp64('-144170-01-01T00:00:00Z') as t1,"
                                + "Timestamp64('148108-01-01T00:00:00.000000Z') as t2;",
                        TxControl.serializableRw()
                )
        ).join().getStatus();

        Assert.assertEquals(StatusCode.GENERIC_ERROR, invalid.getCode());
        Issue[] issues = invalid.getIssues();
        Assert.assertEquals(2, issues.length);
        Assert.assertEquals("Invalid value \"-144170-01-01T00:00:00Z\" for type Timestamp64", issues[0].getMessage());
        Assert.assertEquals("Invalid value \"148108-01-01T00:00:00.000000Z\" for type Timestamp64", issues[1].getMessage());
    }

    private void date32datetime64timestamp64interval64Assert(LocalDate date32, LocalDateTime datetime64,
                                                             Instant timestamp64, Duration interval64) {
        DataQueryResult reader = CTX.supplyResult(
                s -> s.executeDataQuery("" +
                                "DECLARE $date32 AS date32;\n" +
                                "DECLARE $datetime64 AS Datetime64;\n" +
                                "DECLARE $timestamp64 AS Timestamp64;\n" +
                                "DECLARE $interval64 AS Interval64;" +
                                "SELECT  $date32, $datetime64, $timestamp64, $interval64;",
                        TxControl.serializableRw(),
                        Params.of(
                                "$date32", PrimitiveValue.newDate32(date32),
                                "$datetime64", PrimitiveValue.newDatetime64(datetime64),
                                "$timestamp64", PrimitiveValue.newTimestamp64(timestamp64),
                                "$interval64", PrimitiveValue.newInterval64(interval64)
                        )
                )).join().getValue();

        ResultSetReader resultSetReader = reader.getResultSet(0);

        Assert.assertTrue(resultSetReader.next());
        Assert.assertEquals(date32, resultSetReader.getColumn(0).getDate32());
        Assert.assertEquals(datetime64, resultSetReader.getColumn(1).getDatetime64());
        Assert.assertEquals(timestamp64, resultSetReader.getColumn(2).getTimestamp64());
        Assert.assertEquals(interval64, resultSetReader.getColumn(3).getInterval64());
    }

    private void assertTimestamp64(ValueReader vr, Instant expected) {
        Assert.assertNotNull(vr);
        Assert.assertSame(PrimitiveType.Timestamp64, vr.getType());
        Assert.assertEquals(expected, vr.getTimestamp64());
    }
}
