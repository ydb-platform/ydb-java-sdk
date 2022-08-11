package tech.ydb.table.result.impl;

import tech.ydb.ValueProtos;
import tech.ydb.table.result.ValueReader;
import tech.ydb.table.values.proto.ProtoType;
import tech.ydb.table.values.proto.ProtoValue;
import org.junit.Assert;
import org.junit.Test;


/**
 * @author Sergey Polovko
 */
public class TupleReaderTest {

    @Test
    public void empty() {
        ValueProtos.Type type = ProtoType.getTuple();
        ValueProtos.Value value = ProtoValue.tuple();

        AbstractValueReader reader = ProtoValueReaders.forTypeImpl(type);
        reader.setProtoValue(value);

        Assert.assertTrue(reader instanceof ProtoTupleValueReader);
        Assert.assertEquals(0, reader.getTupleElementsCount());
    }

    @Test
    public void primitives() {
        ValueProtos.Type type = ProtoType.getTuple(
            ProtoType.getUtf8(),
            ProtoType.getUint32(),
            ProtoType.getDouble());

        ValueProtos.Value value = ProtoValue.tuple(
            ProtoValue.fromUtf8("hello"),
            ProtoValue.fromUint32(42),
            ProtoValue.fromDouble(3.14159));

        AbstractValueReader reader = ProtoValueReaders.forTypeImpl(type);
        reader.setProtoValue(value);

        Assert.assertTrue(reader instanceof ProtoTupleValueReader);
        Assert.assertEquals(3, reader.getTupleElementsCount());

        Assert.assertEquals("hello", reader.getTupleElement(0).getUtf8());
        Assert.assertEquals(42, reader.getTupleElement(1).getUint32());
        Assert.assertEquals(3.14159, reader.getTupleElement(2).getFloat64(), Double.MIN_VALUE);
    }

    @Test
    public void nested() {
        ValueProtos.Type type = ProtoType.getTuple(
            ProtoType.getTuple(ProtoType.getUtf8(), ProtoType.getUint32()),
            ProtoType.getTuple(ProtoType.getUint32(), ProtoType.getUtf8()));

        ValueProtos.Value value = ProtoValue.tuple(
            ProtoValue.tuple(ProtoValue.fromUtf8("hello"), ProtoValue.fromUint32(42)),
            ProtoValue.tuple(ProtoValue.fromUint32(37), ProtoValue.fromUtf8("bye")));

        AbstractValueReader reader = ProtoValueReaders.forTypeImpl(type);
        reader.setProtoValue(value);

        Assert.assertTrue(reader instanceof ProtoTupleValueReader);
        Assert.assertEquals(2, reader.getTupleElementsCount());

        // 1st tuple
        {
            ValueReader tuple1 = reader.getTupleElement(0);

            Assert.assertTrue(tuple1 instanceof ProtoTupleValueReader);
            Assert.assertEquals(2, tuple1.getTupleElementsCount());

            Assert.assertEquals("hello", tuple1.getTupleElement(0).getUtf8());
            Assert.assertEquals(42, tuple1.getTupleElement(1).getUint32());
        }

        // 2nd tuple
        {
            ValueReader tuple2 = reader.getTupleElement(1);

            Assert.assertTrue(tuple2 instanceof ProtoTupleValueReader);
            Assert.assertEquals(2, tuple2.getTupleElementsCount());

            Assert.assertEquals(37, tuple2.getTupleElement(0).getUint32());
            Assert.assertEquals("bye", tuple2.getTupleElement(1).getUtf8());
        }
    }
}
