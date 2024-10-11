package tech.ydb.table.integration;

import java.util.UUID;

import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Test;

import tech.ydb.table.SessionRetryContext;
import tech.ydb.table.impl.SimpleTableClient;
import tech.ydb.table.query.DataQueryResult;
import tech.ydb.table.result.ResultSetReader;
import tech.ydb.table.result.ValueReader;
import tech.ydb.table.rpc.grpc.GrpcTableRpc;
import tech.ydb.table.transaction.TxControl;
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
}
