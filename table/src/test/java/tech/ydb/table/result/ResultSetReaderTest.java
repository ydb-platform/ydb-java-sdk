package tech.ydb.table.result;

import tech.ydb.ValueProtos;
import tech.ydb.table.result.impl.ProtoValueReaders;
import tech.ydb.table.values.proto.ProtoType;
import tech.ydb.table.values.proto.ProtoValue;
import org.junit.Assert;
import org.junit.Test;


/**
 * @author Sergey Polovko
 */
public class ResultSetReaderTest {

    @Test
    public void readPrimitives() {
        ValueProtos.ResultSet resultSet = ValueProtos.ResultSet.newBuilder()
            .addColumns(newColumn("name", ProtoType.getUtf8()))
            .addColumns(newColumn("year", ProtoType.getUint32()))
            .addRows(newRow(ProtoValue.fromUtf8("Edgy"), ProtoValue.fromUint32(2006)))
            .addRows(newRow(ProtoValue.fromUtf8("Feisty"), ProtoValue.fromUint32(2007)))
            .addRows(newRow(ProtoValue.fromUtf8("Gutsy"), ProtoValue.fromUint32(2007)))
            .addRows(newRow(ProtoValue.fromUtf8("Hardy"), ProtoValue.fromUint32(2008)))
            .build();

        ResultSetReader reader = ProtoValueReaders.forResultSet(resultSet);
        Assert.assertEquals(2, reader.getColumnCount());
        Assert.assertEquals(4, reader.getRowCount());

        Assert.assertTrue(reader.next());
        Assert.assertSame(reader.getColumn("name"), reader.getColumn(0));
        Assert.assertSame(reader.getColumn("year"), reader.getColumn(1));

        Assert.assertEquals("Edgy", reader.getColumn("name").getUtf8());
        Assert.assertEquals(2006, reader.getColumn("year").getUint32());

        Assert.assertTrue(reader.next());
        Assert.assertEquals("Feisty", reader.getColumn("name").getUtf8());
        Assert.assertEquals(2007, reader.getColumn("year").getUint32());

        Assert.assertTrue(reader.next());
        Assert.assertEquals("Gutsy", reader.getColumn("name").getUtf8());
        Assert.assertEquals(2007, reader.getColumn("year").getUint32());

        Assert.assertTrue(reader.next());
        Assert.assertEquals("Hardy", reader.getColumn("name").getUtf8());
        Assert.assertEquals(2008, reader.getColumn("year").getUint32());

        Assert.assertFalse(reader.next());
    }

    @Test
    public void optionalPrimitives() {
        ValueProtos.ResultSet resultSet = ValueProtos.ResultSet.newBuilder()
            .addColumns(newColumn("utf8", ProtoType.getOptional(ProtoType.getUtf8())))
            .addColumns(newColumn("uint32", ProtoType.getOptional(ProtoType.getUint32())))
            .addRows(newRow(ProtoValue.optional(ProtoValue.fromUtf8("a")), ProtoValue.optional()))
            .addRows(newRow(ProtoValue.optional(), ProtoValue.optional(ProtoValue.fromUint32(42))))
            .build();

        ResultSetReader reader = ProtoValueReaders.forResultSet(resultSet);
        Assert.assertEquals(2, reader.getColumnCount());
        Assert.assertEquals(2, reader.getRowCount());

        // row 0
        {
            Assert.assertTrue(reader.next());

            ValueReader utf8 = reader.getColumn("utf8");
            Assert.assertTrue(utf8.isOptionalItemPresent());
            Assert.assertEquals("a", utf8.getUtf8());

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
