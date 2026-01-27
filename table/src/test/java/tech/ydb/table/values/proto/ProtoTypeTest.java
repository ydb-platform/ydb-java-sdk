package tech.ydb.table.values.proto;

import java.util.ArrayList;
import java.util.Arrays;

import org.junit.Assert;
import org.junit.Test;

import tech.ydb.proto.ValueProtos;
import tech.ydb.table.values.PrimitiveType;
import tech.ydb.table.values.StructType;
import tech.ydb.table.values.TupleType;

/**
 *
 * @author Aleksandr Gorshenin
 */
public class ProtoTypeTest {

    @Test
    public void getStructTest() {
        StructType s1 = StructType.of("a", PrimitiveType.Text, "b", PrimitiveType.Bytes);

        ValueProtos.StructMember sm1 = ProtoType.getStructMember("a", ProtoType.getText());
        ValueProtos.StructMember sm2 = ProtoType.getStructMember("b", ProtoType.getBytes());

        Assert.assertEquals(s1.toPb(), ProtoType.getStruct(sm1, sm2));
        Assert.assertEquals(s1.toPb(), ProtoType.getStruct(Arrays.asList(sm1, sm2)));

        Assert.assertNotEquals(s1.toPb(), ProtoType.getStruct(sm2, sm1));
        Assert.assertNotEquals(s1.toPb(), ProtoType.getStruct(Arrays.asList(sm2, sm1)));

        IllegalArgumentException ex = Assert.assertThrows(IllegalArgumentException.class,
                () -> ProtoType.getStruct(new ArrayList<>())
        );
        Assert.assertEquals("Struct members cannot be empty", ex.getMessage());
    }

    @Test
    public void getTupleTest() {
        TupleType t1 = TupleType.ofOwn(PrimitiveType.Text, PrimitiveType.Bytes);

        Assert.assertEquals(t1.toPb(), ProtoType.getTuple(ProtoType.getText(), ProtoType.getBytes()));
        Assert.assertEquals(t1.toPb(), ProtoType.getTuple(Arrays.asList(ProtoType.getText(), ProtoType.getBytes())));

        Assert.assertNotEquals(t1.toPb(), ProtoType.getTuple(ProtoType.getBytes(), ProtoType.getText()));
        Assert.assertNotEquals(t1.toPb(), ProtoType.getTuple(Arrays.asList(ProtoType.getBytes(), ProtoType.getText())));


        IllegalArgumentException ex = Assert.assertThrows(IllegalArgumentException.class,
                () -> ProtoType.getTuple(new ArrayList<>())
        );
        Assert.assertEquals("Tuple elements cannot be empty", ex.getMessage());
    }
}
