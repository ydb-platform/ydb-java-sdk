package tech.ydb.table.integration;

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
import tech.ydb.table.values.PrimitiveType;
import tech.ydb.table.values.VoidType;
import tech.ydb.table.values.VoidValue;
import tech.ydb.test.junit4.GrpcTransportRule;

/**
 *
 * @author Aleksandr Gorshenin
 */
public class NullReadTest {
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

        Assert.assertEquals(PrimitiveType.Bytes, p1.getType());
        Assert.assertEquals(PrimitiveType.Int32, p2.getType());
        Assert.assertEquals(VoidType.of(), p3.getType());

        Assert.assertArrayEquals(new byte[] { '1' }, p1.getBytes());
        Assert.assertEquals(123, p2.getInt32());
        Assert.assertEquals(VoidValue.of(), p3.getValue());
    }
}
