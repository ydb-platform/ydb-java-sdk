package tech.ydb.table.types;

import tech.ydb.table.values.PrimitiveType;
import tech.ydb.table.values.StructType;
import org.junit.Assert;
import org.junit.Test;


/**
 * @author Sergey Polovko
 */
public class StructTypeTest {

    @Test
    public void oneMember() {
        StructType s = StructType.of("a", PrimitiveType.String);

        Assert.assertEquals(1, s.getMembersCount());
        Assert.assertEquals("Struct<'a': String>", s.toString());

        Assert.assertEquals("a", s.getMemberName(0));
        Assert.assertEquals(PrimitiveType.String, s.getMemberType(0));
        Assert.assertEquals(0, s.getMemberIndex("a"));
    }

    @Test
    public void manyMembers() {
        // not ordered names
        StructType s = StructType.of(
            "b", PrimitiveType.Int32,
            "a", PrimitiveType.String);

        Assert.assertEquals(2, s.getMembersCount());
        Assert.assertEquals("Struct<'a': String, 'b': Int32>", s.toString());

        // member 'a'
        Assert.assertEquals("a", s.getMemberName(0));
        Assert.assertEquals(PrimitiveType.String, s.getMemberType(0));
        Assert.assertEquals(0, s.getMemberIndex("a"));

        // member 'b'
        Assert.assertEquals("b", s.getMemberName(1));
        Assert.assertEquals(PrimitiveType.Int32, s.getMemberType(1));
        Assert.assertEquals(1, s.getMemberIndex("b"));
    }
}
