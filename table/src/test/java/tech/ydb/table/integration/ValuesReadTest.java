package tech.ydb.table.integration;

import java.time.Instant;
import java.util.UUID;

import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Test;

import tech.ydb.core.Issue;
import tech.ydb.core.Status;
import tech.ydb.core.StatusCode;
import tech.ydb.table.SessionRetryContext;
import tech.ydb.table.impl.SimpleTableClient;
import tech.ydb.table.query.DataQueryResult;
import tech.ydb.table.result.ResultSetReader;
import tech.ydb.table.result.ValueReader;
import tech.ydb.table.rpc.grpc.GrpcTableRpc;
import tech.ydb.table.transaction.TxControl;
import tech.ydb.table.values.DecimalType;
import tech.ydb.table.values.DecimalValue;
import tech.ydb.table.values.NullType;
import tech.ydb.table.values.NullValue;
import tech.ydb.table.values.PrimitiveType;
import tech.ydb.table.values.PrimitiveValue;
import tech.ydb.table.values.Type;
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

        Assert.assertArrayEquals(new byte[] { '1' }, p1.getBytes());
        Assert.assertEquals(123, p2.getInt32());
        Assert.assertSame(NullValue.of(), p3.getValue());
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
}
