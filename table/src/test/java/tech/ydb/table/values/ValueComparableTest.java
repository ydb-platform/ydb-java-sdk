package tech.ydb.table.values;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;


/**
 * Test for Comparable implementation of Value classes
 */
public class ValueComparableTest {

    private <T> void assertNpe(String message, Comparable<T> one, T other) {
        NullPointerException npe = Assert.assertThrows(NullPointerException.class, () -> one.compareTo(other));
        Assert.assertEquals(message, npe.getMessage());
    }

    private <T> void assertIllegalArgument(String message, Comparable<T> one, T other) {
        IllegalArgumentException ex = Assert.assertThrows(IllegalArgumentException.class, () -> one.compareTo(other));
        Assert.assertEquals(message, ex.getMessage());
    }

    private <T> void assertLess(Comparable<T> one, T other) {
        Assert.assertTrue("" + one + " < " + other + " FAILED", one.compareTo(other) < 0);
    }

    private <T> void assertGreater(Comparable<T> one, T other) {
        Assert.assertTrue("" + one + " > " + other + " FAILED", one.compareTo(other) > 0);
    }

    private <T> void assertEquals(Comparable<T> one, T other) {
        Assert.assertEquals("" + one + " = " + other + " FAILED", 0, one.compareTo(other));
    }

    @Test
    public void testPrimitiveValueComparison() {
        // Test numeric comparisons
        PrimitiveValue int1 = PrimitiveValue.newInt32(1);
        PrimitiveValue int2 = PrimitiveValue.newInt32(2);
        PrimitiveValue int3 = PrimitiveValue.newInt32(1);

        assertLess(int1, int2);
        assertGreater(int2, int1);
        assertEquals(int1, int3);

        // Optional comparing
        assertLess(int1.makeOptional(), int2);
        assertGreater(int2, int1.makeOptional());
        assertEquals(int1.makeOptional(), int3);
        assertEquals(int1, int3.makeOptional());

        // Invalid values
        assertNpe("Cannot compare with null value", int1, null);

        // Test string comparisons
        PrimitiveValue text1 = PrimitiveValue.newText("abc");
        PrimitiveValue text2 = PrimitiveValue.newText("def");
        PrimitiveValue text3 = PrimitiveValue.newText("abc");

        assertLess(text1, text2);
        assertGreater(text2, text1);
        assertEquals(text1, text3);

        // Test boolean comparisons
        PrimitiveValue bool1 = PrimitiveValue.newBool(false);
        PrimitiveValue bool2 = PrimitiveValue.newBool(true);

        assertLess(bool1, bool2);
        assertGreater(bool2, bool1);

        assertIllegalArgument("Cannot compare value Int32 with Text", int1, text1);
        assertNpe("Cannot compare value 1 with NULL", int1, PrimitiveType.Int32.makeOptional().emptyValue());

        // All types check
        assertLess(PrimitiveValue.newInt8(Byte.MIN_VALUE), PrimitiveValue.newInt8(Byte.MAX_VALUE));
        assertLess(PrimitiveValue.newInt16(Short.MIN_VALUE), PrimitiveValue.newInt16(Short.MAX_VALUE));
        assertLess(PrimitiveValue.newInt32(Integer.MIN_VALUE), PrimitiveValue.newInt32(Integer.MAX_VALUE));
        assertLess(PrimitiveValue.newInt64(Long.MIN_VALUE), PrimitiveValue.newInt64(Long.MAX_VALUE));

        assertGreater(PrimitiveValue.newUint8(Byte.MIN_VALUE), PrimitiveValue.newUint8(Byte.MAX_VALUE));
        assertGreater(PrimitiveValue.newUint16(Short.MIN_VALUE), PrimitiveValue.newUint16(Short.MAX_VALUE));
        assertGreater(PrimitiveValue.newUint32(Integer.MIN_VALUE), PrimitiveValue.newUint32(Integer.MAX_VALUE));
        assertGreater(PrimitiveValue.newUint64(Long.MIN_VALUE), PrimitiveValue.newUint64(Long.MAX_VALUE));

        assertLess(PrimitiveValue.newFloat(1e-3f), PrimitiveValue.newFloat(1e-2f));
        assertLess(PrimitiveValue.newDouble(1e-3d), PrimitiveValue.newDouble(1e-2d));

        byte[] b1 = new byte[] { 0x01, 0x02 };
        assertEquals(PrimitiveValue.newBytesOwn(b1), PrimitiveValue.newBytesOwn(b1));
        assertEquals(PrimitiveValue.newBytes(b1), PrimitiveValue.newBytes(new byte[] { 0x01, 0x02 }));
        assertLess(PrimitiveValue.newBytes(b1), PrimitiveValue.newBytes(new byte[] { 0x01, 0x02, 0x1 }));
        assertLess(PrimitiveValue.newBytes(b1), PrimitiveValue.newBytes(new byte[] { 0x02 }));

        assertLess(PrimitiveValue.newYson(b1), PrimitiveValue.newYson(new byte[] { 0x01, 0x03 }));

        assertLess(PrimitiveValue.newText("abc"), PrimitiveValue.newText("abcd"));
        assertLess(PrimitiveValue.newJson("['abc']"), PrimitiveValue.newJson("['abcd']"));
        assertLess(PrimitiveValue.newJsonDocument("['abc']"), PrimitiveValue.newJsonDocument("['abcd']"));

        for (String[] uuid : new String[][] {
            // Sort by       3 2 1 0  5 4  7 6  8 9  A B C D E F
            new String[] { "FFFFFFFE-FFFF-FFFF-FFFF-FFFFFFFFFFFF", "000000FF-0000-0000-0000-000000000000" },
            new String[] { "FFFF3000-FFFF-FFFF-FFFF-FFFFFFFFFFFF", "00003100-0000-0000-0000-000000000000" },
            new String[] { "FF390000-FFFF-FFFF-FFFF-FFFFFFFFFFFF", "00400000-0000-0000-0000-000000000000" },
            new String[] { "00000000-FFFF-FFFF-FFFF-FFFFFFFFFFFF", "01000000-0000-0000-0000-000000000000" },

            new String[] { "00000000-FFFE-FFFF-FFFF-FFFFFFFFFFFF", "00000000-00FF-0000-0000-000000000000" },
            new String[] { "00000000-A000-FFFF-FFFF-FFFFFFFFFFFF", "00000000-A100-0000-0000-000000000000" },
            new String[] { "00000000-0000-FF00-FFFF-FFFFFFFFFFFF", "00000000-0000-0001-0000-000000000000" },
            new String[] { "00000000-0000-0200-FFFF-FFFFFFFFFFFF", "00000000-0000-2000-0000-000000000000" },

            new String[] { "00000000-0000-0000-F0FF-FFFFFFFFFFFF", "00000000-0000-0000-F100-000000000000" },
            new String[] { "00000000-0000-0000-00FE-FFFFFFFFFFFF", "00000000-0000-0000-00FF-000000000000" },

            new String[] { "00000000-0000-0000-0000-FEFFFFFFFFFF", "00000000-0000-0000-0000-FF0000000000" },
            new String[] { "00000000-0000-0000-0000-00ABFFFFFFFF", "00000000-0000-0000-0000-00AC00000000" },
            new String[] { "00000000-0000-0000-0000-000050FFFFFF", "00000000-0000-0000-0000-000060000000" },
            new String[] { "00000000-0000-0000-0000-00000045FFFF", "00000000-0000-0000-0000-000004600000" },
            new String[] { "00000000-0000-0000-0000-0000000012FF", "00000000-0000-0000-0000-000000001300" },
            new String[] { "00000000-0000-0000-0000-000000000000", "00000000-0000-0000-0000-000000000001" },
        }) {
            assertLess(PrimitiveValue.newUuid(uuid[0]), PrimitiveValue.newUuid(uuid[1]));
        }

        assertLess(PrimitiveValue.newDate(20000), PrimitiveValue.newDate(20001));
        assertLess(PrimitiveValue.newDatetime(1728000000), PrimitiveValue.newDatetime(1728000001));
        assertLess(
                PrimitiveValue.newTimestamp(Instant.ofEpochSecond(1728000000, 123456000)),
                PrimitiveValue.newTimestamp(Instant.ofEpochSecond(1728000000, 123457000))
        );
        assertLess(PrimitiveValue.newInterval(20000), PrimitiveValue.newInterval(20001));

        assertLess(PrimitiveValue.newDate32(20000), PrimitiveValue.newDate32(20001));
        assertLess(PrimitiveValue.newDatetime64(1728000000), PrimitiveValue.newDatetime64(1728000001));
        assertLess(
                PrimitiveValue.newTimestamp64(Instant.ofEpochSecond(1728000000, 123456000)),
                PrimitiveValue.newTimestamp64(Instant.ofEpochSecond(1728000000, 123457000))
        );
        assertLess(PrimitiveValue.newInterval64(20000), PrimitiveValue.newInterval64(20001));
    }

    @Test
    public void testListValueComparison() {
        ListValue list1 = ListValue.of(PrimitiveValue.newInt32(1), PrimitiveValue.newInt32(2));
        ListValue list2 = ListValue.of(
                PrimitiveValue.newInt32(1).makeOptional(),
                PrimitiveValue.newInt32(2).makeOptional()
        );

        assertEquals(list1, list2);
        assertEquals(list1.makeOptional(), list2);
        assertEquals(list1, list2.makeOptional());

        ListValue list3 = ListValue.of(PrimitiveValue.newInt32(1), PrimitiveValue.newInt32(3));
        ListValue list4 = ListValue.of(PrimitiveValue.newInt32(2), PrimitiveValue.newInt32(2));

        assertLess(list1, list3);
        assertLess(list2, list3);
        assertGreater(list4, list3);
        assertLess(list3, list4);

        ListValue list5 = ListValue.of(PrimitiveValue.newText("A"), PrimitiveValue.newText("Z"));
        ListValue list6 = ListValue.of(PrimitiveValue.newText("A"), PrimitiveValue.newText("Z"));
        ListValue list7 = ListValue.of(PrimitiveValue.newText("A"));
        ListValue list8 = ListValue.of(PrimitiveValue.newText("Z"));

        assertEquals(list5, list6);
        assertEquals(list6, list5);
        assertLess(list7, list5); // shorter list comes first

        // Test proper lexicographical ordering

        // ('Z') should be "bigger" than ('A','Z') in lexicographical order
        assertLess(list5, list8); // ('A','Z') < ('Z')
        assertGreater(list8, list5); // ('Z') > ('A','Z')

        assertNpe("Cannot compare with null value", list1, null);
        assertNpe("Cannot compare value List[1, 2] with NULL", list1, list1.getType().makeOptional().emptyValue());
        assertIllegalArgument("Cannot compare value Int32 with Text", list1, list5);
        assertIllegalArgument("Cannot compare value Int32 with Text", list2, list5);
    }

    @Test
    public void testStructValueComparison() {
        StructValue s1 = StructValue.of("a", PrimitiveValue.newInt32(1), "b", PrimitiveValue.newInt32(2));
        StructValue s2 = StructValue.of("a", PrimitiveValue.newInt32(1), "b", PrimitiveValue.newInt32(2));
        StructValue s3 = StructValue.of("a", PrimitiveValue.newInt32(2), "b", PrimitiveValue.newInt32(1));
        StructValue s4 = StructValue.of("a", PrimitiveValue.newInt32(1), "b", PrimitiveValue.newText("a"));
        StructValue s5 = StructValue.of("a", PrimitiveValue.newInt32(1));

        assertEquals(s1, s2);
        assertEquals(s1, s2.makeOptional());
        assertEquals(s1.makeOptional(), s2);

        assertLess(s1, s3);
        assertGreater(s3, s1);

        assertNpe("Cannot compare with null value", s1, null);
        assertNpe("Cannot compare value Struct[1, 2] with NULL", s1, s1.getType().makeOptional().emptyValue());
        assertIllegalArgument("Cannot compare value Struct<'a': Int32, 'b': Int32> with Struct<'a': Int32, 'b': Text>",
                s1, s4);
        assertIllegalArgument("Cannot compare value Struct<'a': Int32, 'b': Int32> with Struct<'a': Int32>", s1, s5);
    }

    @Test
    public void testDictValueComparison() {
        DictValue d1 = DictValue.of(PrimitiveValue.newText("a"), PrimitiveValue.newInt32(1));
        DictValue d2 = DictValue.of(PrimitiveValue.newText("a"), PrimitiveValue.newInt32(2));
        DictValue d3 = DictValue.of(PrimitiveValue.newText("a"), PrimitiveValue.newInt32(1));

        assertLess(d1, d2);
        assertGreater(d2, d1);

        assertEquals(d1, d3);
        assertEquals(d1, d3.makeOptional());
        assertEquals(d1.makeOptional(), d3);

        DictValue d4 = DictValue.of(PrimitiveValue.newText("a"), PrimitiveValue.newText("abc"));
        assertIllegalArgument("Cannot compare value Dict<Text, Int32> with Dict<Text, Text>", d1, d4);
        assertIllegalArgument("Cannot compare value Dict<Text, Text> with Dict<Text, Int32>", d4, d1);

        DictValue d5 = DictValue.of(PrimitiveValue.newText("b"), PrimitiveValue.newInt32(1));

        // {"a": 1} should be "bigger" than {"b": 1 } in lexicographical order
        assertGreater(d1, d5);
        assertLess(d5, d1);

        Map<Value<?>, Value<?>> map6 = new HashMap<>();
        map6.put(PrimitiveValue.newText("a"), PrimitiveValue.newInt32(1));
        map6.put(PrimitiveValue.newText("b"), PrimitiveValue.newInt32(2));
        DictValue d6 = DictType.of(PrimitiveType.Text, PrimitiveType.Int32).newValueOwn(map6);

        assertLess(d1, d6); // {"a": 1} < {"a": 1, "b": 2} (prefix case)
        assertGreater(d6, d1); // {"a": 1, "b": 2} > {"a": 1}

        assertNpe("Cannot compare with null value", d1, null);
        assertNpe("Cannot compare value Dict[\"a\": 1] with NULL", d1, d1.getType().makeOptional().emptyValue());
    }

    @Test
    public void testOptionalValueComparison() {
        OptionalValue opt1 = OptionalValue.of(PrimitiveValue.newInt32(1));
        OptionalValue opt2 = OptionalValue.of(PrimitiveValue.newInt32(2));
        OptionalValue opt3 = OptionalValue.of(PrimitiveValue.newInt32(1));
        OptionalValue opt4 = PrimitiveType.Int32.makeOptional().emptyValue();
        OptionalValue opt5 = OptionalValue.of(PrimitiveValue.newText("abc"));
        OptionalValue opt6 = PrimitiveType.Text.makeOptional().emptyValue();

        assertLess(opt1, opt2);
        assertGreater(opt2, opt1);
        assertEquals(opt1, opt3);

        assertNpe("Cannot compare NULL with value 1", opt4, opt1);
        assertNpe("Cannot compare value 1 with NULL", opt1, opt4);

        assertIllegalArgument("Cannot compare value Int32 with Text", opt1, opt5);
        assertIllegalArgument("Cannot compare value Text with Int32", opt5, opt1);

        assertEquals(opt4, opt6);
        assertEquals(opt6, opt4);

        assertNpe("Cannot compare with null value", opt1, null);
    }

    @Test
    public void testTupleValueComparison() {
        TupleValue t1 = TupleValue.of(PrimitiveValue.newInt32(1), PrimitiveValue.newInt32(2));
        TupleValue t2 = TupleValue.of(PrimitiveValue.newInt32(1), PrimitiveValue.newInt32(3));
        TupleValue t3 = TupleValue.of(PrimitiveValue.newInt32(1), PrimitiveValue.newInt32(2));
        TupleValue t4 = TupleValue.of(PrimitiveValue.newInt32(1));
        TupleValue t5 = TupleValue.of(PrimitiveValue.newInt32(1), PrimitiveValue.newUint32(2));

        assertLess(t1, t2);
        assertGreater(t2, t1);
        assertEquals(t1, t3);
        assertEquals(t1, t3.makeOptional());
        assertEquals(t1.makeOptional(), t3);

        assertNpe("Cannot compare with null value", t1, null);
        assertNpe("Cannot compare value Tuple[1, 2] with NULL", t1, t1.getType().makeOptional().emptyValue());
        assertIllegalArgument("Cannot compare value Tuple<Int32> with Tuple<Int32, Int32>", t4, t1);
        assertIllegalArgument("Cannot compare value Tuple<Int32, Uint32> with Tuple<Int32, Int32>", t5, t1);
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

        assertLess(v1, v2); // type index 0 < 1
        assertGreater(v2, v1);
        assertEquals(v1, v3);
        assertEquals(v3, v1);
        assertGreater(v2, v4.makeOptional());
        assertLess(v4.makeOptional(), v2);

        assertNpe("Cannot compare with null value", v1, null);
        assertNpe("Cannot compare value Variant[0; 1] with NULL", v1, t1.makeOptional().emptyValue());
        assertNpe("Cannot compare NULL with value Variant[0; 1]", t1.makeOptional().emptyValue(), v1);

        assertIllegalArgument("Cannot compare value Variant<Int32, Text> with Variant<Int32, Text, Int32>", v1, v5);
        assertIllegalArgument("Cannot compare value Variant<Int32, Text, Int32> with Variant<Int32, Text>", v5, v1);
    }

    @Test
    public void testVoidValueComparison() {
        VoidValue void1 = VoidValue.of();
        VoidValue void2 = VoidValue.of();

        assertEquals(void1, void2);
        assertEquals(void1, void2.makeOptional());
        assertEquals(void1.makeOptional(), void2);

        assertEquals(void1, NullValue.of());
        assertEquals(void1, NullValue.of().makeOptional());
        assertEquals(void1, PrimitiveType.Int32.makeOptional().emptyValue());

        assertNpe("Cannot compare with null value", void1, null);
        assertIllegalArgument("Cannot compare value Void with Int32", void1, PrimitiveValue.newInt32(1));
    }

    @Test
    public void testNullValueComparison() {
        NullValue null1 = NullValue.of();
        NullValue null2 = NullValue.of();

        assertEquals(null1, null2);
        assertEquals(null1, null2.makeOptional());
        assertEquals(null1.makeOptional(), null2);

        assertEquals(null1, VoidValue.of());
        assertEquals(null1, VoidValue.of().makeOptional());
        assertEquals(null1, PrimitiveType.Int32.makeOptional().emptyValue());

        assertNpe("Cannot compare with null value", null1, null);
        assertIllegalArgument("Cannot compare value Null with Int32", null1, PrimitiveValue.newInt32(1));
    }

    @Test
    public void testDecimalValueComparison() {
        DecimalType t1 = DecimalType.of(33, 2);
        DecimalType t2 = DecimalType.of(30, 9);
        DecimalType t3 = DecimalType.of(11, 2);

        assertEquals(t1.newValue("1"), t2.newValue("1"));
        assertEquals(t1.newValue("1.23").makeOptional(), t2.newValue("1.23"));
        assertEquals(t1.newValue("-1.23"), t2.newValue("-1.23").makeOptional());

        // the same scale
        assertEquals(t3.newValue("999999999.99"), t1.newValue("999999999.99"));
        assertEquals(t3.newValue("-999999999.99"), t1.newValue("-999999999.99"));
        assertLess(t3.newValue("999999999.98"), t1.newValue("999999999.99"));
        assertLess(t3.newValue("899999999.99"), t1.newValue("999999999.99"));
        assertGreater(t3.newValue("-999999999.98"), t1.newValue("-999999999.99"));
        assertGreater(t3.newValue("-899999999.99"), t1.newValue("-999999999.99"));

        // the differnt scales
        assertEquals(t3.newValue("999999999.99"), t2.newValue("999999999.99"));
        assertEquals(t3.newValue("-999999999.99"), t2.newValue("-999999999.99"));
        assertLess(t3.newValue("999999999.99"), t2.newValue("1000000000"));
        assertGreater(t3.newValue("-999999999.99"), t2.newValue("-1000000000"));
        assertLess(t1.newValue("1.23"), t2.newValue("1.234"));
        assertGreater(t1.newValue("-1.23"), t2.newValue("-1.234"));

        // special values
        assertEquals(t1.getInf(), t2.getInf());
        assertEquals(t1.getNegInf(), t2.getNegInf());
        assertEquals(t1.getNaN(), t2.getNaN());

        // type bounds
        assertLess(t1.getNegInf(), t2.newValue("-999999999999999999999.999999999"));
        assertGreater(t2.newValue("-999999999999999999999.999999999"), t1.getNegInf());
        assertEquals(t1.getNegInf(), t2.newValue("-1000000000000000000000"));

        assertGreater(t1.getInf(), t2.newValue("999999999999999999999.999999999"));
        assertLess(t2.newValue("-999999999999999999999.999999999"), t1.getInf());
        assertEquals(t1.getInf(), t2.newValue("1000000000000000000000"));

        assertGreater(t1.getNaN(), t2.newValue("999999999999999999999.999999999"));
        assertGreater(t1.getNaN(), t2.newValue("9000000000000000000000"));

        // errors
        assertNpe("Cannot compare with null value", t1.newValue("1"), null);
        assertNpe("Cannot compare value 1.00 with NULL", t1.newValue("1"), t1.makeOptional().emptyValue());
        assertIllegalArgument("Cannot compare DecimalValue with Int32", t1.newValue("1"), PrimitiveValue.newInt32(1));
    }
}