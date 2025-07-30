package tech.ydb.table.values;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Test for Comparable implementation of Value classes
 */
public class ValueComparableTest {

    @Test
    public void testPrimitiveValueComparison() {
        // Test numeric comparisons
        PrimitiveValue int1 = PrimitiveValue.newInt32(1);
        PrimitiveValue int2 = PrimitiveValue.newInt32(2);
        PrimitiveValue int3 = PrimitiveValue.newInt32(1);
        
        assertTrue(int1.compareTo(int2) < 0);
        assertTrue(int2.compareTo(int1) > 0);
        assertEquals(0, int1.compareTo(int3));
        
        // Test string comparisons
        PrimitiveValue text1 = PrimitiveValue.newText("abc");
        PrimitiveValue text2 = PrimitiveValue.newText("def");
        PrimitiveValue text3 = PrimitiveValue.newText("abc");
        
        assertTrue(text1.compareTo(text2) < 0);
        assertTrue(text2.compareTo(text1) > 0);
        assertEquals(0, text1.compareTo(text3));
        
        // Test boolean comparisons
        PrimitiveValue bool1 = PrimitiveValue.newBool(false);
        PrimitiveValue bool2 = PrimitiveValue.newBool(true);
        
        assertTrue(bool1.compareTo(bool2) < 0);
        assertTrue(bool2.compareTo(bool1) > 0);
    }

    @Test
    public void testListValueComparison() {
        ListValue list1 = ListValue.of(PrimitiveValue.newInt32(1), PrimitiveValue.newInt32(2));
        ListValue list2 = ListValue.of(PrimitiveValue.newInt32(1), PrimitiveValue.newInt32(3));
        ListValue list3 = ListValue.of(PrimitiveValue.newInt32(1), PrimitiveValue.newInt32(2));
        ListValue list4 = ListValue.of(PrimitiveValue.newInt32(1));
        
        assertTrue(list1.compareTo(list2) < 0);
        assertTrue(list2.compareTo(list1) > 0);
        assertEquals(0, list1.compareTo(list3));
        assertTrue(list4.compareTo(list1) < 0); // shorter list comes first
    }

    @Test
    public void testListValueLexicographical() {
        // Test proper lexicographical ordering
        ListValue list1 = ListValue.of(PrimitiveValue.newText("A"), PrimitiveValue.newText("Z"));
        ListValue list2 = ListValue.of(PrimitiveValue.newText("Z"));
        
        // ('Z') should be "bigger" than ('A','Z') in lexicographical order
        assertTrue(list1.compareTo(list2) < 0); // ('A','Z') < ('Z')
        assertTrue(list2.compareTo(list1) > 0); // ('Z') > ('A','Z')
        
        // Test prefix ordering
        ListValue list3 = ListValue.of(PrimitiveValue.newText("A"));
        ListValue list4 = ListValue.of(PrimitiveValue.newText("A"), PrimitiveValue.newText("B"));
        
        assertTrue(list3.compareTo(list4) < 0); // ('A') < ('A','B')
        assertTrue(list4.compareTo(list3) > 0); // ('A','B') > ('A')
    }

    @Test(expected = IllegalArgumentException.class)
    public void testListValueDifferentTypes() {
        ListValue list1 = ListValue.of(PrimitiveValue.newInt32(1));
        ListValue list2 = ListValue.of(PrimitiveValue.newText("abc"));
        list1.compareTo(list2); // Should throw exception for different element types
    }

    @Test
    public void testStructValueComparison() {
        StructValue struct1 = StructValue.of("a", PrimitiveValue.newInt32(1), "b", PrimitiveValue.newInt32(2));
        StructValue struct2 = StructValue.of("a", PrimitiveValue.newInt32(1), "b", PrimitiveValue.newInt32(3));
        StructValue struct3 = StructValue.of("a", PrimitiveValue.newInt32(1), "b", PrimitiveValue.newInt32(2));
        
        assertTrue(struct1.compareTo(struct2) < 0);
        assertTrue(struct2.compareTo(struct1) > 0);
        assertEquals(0, struct1.compareTo(struct3));
    }

    @Test
    public void testStructValueLexicographical() {
        // Test proper lexicographical ordering
        StructValue struct1 = StructValue.of("a", PrimitiveValue.newText("A"), "b", PrimitiveValue.newText("Z"));
        StructValue struct2 = StructValue.of("a", PrimitiveValue.newText("Z"));
        
        // ('Z') should be "bigger" than ('A','Z') in lexicographical order
        assertTrue(struct1.compareTo(struct2) < 0); // ('A','Z') < ('Z')
        assertTrue(struct2.compareTo(struct1) > 0); // ('Z') > ('A','Z')
    }

    @Test(expected = IllegalArgumentException.class)
    public void testStructValueDifferentTypes() {
        StructValue struct1 = StructValue.of("a", PrimitiveValue.newInt32(1));
        StructValue struct2 = StructValue.of("a", PrimitiveValue.newText("abc"));
        struct1.compareTo(struct2); // Should throw exception for different member types
    }

    @Test
    public void testDictValueComparison() {
        DictValue dict1 = DictValue.of(PrimitiveValue.newText("a"), PrimitiveValue.newInt32(1));
        DictValue dict2 = DictValue.of(PrimitiveValue.newText("b"), PrimitiveValue.newInt32(1));
        DictValue dict3 = DictValue.of(PrimitiveValue.newText("a"), PrimitiveValue.newInt32(1));
        
        assertTrue(dict1.compareTo(dict2) < 0);
        assertTrue(dict2.compareTo(dict1) > 0);
        assertEquals(0, dict1.compareTo(dict3));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testDictValueDifferentTypes() {
        DictValue dict1 = DictValue.of(PrimitiveValue.newText("a"), PrimitiveValue.newInt32(1));
        DictValue dict2 = DictValue.of(PrimitiveValue.newText("a"), PrimitiveValue.newText("abc"));
        dict1.compareTo(dict2); // Should throw exception for different value types
    }

    @Test
    public void testOptionalValueComparison() {
        OptionalValue opt1 = OptionalValue.of(PrimitiveValue.newInt32(1));
        OptionalValue opt2 = OptionalValue.of(PrimitiveValue.newInt32(2));
        OptionalValue opt3 = OptionalValue.of(PrimitiveValue.newInt32(1));
        OptionalValue opt4 = PrimitiveType.Int32.makeOptional().emptyValue();
        
        assertTrue(opt1.compareTo(opt2) < 0);
        assertTrue(opt2.compareTo(opt1) > 0);
        assertEquals(0, opt1.compareTo(opt3));
        assertTrue(opt4.compareTo(opt1) < 0); // empty values come first
    }

    @Test(expected = IllegalArgumentException.class)
    public void testOptionalValueDifferentTypes() {
        OptionalValue opt1 = OptionalValue.of(PrimitiveValue.newInt32(1));
        OptionalValue opt2 = OptionalValue.of(PrimitiveValue.newText("abc"));
        opt1.compareTo(opt2); // Should throw exception for different item types
    }

    @Test
    public void testOptionalValueWithNonOptional() {
        OptionalValue opt1 = OptionalValue.of(PrimitiveValue.newInt32(1));
        OptionalValue opt2 = OptionalValue.of(PrimitiveValue.newInt32(2));
        OptionalValue opt3 = PrimitiveType.Int32.makeOptional().emptyValue();
        PrimitiveValue prim1 = PrimitiveValue.newInt32(1);
        PrimitiveValue prim2 = PrimitiveValue.newInt32(2);
        
        // Optional with non-optional of same type
        assertEquals(0, opt1.compareTo(prim1)); // Same value
        assertTrue(opt1.compareTo(prim2) < 0); // Optional value less than non-optional
        assertTrue(opt2.compareTo(prim1) > 0); // Optional value greater than non-optional
        
        // Empty optional with non-optional
        assertTrue(opt3.compareTo(prim1) < 0); // Empty < non-empty
        
        // Non-optional with optional
        assertEquals(0, prim1.compareTo(opt1)); // Same value
        assertTrue(prim1.compareTo(opt2) < 0); // Non-optional less than optional
        assertTrue(prim2.compareTo(opt1) > 0); // Non-optional greater than optional
        assertTrue(prim1.compareTo(opt3) > 0); // Non-empty > empty
    }

    @Test(expected = IllegalArgumentException.class)
    public void testOptionalValueWithIncompatibleType() {
        OptionalValue opt1 = OptionalValue.of(PrimitiveValue.newInt32(1));
        PrimitiveValue prim1 = PrimitiveValue.newText("abc");
        opt1.compareTo(prim1); // Should throw exception for incompatible types
    }

    @Test
    public void testTupleValueComparison() {
        TupleValue tuple1 = TupleValue.of(PrimitiveValue.newInt32(1), PrimitiveValue.newInt32(2));
        TupleValue tuple2 = TupleValue.of(PrimitiveValue.newInt32(1), PrimitiveValue.newInt32(3));
        TupleValue tuple3 = TupleValue.of(PrimitiveValue.newInt32(1), PrimitiveValue.newInt32(2));
        TupleValue tuple4 = TupleValue.of(PrimitiveValue.newInt32(1));
        
        assertTrue(tuple1.compareTo(tuple2) < 0);
        assertTrue(tuple2.compareTo(tuple1) > 0);
        assertEquals(0, tuple1.compareTo(tuple3));
        assertTrue(tuple4.compareTo(tuple1) < 0); // shorter tuple comes first
    }

    @Test
    public void testTupleValueLexicographical() {
        // Test proper lexicographical ordering
        TupleValue tuple1 = TupleValue.of(PrimitiveValue.newText("A"), PrimitiveValue.newText("Z"));
        TupleValue tuple2 = TupleValue.of(PrimitiveValue.newText("Z"));
        
        // ('Z') should be "bigger" than ('A','Z') in lexicographical order
        assertTrue(tuple1.compareTo(tuple2) < 0); // ('A','Z') < ('Z')
        assertTrue(tuple2.compareTo(tuple1) > 0); // ('Z') > ('A','Z')
    }

    @Test(expected = IllegalArgumentException.class)
    public void testTupleValueDifferentTypes() {
        TupleValue tuple1 = TupleValue.of(PrimitiveValue.newInt32(1));
        TupleValue tuple2 = TupleValue.of(PrimitiveValue.newText("abc"));
        tuple1.compareTo(tuple2); // Should throw exception for different element types
    }

    @Test
    public void testVariantValueComparison() {
        VariantValue variant1 = new VariantValue(VariantType.ofOwn(PrimitiveType.Int32, PrimitiveType.Text), 
                                                 PrimitiveValue.newInt32(1), 0);
        VariantValue variant2 = new VariantValue(VariantType.ofOwn(PrimitiveType.Int32, PrimitiveType.Text), 
                                                 PrimitiveValue.newText("abc"), 1);
        VariantValue variant3 = new VariantValue(VariantType.ofOwn(PrimitiveType.Int32, PrimitiveType.Text), 
                                                 PrimitiveValue.newInt32(2), 0);
        
        assertTrue(variant1.compareTo(variant2) < 0); // type index 0 < 1
        assertTrue(variant2.compareTo(variant1) > 0);
        assertTrue(variant1.compareTo(variant3) < 0); // same type index, compare values
    }

    @Test(expected = IllegalArgumentException.class)
    public void testVariantValueDifferentTypes() {
        VariantValue variant1 = new VariantValue(VariantType.ofOwn(PrimitiveType.Int32, PrimitiveType.Text), 
                                                 PrimitiveValue.newInt32(1), 0);
        VariantValue variant2 = new VariantValue(VariantType.ofOwn(PrimitiveType.Int32, PrimitiveType.Text), 
                                                 PrimitiveValue.newText("abc"), 0);
        variant1.compareTo(variant2); // Should throw exception for different item types
    }

    @Test
    public void testVoidValueComparison() {
        VoidValue void1 = VoidValue.of();
        VoidValue void2 = VoidValue.of();
        
        assertEquals(0, void1.compareTo(void2));
    }

    @Test
    public void testNullValueComparison() {
        NullValue null1 = NullValue.of();
        NullValue null2 = NullValue.of();
        
        assertEquals(0, null1.compareTo(null2));
    }

    @Test
    public void testDecimalValueComparison() {
        DecimalValue decimal1 = DecimalValue.fromLong(DecimalType.of(10, 2), 100);
        DecimalValue decimal2 = DecimalValue.fromLong(DecimalType.of(10, 2), 200);
        DecimalValue decimal3 = DecimalValue.fromLong(DecimalType.of(10, 2), 100);
        
        assertTrue(decimal1.compareTo(decimal2) < 0);
        assertTrue(decimal2.compareTo(decimal1) > 0);
        assertEquals(0, decimal1.compareTo(decimal3));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCompareWithNull() {
        PrimitiveValue value = PrimitiveValue.newInt32(1);
        value.compareTo(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCompareDifferentTypes() {
        PrimitiveValue intValue = PrimitiveValue.newInt32(1);
        PrimitiveValue textValue = PrimitiveValue.newText("abc");
        intValue.compareTo(textValue);
    }
} 