package tech.ydb.query.tools;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.junit.Assert;
import org.junit.Test;

import tech.ydb.core.Result;
import tech.ydb.proto.ValueProtos;
import tech.ydb.query.QueryStream;
import tech.ydb.query.result.QueryInfo;
import tech.ydb.query.result.QueryResultPart;
import tech.ydb.table.result.ResultSetReader;
import tech.ydb.table.values.PrimitiveType;
import tech.ydb.table.values.PrimitiveValue;

/**
 *
 * @author zinal
 * @author Aleksandr Gorshenin
 */
public class QueryReaderTest {

    @Test
    public void simpleTest() {
        TestStream stream = new TestStream();
        stream.addPartRows(0, 0, 1);
        stream.addPartRows(2, 0, 0);

        QueryReader qr = QueryReader.readFrom(stream).join().getValue();

        Assert.assertNotNull(qr.getQueryInfo());
        Assert.assertTrue(qr.getIssueList().isEmpty());

        Assert.assertEquals(3, qr.getResultSetCount());

        ResultSetReader rsr1 = qr.getResultSet(0);
        ResultSetReader rsr2 = qr.getResultSet(1);
        ResultSetReader rsr3 = qr.getResultSet(2);

        Assert.assertFalse(rsr1.isTruncated());
        Assert.assertFalse(rsr2.isTruncated());
        Assert.assertFalse(rsr3.isTruncated());

        // first result set is one line
        Assert.assertEquals(1, rsr1.getRowCount());
        Assert.assertEquals(3, rsr1.getColumnCount());
        Assert.assertEquals("a", rsr1.getColumnName(0));
        Assert.assertEquals("b", rsr1.getColumnName(1));
        Assert.assertEquals("c", rsr1.getColumnName(2));
        Assert.assertEquals(0, rsr1.getColumnIndex("a"));
        Assert.assertEquals(1, rsr1.getColumnIndex("b"));
        Assert.assertEquals(2, rsr1.getColumnIndex("c"));
        Assert.assertEquals(PrimitiveType.Int32, rsr1.getColumnType(0));
        Assert.assertEquals(PrimitiveType.Text, rsr1.getColumnType(1));
        Assert.assertEquals(PrimitiveType.Int64, rsr1.getColumnType(2));

        // second result set is null
        Assert.assertEquals(0, rsr2.getRowCount());
        Assert.assertEquals(0, rsr2.getColumnCount());
        Assert.assertEquals(null, rsr2.getColumnName(0));
        Assert.assertEquals(-1, rsr2.getColumnIndex("a"));
        Assert.assertEquals(null, rsr2.getColumnType(0));
        Assert.assertEquals(null, rsr2.getColumn("any"));
        Assert.assertEquals(null, rsr2.getColumn(0));

        // third result set is empty
        Assert.assertEquals(0, rsr3.getRowCount());
        Assert.assertEquals(3, rsr3.getColumnCount());
        Assert.assertEquals("a", rsr3.getColumnName(0));
        Assert.assertEquals("b", rsr3.getColumnName(1));
        Assert.assertEquals("c", rsr3.getColumnName(2));
        Assert.assertEquals(0, rsr3.getColumnIndex("a"));
        Assert.assertEquals(1, rsr3.getColumnIndex("b"));
        Assert.assertEquals(2, rsr3.getColumnIndex("c"));
        Assert.assertEquals(PrimitiveType.Int32, rsr3.getColumnType(0));
        Assert.assertEquals(PrimitiveType.Text, rsr3.getColumnType(1));
        Assert.assertEquals(PrimitiveType.Int64, rsr3.getColumnType(2));
    }

    @Test
    public void iteratorTest() {
        TestStream stream = new TestStream();
        stream.addPartRows(0, 0, 1);
        stream.addPartRows(1, 0, 2);
        stream.addPartRows(2, 0, 3);

        QueryReader qr = QueryReader.readFrom(stream).join().getValue();

        int idx = 0;
        for (ResultSetReader rsr: qr) {
            Assert.assertEquals(++idx, rsr.getRowCount());
        }
    }

    @Test
    public void emptyResultsTest() {
        TestStream stream = new TestStream();
        stream.addPartRows(0, 0, 5);
        stream.addPartRows(2, 0, 5);
        stream.addPartRows(7, 0, 5);

        QueryReader qr = QueryReader.readFrom(stream).join().getValue();

        Assert.assertEquals(8, qr.getResultSetCount());
        Assert.assertEquals(5, readAll(qr.getResultSet(0), 0));
        Assert.assertEquals(0, readAll(qr.getResultSet(1), 0));
        Assert.assertEquals(5, readAll(qr.getResultSet(2), 0));
        Assert.assertEquals(0, readAll(qr.getResultSet(3), 0));
        Assert.assertEquals(0, readAll(qr.getResultSet(4), 0));
        Assert.assertEquals(0, readAll(qr.getResultSet(5), 0));
        Assert.assertEquals(0, readAll(qr.getResultSet(6), 0));
        Assert.assertEquals(5, readAll(qr.getResultSet(7), 0));
    }

    @Test
    public void compositeResultSetTest() {
        TestStream stream = new TestStream();
        stream.addPartRows(0, 0, 1); // 0
        stream.addPartRows(0, 1, 2); // 1, 2
        stream.addPartRows(0, 3, 3); // 3, 4, 5

        QueryReader qr = QueryReader.readFrom(stream).join().getValue();

        Assert.assertEquals(1, qr.getResultSetCount());
        ResultSetReader rsr = qr.getResultSet(0);

        Assert.assertEquals(3, rsr.getColumnCount());

        Assert.assertEquals(6, readAll(rsr, 0));
        Assert.assertEquals(0, readAll(rsr, 0));

        rsr.setRowIndex(0);
        Assert.assertEquals(6, readAll(rsr, 0));
        Assert.assertEquals(0, readAll(rsr, 0));

        rsr.setRowIndex(3);
        Assert.assertEquals(3, readAll(rsr, 3));

        rsr.setRowIndex(5);
        Assert.assertEquals(1, readAll(rsr, 5));

        rsr.setRowIndex(-1);
        Assert.assertEquals(6, readAll(rsr, 0));

        rsr.setRowIndex(6);
        Assert.assertEquals(0, readAll(rsr, 0));
//        IndexOutOfBoundsException ex1 = Assert.assertThrows(IndexOutOfBoundsException.class, () -> rsr.setRowIndex(6));
//        Assert.assertEquals("Index 6 out of bounds for length 6", ex1.getMessage());
//
//        IndexOutOfBoundsException ex2 = Assert.assertThrows(IndexOutOfBoundsException.class, () -> rsr.setRowIndex(-1));
//        Assert.assertEquals("Index -1 out of bounds for length 6", ex2.getMessage());
    }

    private int readAll(ResultSetReader rsr, int startKey) {
        int key = startKey;
        while (rsr.next()) {
            Assert.assertEquals(10 + key, rsr.getColumn("a").getInt32());
            Assert.assertEquals(3, rsr.getColumn(1).getText().length());
            Assert.assertEquals(100l * key, rsr.getColumn("c").getInt64());
            key++;
        }

        return key - startKey;
    }

    private static class TestStream implements QueryStream {
        private final List<QueryResultPart> parts = new ArrayList<>();

        @Override
        public CompletableFuture<Result<QueryInfo>> execute(PartsHandler handler) {
            parts.forEach(handler::onNextPart);
            return CompletableFuture.completedFuture(Result.success(new QueryInfo(null)));
        }

        @Override
        public void cancel() {
        }

        public void addPartRows(long rsIdx, int from, int length) {
            ValueProtos.ResultSet.Builder rsb = ValueProtos.ResultSet.newBuilder();
            rsb.setTruncated(false);
            rsb.addColumns(ValueProtos.Column.newBuilder().setName("a").setType(PrimitiveType.Int32.toPb()));
            rsb.addColumns(ValueProtos.Column.newBuilder().setName("b").setType(PrimitiveType.Text.toPb()));
            rsb.addColumns(ValueProtos.Column.newBuilder().setName("c").setType(PrimitiveType.Int64.toPb()));

            for (int key = from; key < from + length; key++) {
                char ch = (char) (key + 'a');
                rsb.addRows(ValueProtos.Value.newBuilder()
                        .addItems(PrimitiveValue.newInt32(10 + key).toPb())
                        .addItems(PrimitiveValue.newText(String.valueOf(new char[] { ch, ch ,ch })).toPb())
                        .addItems(PrimitiveValue.newInt64(key * 100L).toPb())
                );
            }

            parts.add(new QueryResultPart(rsIdx, rsb.build()));
        }
    }
}
