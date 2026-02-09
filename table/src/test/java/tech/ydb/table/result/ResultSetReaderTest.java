package tech.ydb.table.result;

import org.junit.Assert;
import org.junit.Test;
import org.junit.function.ThrowingRunnable;

import tech.ydb.proto.ValueProtos;
import tech.ydb.table.result.impl.ProtoValueReaders;
import tech.ydb.table.values.PrimitiveType;
import tech.ydb.table.values.proto.ProtoType;
import tech.ydb.table.values.proto.ProtoValue;


/**
 * @author Sergey Polovko
 */
public class ResultSetReaderTest {

    private void assertNotPositioned(ThrowingRunnable runnable) {
        IllegalStateException ex = Assert.assertThrows(IllegalStateException.class, runnable);
        Assert.assertEquals("ResultSetReader not positioned properly, perhaps you need to call next.", ex.getMessage());
    }

    private void assertIllegalArgumentException(String msg, ThrowingRunnable runnable) {
        IllegalArgumentException ex = Assert.assertThrows(IllegalArgumentException.class, runnable);
        Assert.assertEquals(msg, ex.getMessage());
    }

    @Test
    public void columnInfoTest() {
        ValueProtos.ResultSet resultSet = ValueProtos.ResultSet.newBuilder()
                .addColumns(newColumn("name", ProtoType.getText()))
                .addColumns(newColumn("year", ProtoType.getUint32()))
                .addRows(newRow(ProtoValue.fromText("Edgy"), ProtoValue.fromUint32(2006)))
                .build();

        ResultSetReader reader = ProtoValueReaders.forResultSet(resultSet);

        Assert.assertEquals("name", reader.getColumnName(0));
        Assert.assertEquals(PrimitiveType.Text, reader.getColumnType(0));
        Assert.assertEquals(0, reader.getColumnIndex("name"));

        Assert.assertEquals("year", reader.getColumnName(1));
        Assert.assertEquals(PrimitiveType.Uint32, reader.getColumnType(1));
        Assert.assertEquals(1, reader.getColumnIndex("year"));

        Assert.assertEquals(-1, reader.getColumnIndex("test")); // not found
        assertIllegalArgumentException("Column index: -1, columns count: 2", () -> reader.getColumnName(-1));
        assertIllegalArgumentException("Column index: 2, columns count: 2", () -> reader.getColumnName(2));
        assertIllegalArgumentException("Column index: -1, columns count: 2", () -> reader.getColumnType(-1));
        assertIllegalArgumentException("Column index: 2, columns count: 2", () -> reader.getColumnType(2));

        // before first row state
        assertNotPositioned(() -> reader.getColumn("name"));
        assertNotPositioned(() -> reader.getColumn(0));

        Assert.assertTrue(reader.next()); // first row

        assertIllegalArgumentException("Column index: -1, columns count: 2", () -> reader.getColumn(-1));
        assertIllegalArgumentException("Column index: 2, columns count: 2", () -> reader.getColumn(2));

        assertIllegalArgumentException("Unknown column 'test'", () -> reader.getColumn("test"));
    }

    @Test
    public void iterateReaderTest() {
        ValueProtos.ResultSet resultSet = ValueProtos.ResultSet.newBuilder()
                .addColumns(newColumn("name", ProtoType.getText()))
                .addColumns(newColumn("year", ProtoType.getUint32()))
                .addRows(newRow(ProtoValue.fromText("Edgy"), ProtoValue.fromUint32(2006)))
                .addRows(newRow(ProtoValue.fromText("Feisty"), ProtoValue.fromUint32(2007)))
                .addRows(newRow(ProtoValue.fromText("Gutsy"), ProtoValue.fromUint32(2007)))
                .addRows(newRow(ProtoValue.fromText("Hardy"), ProtoValue.fromUint32(2008)))
                .build();

        ResultSetReader reader = ProtoValueReaders.forResultSet(resultSet);

        // before first row state
        assertNotPositioned(() -> reader.getColumn("name"));
        assertNotPositioned(() -> reader.getColumn(0));

        Assert.assertTrue(reader.next()); // first row
        Assert.assertEquals("Edgy", reader.getColumn("name").getText());
        Assert.assertEquals(2006, reader.getColumn("year").getUint32());

        reader.setRowIndex(-10); // reset to before first
        assertNotPositioned(() -> reader.getColumn("name"));
        assertNotPositioned(() -> reader.getColumn(0));

        Assert.assertTrue(reader.next()); // first row
        Assert.assertEquals("Edgy", reader.getColumn("name").getText());
        Assert.assertEquals(2006, reader.getColumn("year").getUint32());

        reader.setRowIndex(3); // to last row
        Assert.assertEquals("Hardy", reader.getColumn("name").getText());
        Assert.assertEquals(2008, reader.getColumn("year").getUint32());

        Assert.assertFalse(reader.next()); // after last row
        assertNotPositioned(() -> reader.getColumn("name"));
        assertNotPositioned(() -> reader.getColumn(0));

        reader.setRowIndex(0); // reset to first
        Assert.assertEquals("Edgy", reader.getColumn("name").getText());
        Assert.assertEquals(2006, reader.getColumn("year").getUint32());

        Assert.assertTrue(reader.next()); // second row
        Assert.assertEquals("Feisty", reader.getColumn("name").getText());
        Assert.assertEquals(2007, reader.getColumn("year").getUint32());

        reader.setRowIndex(1000); // after last row
        assertNotPositioned(() -> reader.getColumn("name"));
        assertNotPositioned(() -> reader.getColumn(0));
    }

    @Test
    public void readPrimitives() {
        ValueProtos.ResultSet resultSet = ValueProtos.ResultSet.newBuilder()
                .addColumns(newColumn("name", ProtoType.getText()))
                .addColumns(newColumn("year", ProtoType.getUint32()))
                .addRows(newRow(ProtoValue.fromText("Edgy"), ProtoValue.fromUint32(2006)))
                .addRows(newRow(ProtoValue.fromText("Feisty"), ProtoValue.fromUint32(2007)))
                .addRows(newRow(ProtoValue.fromText("Gutsy"), ProtoValue.fromUint32(2007)))
                .addRows(newRow(ProtoValue.fromText("Hardy"), ProtoValue.fromUint32(2008)))
                .setTruncated(true)
                .build();

        ResultSetReader reader = ProtoValueReaders.forResultSet(resultSet);
        Assert.assertEquals(2, reader.getColumnCount());
        Assert.assertEquals(4, reader.getRowCount());
        Assert.assertTrue(reader.isTruncated());

        Assert.assertTrue(reader.next());
        Assert.assertSame(reader.getColumn("name"), reader.getColumn(0));
        Assert.assertSame(reader.getColumn("year"), reader.getColumn(1));

        Assert.assertEquals("Edgy", reader.getColumn("name").getText());
        Assert.assertEquals(2006, reader.getColumn("year").getUint32());

        Assert.assertTrue(reader.next());
        Assert.assertEquals("Feisty", reader.getColumn("name").getText());
        Assert.assertEquals(2007, reader.getColumn("year").getUint32());

        Assert.assertTrue(reader.next());
        Assert.assertEquals("Gutsy", reader.getColumn("name").getText());
        Assert.assertEquals(2007, reader.getColumn("year").getUint32());

        Assert.assertTrue(reader.next());
        Assert.assertEquals("Hardy", reader.getColumn("name").getText());
        Assert.assertEquals(2008, reader.getColumn("year").getUint32());

        Assert.assertFalse(reader.next());
    }

    @Test
    public void readUnsignedInts() {
        ValueProtos.ResultSet resultSet = ValueProtos.ResultSet.newBuilder()
                .addColumns(newColumn("u8", ProtoType.getUint8()))
                .addColumns(newColumn("u16", ProtoType.getUint16()))
                .addColumns(newColumn("u32", ProtoType.getUint32()))
                .addColumns(newColumn("u64", ProtoType.getUint64()))
                .addRows(newRow(
                        ProtoValue.fromUint8(1), ProtoValue.fromUint16(1),
                        ProtoValue.fromUint32(1), ProtoValue.fromUint64(1)))
                .addRows(newRow(
                        ProtoValue.fromUint8(-1), ProtoValue.fromUint16(-1),
                        ProtoValue.fromUint32(-1), ProtoValue.fromUint64(-1)))
                .addRows(newRow(
                        ProtoValue.fromUint8(0xFF), ProtoValue.fromUint16(0xFFFF),
                        ProtoValue.fromUint32(0xFFFFFFFFl), ProtoValue.fromUint64(0xFFFFFFFFFFFFFFFFl)))
                .setTruncated(false)
                .build();

        ResultSetReader reader = ProtoValueReaders.forResultSet(resultSet);
        Assert.assertEquals(4, reader.getColumnCount());
        Assert.assertEquals(3, reader.getRowCount());
        Assert.assertFalse(reader.isTruncated());

        Assert.assertTrue(reader.next());
        Assert.assertSame(reader.getColumn("u8"), reader.getColumn(0));
        Assert.assertSame(reader.getColumn("u16"), reader.getColumn(1));
        Assert.assertSame(reader.getColumn("u32"), reader.getColumn(2));
        Assert.assertSame(reader.getColumn("u64"), reader.getColumn(3));

        Assert.assertEquals(1, reader.getColumn("u8").getUint8());
        Assert.assertEquals(1, reader.getColumn("u16").getUint16());
        Assert.assertEquals(1, reader.getColumn("u32").getUint32());
        Assert.assertEquals(1, reader.getColumn("u64").getUint64());

        Assert.assertTrue(reader.next());
        Assert.assertEquals(0xFF, reader.getColumn("u8").getUint8());
        Assert.assertEquals(0xFFFF, reader.getColumn("u16").getUint16());
        Assert.assertEquals(0xFFFFFFFFl, reader.getColumn("u32").getUint32());
        Assert.assertEquals(0xFFFFFFFFFFFFFFFFl, reader.getColumn("u64").getUint64());

        Assert.assertTrue(reader.next());
        Assert.assertEquals(0xFF, reader.getColumn("u8").getUint8());
        Assert.assertEquals(0xFFFF, reader.getColumn("u16").getUint16());
        Assert.assertEquals(0xFFFFFFFFl, reader.getColumn("u32").getUint32());
        Assert.assertEquals(0xFFFFFFFFFFFFFFFFl, reader.getColumn("u64").getUint64());

        Assert.assertFalse(reader.next());
    }

    @Test
    public void optionalPrimitives() {
        ValueProtos.ResultSet resultSet = ValueProtos.ResultSet.newBuilder()
            .addColumns(newColumn("utf8", ProtoType.getOptional(ProtoType.getText())))
            .addColumns(newColumn("uint32", ProtoType.getOptional(ProtoType.getUint32())))
            .addRows(newRow(ProtoValue.fromText("a"), ProtoValue.optional()))
            .addRows(newRow(ProtoValue.optional(), ProtoValue.fromUint32(42)))
            .build();

        ResultSetReader reader = ProtoValueReaders.forResultSet(resultSet);
        Assert.assertEquals(2, reader.getColumnCount());
        Assert.assertEquals(2, reader.getRowCount());

        // row 0
        {
            Assert.assertTrue(reader.next());

            ValueReader utf8 = reader.getColumn("utf8");
            Assert.assertTrue(utf8.isOptionalItemPresent());
            Assert.assertEquals("a", utf8.getText());

            ValueReader uint32 = reader.getColumn("uint32");
            Assert.assertFalse(uint32.isOptionalItemPresent());
        }

        // row 1
        {
            Assert.assertTrue(reader.next());

            ValueReader utf8 = reader.getColumn("utf8");
            Assert.assertFalse(utf8.isOptionalItemPresent());

            ValueReader uint32 = reader.getColumn("uint32");
            Assert.assertTrue(uint32.isOptionalItemPresent());
            Assert.assertEquals(42, uint32.getUint32());
        }

        Assert.assertFalse(reader.next());
    }

    private static ValueProtos.Column newColumn(String name, ValueProtos.Type type) {
        return ValueProtos.Column.newBuilder().setName(name).setType(type).build();
    }

    private static ValueProtos.Value newRow(ValueProtos.Value... columns) {
        ValueProtos.Value.Builder builder = ValueProtos.Value.newBuilder();
        for (ValueProtos.Value column : columns) {
            builder.addItems(column);
        }
        return builder.build();
    }
}
