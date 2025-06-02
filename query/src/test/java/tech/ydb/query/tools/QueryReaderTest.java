package tech.ydb.query.tools;

import java.util.Arrays;
import org.junit.Assert;
import org.junit.Test;
import tech.ydb.proto.ValueProtos;
import tech.ydb.query.result.QueryInfo;
import tech.ydb.query.result.QueryResultPart;
import tech.ydb.table.result.ResultSetReader;

/**
 *
 * @author zinal
 */
public class QueryReaderTest {

    @Test
    public void testNextAndSetIndex() {
        QueryInfo qi = new QueryInfo(null);
        QueryReader.ResultSetParts rsp0 = initRsp(0L);
        QueryReader.ResultSetParts rsp1 = initRsp(1L);
        QueryReader.ResultSetParts rsp2 = initRsp(2L);
        QueryReader qr = new QueryReader(qi, null, Arrays.asList(rsp0, rsp1, rsp2));
        checkRows(qr, 0);
        checkRows(qr, 1);
        checkRows(qr, 2);
    }

    private QueryReader.ResultSetParts initRsp(long ix) {
        QueryReader.ResultSetParts rsp = new QueryReader.ResultSetParts(ix);
        rsp.addPart(initQrp(ix));
        rsp.addPart(initQrp(ix));
        rsp.addPart(initQrp(ix));
        rsp.addPart(initQrp(ix));
        return rsp;
    }

    private QueryResultPart initQrp(long ix) {
        ValueProtos.ResultSet.Builder rsBuilder = ValueProtos.ResultSet.newBuilder();
        rsBuilder.setTruncated(false);
        rsBuilder.addColumns(ValueProtos.Column.newBuilder()
                .setName("a")
                .setType(ValueProtos.Type.newBuilder().setTypeIdValue(ValueProtos.Type.PrimitiveTypeId.INT32_VALUE).build())
                .build()
        );
        rsBuilder.addColumns(ValueProtos.Column.newBuilder()
                .setName("b")
                .setType(ValueProtos.Type.newBuilder().setTypeIdValue(ValueProtos.Type.PrimitiveTypeId.UTF8_VALUE).build())
                .build()
        );
        rsBuilder.addColumns(ValueProtos.Column.newBuilder()
                .setName("c")
                .setType(ValueProtos.Type.newBuilder().setTypeIdValue(ValueProtos.Type.PrimitiveTypeId.INT64_VALUE).build())
                .build()
        );
        rsBuilder.addRows(ValueProtos.Value.newBuilder()
                .addItems(ValueProtos.Value.newBuilder()
                        .setInt32Value(10)
                        .build())
                .addItems(ValueProtos.Value.newBuilder()
                        .setTextValue("aaa")
                        .build())
                .addItems(ValueProtos.Value.newBuilder()
                        .setInt64Value(100L)
                        .build())
                .build());
        rsBuilder.addRows(ValueProtos.Value.newBuilder()
                .addItems(ValueProtos.Value.newBuilder()
                        .setInt32Value(20)
                        .build())
                .addItems(ValueProtos.Value.newBuilder()
                        .setTextValue("bbb")
                        .build())
                .addItems(ValueProtos.Value.newBuilder()
                        .setInt64Value(200L)
                        .build())
                .build());
        rsBuilder.addRows(ValueProtos.Value.newBuilder()
                .addItems(ValueProtos.Value.newBuilder()
                        .setInt32Value(30)
                        .build())
                .addItems(ValueProtos.Value.newBuilder()
                        .setTextValue("ccc")
                        .build())
                .addItems(ValueProtos.Value.newBuilder()
                        .setInt64Value(300L)
                        .build())
                .build());
        return new QueryResultPart(ix, rsBuilder.build());
    }

    private void checkRows(QueryReader qr, int rsIndex) {
        ResultSetReader rsr = qr.getResultSet(rsIndex);
        Assert.assertNotNull(rsr);
        
        Assert.assertEquals(3, rsr.getColumnCount());
        Assert.assertEquals("a", rsr.getColumnName(0));
        Assert.assertEquals("b", rsr.getColumnName(1));
        Assert.assertEquals("c", rsr.getColumnName(2));

        int rowCount = 0;
        while (rsr.next()) {
            Assert.assertNotNull(rsr.getColumn(0));
            Assert.assertNotNull(rsr.getColumn(1));
            Assert.assertNotNull(rsr.getColumn(2));
            rowCount += 1;
        }
        Assert.assertEquals(12, rowCount);

        rsr.setRowIndex(0);
        rowCount = 0;
        while (rsr.next()) {
            rowCount += 1;
        }
        Assert.assertEquals(12, rowCount);
    }
    
}
