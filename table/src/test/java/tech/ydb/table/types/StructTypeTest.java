package tech.ydb.table.types;

import org.junit.Assert;
import org.junit.Test;


/**
 * @author Sergey Polovko
 */
public class StructTypeTest {

    @Test
    public void oneMember() {
        StructType s = StructType.of("a", PrimitiveType.string());

        Assert.assertEquals(1, s.getMembersCount());
        Assert.assertEquals("Struct<'a': String>", s.toString());

        Assert.assertEquals("a", s.getMemberName(0));
        Assert.assertEquals(PrimitiveType.string(), s.getMemberType(0));
        Assert.assertEquals(0, s.getMemberIndex("a"));
    }

    @Test
    public void manyMembers() {
        // not ordered names
        StructType s = StructType.of(
            "b", PrimitiveType.int32(),
            "a", PrimitiveType.string());

        Assert.assertEquals(2, s.getMembersCount());
        Assert.assertEquals("Struct<'a': String, 'b': Int32>", s.toString());

        // member 'a'
        Assert.assertEquals("a", s.getMemberName(0));
        Assert.assertEquals(PrimitiveType.string(), s.getMemberType(0));
        Assert.assertEquals(0, s.getMemberIndex("a"));

        // member 'b'
        Assert.assertEquals("b", s.getMemberName(1));
        Assert.assertEquals(PrimitiveType.int32(), s.getMemberType(1));
        Assert.assertEquals(1, s.getMemberIndex("b"));
    }
}
