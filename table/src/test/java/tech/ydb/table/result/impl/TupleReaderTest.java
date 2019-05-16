package tech.ydb.table.result.impl;

import org.junit.Assert;
import org.junit.Test;

import tech.ydb.ValueProtos;
import tech.ydb.table.result.ValueReader;
import tech.ydb.table.types.proto.ProtoType;
import tech.ydb.table.values.proto.ProtoValue;


/**
 * @author Sergey Polovko
 */
public class TupleReaderTest {

    @Test
    public void empty() {
        ValueProtos.Type type = ProtoType.tuple();
        ValueProtos.Value value = ProtoValue.tuple();

        AbstractValueReader reader = ProtoValueReaders.forTypeImpl(type);
        reader.setValue(value);

        Assert.assertTrue(reader instanceof ProtoTupleValueReader);
        Assert.assertEquals(0, reader.getTupleElementsCount());
    }

    @Test
    public void primitives() {
        ValueProtos.Type type = ProtoType.tuple(
            ProtoType.utf8(),
            ProtoType.uint32(),
            ProtoType.float64());

        ValueProtos.Value value = ProtoValue.tuple(
            ProtoValue.utf8("hello"),
            ProtoValue.uint32(42),
            ProtoValue.float64(3.14159));

        AbstractValueReader reader = ProtoValueReaders.forTypeImpl(type);
        reader.setValue(value);

        Assert.assertTrue(reader instanceof ProtoTupleValueReader);
        Assert.assertEquals(3, reader.getTupleElementsCount());

        Assert.assertEquals("hello", reader.getTupleElement(0).getUtf8());
        Assert.assertEquals(42, reader.getTupleElement(1).getUint32());
        Assert.assertEquals(3.14159, reader.getTupleElement(2).getFloat64(), Double.MIN_VALUE);
    }

    @Test
    public void nested() {
        ValueProtos.Type type = ProtoType.tuple(
            ProtoType.tuple(ProtoType.utf8(), ProtoType.uint32()),
            ProtoType.tuple(ProtoType.uint32(), ProtoType.utf8()));

        ValueProtos.Value value = ProtoValue.tuple(
            ProtoValue.tuple(ProtoValue.utf8("hello"), ProtoValue.uint32(42)),
            ProtoValue.tuple(ProtoValue.uint32(37), ProtoValue.utf8("bye")));

        AbstractValueReader reader = ProtoValueReaders.forTypeImpl(type);
        reader.setValue(value);

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
