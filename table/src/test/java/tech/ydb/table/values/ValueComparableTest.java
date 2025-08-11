package tech.ydb.table.values;

import java.util.HashMap;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;
import org.junit.function.ThrowingRunnable;


/**
 * Test for Comparable implementation of Value classes
 */
public class ValueComparableTest {

    private void assertNpe(String message, ThrowingRunnable runnable) {
        NullPointerException npe = Assert.assertThrows(NullPointerException.class, runnable);
        Assert.assertEquals(message, npe.getMessage());
    }

    private void assertIllegalArgument(String message, ThrowingRunnable runnable) {
        IllegalArgumentException ex = Assert.assertThrows(IllegalArgumentException.class, runnable);
        Assert.assertEquals(message, ex.getMessage());
    }

    @Test
    public void testPrimitiveValueComparison() {
        // Test numeric comparisons
        PrimitiveValue int1 = PrimitiveValue.newInt32(1);
        PrimitiveValue int2 = PrimitiveValue.newInt32(2);
        PrimitiveValue int3 = PrimitiveValue.newInt32(1);

        Assert.assertTrue(int1.compareTo(int2) < 0);
        Assert.assertTrue(int2.compareTo(int1) > 0);
        Assert.assertEquals(0, int1.compareTo(int3));

        // Optional comparing
        Assert.assertTrue(int1.makeOptional().compareTo(int2) < 0);
        Assert.assertTrue(int2.compareTo(int1.makeOptional()) > 0);
        Assert.assertEquals(0, int1.makeOptional().compareTo(int3));
        Assert.assertEquals(0, int1.compareTo(int3.makeOptional()));

        // Invalid values
        assertNpe("Cannot compare with null value", () -> int1.compareTo(null));

        // Test string comparisons
        PrimitiveValue text1 = PrimitiveValue.newText("abc");
        PrimitiveValue text2 = PrimitiveValue.newText("def");
        PrimitiveValue text3 = PrimitiveValue.newText("abc");

        Assert.assertTrue(text1.compareTo(text2) < 0);
        Assert.assertTrue(text2.compareTo(text1) > 0);
        Assert.assertEquals(0, text1.compareTo(text3));

        // Test boolean comparisons
        PrimitiveValue bool1 = PrimitiveValue.newBool(false);
        PrimitiveValue bool2 = PrimitiveValue.newBool(true);

        Assert.assertTrue(bool1.compareTo(bool2) < 0);
        Assert.assertTrue(bool2.compareTo(bool1) > 0);

        assertIllegalArgument("Cannot compare value Int32 with Text", () -> int1.compareTo(text1));
    }

    @Test
    public void testListValueComparison() {
        ListValue list1 = ListValue.of(PrimitiveValue.newInt32(1), PrimitiveValue.newInt32(2));
        ListValue list2 = ListValue.of(
                PrimitiveValue.newInt32(1).makeOptional(),
                PrimitiveValue.newInt32(2).makeOptional()
        );

        Assert.assertEquals(0, list1.compareTo(list2));
        Assert.assertEquals(0, list1.makeOptional().compareTo(list2));
        Assert.assertEquals(0, list1.compareTo(list2.makeOptional()));

        ListValue list3 = ListValue.of(PrimitiveValue.newInt32(1), PrimitiveValue.newInt32(3));
        ListValue list4 = ListValue.of(PrimitiveValue.newInt32(2), PrimitiveValue.newInt32(2));

        Assert.assertTrue(list1.compareTo(list3) < 0);
        Assert.assertTrue(list2.compareTo(list3) < 0);
        Assert.assertTrue(list4.compareTo(list3) > 0);
        Assert.assertTrue(list3.compareTo(list4) < 0);

        ListValue list5 = ListValue.of(PrimitiveValue.newText("A"), PrimitiveValue.newText("Z"));
        ListValue list6 = ListValue.of(PrimitiveValue.newText("A"), PrimitiveValue.newText("Z"));
        ListValue list7 = ListValue.of(PrimitiveValue.newText("A"));
        ListValue list8 = ListValue.of(PrimitiveValue.newText("Z"));

        Assert.assertEquals(0, list5.compareTo(list6));
        Assert.assertEquals(0, list6.compareTo(list5));
        Assert.assertTrue(list7.compareTo(list5) < 0); // shorter list comes first

        // Test proper lexicographical ordering

        // ('Z') should be "bigger" than ('A','Z') in lexicographical order
        Assert.assertTrue(list5.compareTo(list8) < 0); // ('A','Z') < ('Z')
        Assert.assertTrue(list8.compareTo(list5) > 0); // ('Z') > ('A','Z')

        assertNpe("Cannot compare with null value", () -> list1.compareTo(null));
        assertIllegalArgument("Cannot compare value Int32 with Text", () -> list1.compareTo(list5));
        assertIllegalArgument("Cannot compare value Int32 with Text", () -> list2.compareTo(list5));
    }

    @Test
    public void testStructValueComparison() {
        StructValue s1 = StructValue.of("a", PrimitiveValue.newInt32(1), "b", PrimitiveValue.newInt32(2));
        StructValue s2 = StructValue.of("a", PrimitiveValue.newInt32(1), "b", PrimitiveValue.newInt32(2));
        StructValue s3 = StructValue.of("a", PrimitiveValue.newInt32(2), "b", PrimitiveValue.newInt32(1));
        StructValue s4 = StructValue.of("a", PrimitiveValue.newInt32(1), "b", PrimitiveValue.newText("a"));
        StructValue s5 = StructValue.of("a", PrimitiveValue.newInt32(1));

        Assert.assertEquals(0, s1.compareTo(s2));
        Assert.assertEquals(0, s1.compareTo(s2.makeOptional()));
        Assert.assertEquals(0, s1.makeOptional().compareTo(s2));

        Assert.assertTrue(s1.compareTo(s3) < 0);
        Assert.assertTrue(s3.compareTo(s1) > 0);

        assertNpe("Cannot compare with null value", () -> s1.compareTo(null));
        assertIllegalArgument("Cannot compare value Struct<'a': Int32, 'b': Int32> with Struct<'a': Int32, 'b': Text>",
                () -> s1.compareTo(s4));
        assertIllegalArgument("Cannot compare value Struct<'a': Int32, 'b': Int32> with Struct<'a': Int32>",
                () -> s1.compareTo(s5));
    }

    @Test
    public void testDictValueComparison() {
        DictValue d1 = DictValue.of(PrimitiveValue.newText("a"), PrimitiveValue.newInt32(1));
        DictValue d2 = DictValue.of(PrimitiveValue.newText("a"), PrimitiveValue.newInt32(2));
        DictValue d3 = DictValue.of(PrimitiveValue.newText("a"), PrimitiveValue.newInt32(1));

        Assert.assertTrue(d1.compareTo(d2) < 0);
        Assert.assertTrue(d2.compareTo(d1) > 0);

        Assert.assertEquals(0, d1.compareTo(d3));
        Assert.assertEquals(0, d1.compareTo(d3.makeOptional()));
        Assert.assertEquals(0, d1.makeOptional().compareTo(d3));

        DictValue d4 = DictValue.of(PrimitiveValue.newText("a"), PrimitiveValue.newText("abc"));
        assertIllegalArgument("Cannot compare value Dict<Text, Int32> with Dict<Text, Text>", () -> d1.compareTo(d4));
        assertIllegalArgument("Cannot compare value Dict<Text, Text> with Dict<Text, Int32>", () -> d4.compareTo(d1));

        DictValue d5 = DictValue.of(PrimitiveValue.newText("b"), PrimitiveValue.newInt32(1));

        // {"a": 1} should be "bigger" than {"b": 1 } in lexicographical order
        Assert.assertTrue(d1.compareTo(d5) > 0);
        Assert.assertTrue(d5.compareTo(d1) < 0);

        Map<Value<?>, Value<?>> map6 = new HashMap<>();
        map6.put(PrimitiveValue.newText("a"), PrimitiveValue.newInt32(1));
        map6.put(PrimitiveValue.newText("b"), PrimitiveValue.newInt32(2));
        DictValue d6 = DictType.of(PrimitiveType.Text, PrimitiveType.Int32).newValueOwn(map6);

        Assert.assertTrue(d1.compareTo(d6) < 0); // {"a": 1} < {"a": 1, "b": 2} (prefix case)
        Assert.assertTrue(d6.compareTo(d1) > 0); // {"a": 1, "b": 2} > {"a": 1}
    }

    @Test
    public void testOptionalValueComparison() {
        OptionalValue opt1 = OptionalValue.of(PrimitiveValue.newInt32(1));
        OptionalValue opt2 = OptionalValue.of(PrimitiveValue.newInt32(2));
        OptionalValue opt3 = OptionalValue.of(PrimitiveValue.newInt32(1));
        OptionalValue opt4 = PrimitiveType.Int32.makeOptional().emptyValue();
        OptionalValue opt5 = OptionalValue.of(PrimitiveValue.newText("abc"));
        OptionalValue opt6 = PrimitiveType.Text.makeOptional().emptyValue();

        Assert.assertTrue(opt1.compareTo(opt2) < 0);
        Assert.assertTrue(opt2.compareTo(opt1) > 0);
        Assert.assertEquals(0, opt1.compareTo(opt3));

        assertNpe("Cannot compare NULL with value 1", () -> opt4.compareTo(opt1));
        assertNpe("Cannot compare value 1 with NULL", () -> opt1.compareTo(opt4));

        assertIllegalArgument("Cannot compare value Int32 with Text", () -> opt1.compareTo(opt5));
        assertIllegalArgument("Cannot compare value Text with Int32", () -> opt5.compareTo(opt1));

        Assert.assertEquals(0, opt4.compareTo(opt6));
        Assert.assertEquals(0, opt6.compareTo(opt4));

        assertNpe("Cannot compare with null value", () -> opt1.compareTo(null));
    }

    @Test
    public void testTupleValueComparison() {
        TupleValue t1 = TupleValue.of(PrimitiveValue.newInt32(1), PrimitiveValue.newInt32(2));
        TupleValue t2 = TupleValue.of(PrimitiveValue.newInt32(1), PrimitiveValue.newInt32(3));
        TupleValue t3 = TupleValue.of(PrimitiveValue.newInt32(1), PrimitiveValue.newInt32(2));
        TupleValue t4 = TupleValue.of(PrimitiveValue.newInt32(1));
        TupleValue t5 = TupleValue.of(PrimitiveValue.newInt32(1), PrimitiveValue.newUint32(2));

        Assert.assertTrue(t1.compareTo(t2) < 0);
        Assert.assertTrue(t2.compareTo(t1) > 0);
        Assert.assertEquals(0, t1.compareTo(t3));
        Assert.assertEquals(0, t1.compareTo(t3.makeOptional()));
        Assert.assertEquals(0, t1.makeOptional().compareTo(t3));

        assertNpe("Cannot compare with null value", () -> t1.compareTo(null));
        assertIllegalArgument("Cannot compare value Tuple<Int32> with Tuple<Int32, Int32>", () -> t4.compareTo(t1));
        assertIllegalArgument("Cannot compare value Tuple<Int32, Uint32> with Tuple<Int32, Int32>",
                () -> t5.compareTo(t1));
    }

    @Test
    public void testVariantValueComparison() {
        VariantType t1 = VariantType.ofOwn(PrimitiveType.Int32, PrimitiveType.Text);
        VariantType t2 = VariantType.ofOwn(PrimitiveType.Int32, PrimitiveType.Text, PrimitiveType.Int32);

        VariantValue v1 = new VariantValue(t1, PrimitiveValue.newInt32(1), 0);
        VariantValue v2 = new VariantValue(t1, PrimitiveValue.newText("abc"), 1);
        VariantValue v3 = new VariantValue(t1, PrimitiveValue.newInt32(1), 0);
        VariantValue v4 = new VariantValue(t1, PrimitiveValue.newText("aBc"), 1);
        VariantValue v5 = new VariantValue(t2, PrimitiveValue.newInt32(1), 0);

        Assert.assertTrue(v1.compareTo(v2) < 0); // type index 0 < 1
        Assert.assertTrue(v2.compareTo(v1) > 0);
        Assert.assertEquals(0, v1.compareTo(v3));
        Assert.assertEquals(0, v3.compareTo(v1));
        Assert.assertTrue(v2.compareTo(v4.makeOptional()) > 0);
        Assert.assertTrue(v4.makeOptional().compareTo(v2) < 0);

        assertNpe("Cannot compare with null value", () -> v1.compareTo(null));
        assertNpe("Cannot compare value Variant[0; 1] with NULL", () -> v1.compareTo(t1.makeOptional().emptyValue()));
        assertNpe("Cannot compare NULL with value Variant[0; 1]", () -> t1.makeOptional().emptyValue().compareTo(v1));

        assertIllegalArgument("Cannot compare value Variant<Int32, Text> with Variant<Int32, Text, Int32>",
                () -> v1.compareTo(v5));
        assertIllegalArgument("Cannot compare value Variant<Int32, Text, Int32> with Variant<Int32, Text>",
                () -> v5.compareTo(v1));
    }

    @Test
    public void testVoidValueComparison() {
        VoidValue void1 = VoidValue.of();
        VoidValue void2 = VoidValue.of();

        Assert.assertEquals(0, void1.compareTo(void2));
        Assert.assertEquals(0, void1.compareTo(void2.makeOptional()));
        Assert.assertEquals(0, void1.makeOptional().compareTo(void2));

        assertNpe("Cannot compare with null value", () -> void1.compareTo(null));
        assertIllegalArgument("Cannot compare value Void with Null", () -> void1.compareTo(NullValue.of()));
    }

    @Test
    public void testNullValueComparison() {
        NullValue null1 = NullValue.of();
        NullValue null2 = NullValue.of();

        Assert.assertEquals(0, null1.compareTo(null2));
        Assert.assertEquals(0, null1.compareTo(null2.makeOptional()));
        Assert.assertEquals(0, null1.makeOptional().compareTo(null2));

        assertNpe("Cannot compare with null value", () -> null1.compareTo(null));
        assertIllegalArgument("Cannot compare value Null with Void", () -> null1.compareTo(VoidValue.of()));
    }

    @Test
    public void testDecimalValueComparison() {
        DecimalValue decimal1 = DecimalValue.fromLong(DecimalType.of(10, 2), 100);
        DecimalValue decimal2 = DecimalValue.fromLong(DecimalType.of(10, 2), 200);
        DecimalValue decimal3 = DecimalValue.fromLong(DecimalType.of(10, 2), 100);

        Assert.assertTrue(decimal1.compareTo(decimal2) < 0);
        Assert.assertTrue(decimal2.compareTo(decimal1) > 0);
        Assert.assertEquals(0, decimal1.compareTo(decimal3));
    }
}