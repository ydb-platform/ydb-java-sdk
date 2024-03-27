package tech.ydb.table.values;

import com.google.common.collect.ImmutableMap;
import org.junit.Assert;
import org.junit.Test;

import tech.ydb.proto.ValueProtos;
import tech.ydb.table.values.proto.ProtoValue;


/**
 * @author Sergey Polovko
 */
public class StructValueTest {

    private final StructType employeeType = StructType.of(
        "name", PrimitiveType.Text,
        "age", PrimitiveType.Uint32,
        "salary", PrimitiveType.Double);

    @Test
    public void newInstanceWithIndexes() {
        int nameIdx = employeeType.getMemberIndex("name");
        int ageIdx = employeeType.getMemberIndex("age");
        int salaryIdx = employeeType.getMemberIndex("salary");

        Value<?>[] members = new Value<?>[employeeType.getMembersCount()];
        members[nameIdx] = PrimitiveValue.newText("William");
        members[ageIdx] = PrimitiveValue.newUint32(99);
        members[salaryIdx] = PrimitiveValue.newDouble(1234.56);

        StructValue employee = employeeType.newValueUnsafe(members);

        Assert.assertEquals(employeeType.getMembersCount(), employee.getMembersCount());

        Assert.assertTrue(PrimitiveValue.newText("William").equals(employee.getMemberValue(nameIdx)));
        Assert.assertTrue(PrimitiveValue.newUint32(99).equals(employee.getMemberValue(ageIdx)));
        Assert.assertTrue(PrimitiveValue.newDouble(1234.56).equals(employee.getMemberValue(salaryIdx)));
    }

    @Test
    public void newInstanceWithNames() {
        StructValue employee = employeeType.newValue(ImmutableMap.of(
            "age", PrimitiveValue.newUint32(99),
            "salary", PrimitiveValue.newDouble(1234.56),
            "name", PrimitiveValue.newText("William")));

        Assert.assertEquals(employeeType.getMembersCount(), employee.getMembersCount());

        Assert.assertTrue(PrimitiveValue.newText("William")
                .equals(employee.getMemberValue(employeeType.getMemberIndex("name"))));
        Assert.assertTrue(PrimitiveValue.newUint32(99)
                .equals(employee.getMemberValue(employeeType.getMemberIndex("age"))));
        Assert.assertTrue(PrimitiveValue.newDouble(1234.56)
                .equals(employee.getMemberValue(employeeType.getMemberIndex("salary"))));
    }

    @Test
    public void oneMemberProtobuf() {
        StructValue value = StructValue.of("a", PrimitiveValue.newUint32(1));
        StructType type = value.getType();

        ValueProtos.Value valuePb = value.toPb();
        Assert.assertEquals(ValueProtos.Value.newBuilder()
                .addItems(ProtoValue.fromUint32(1))
                .build(), valuePb);

        Value<?> valueX = ProtoValue.fromPb(type, valuePb);
        Assert.assertTrue(value.equals(valueX));
    }

    @Test
    public void manyMembersProtobuf() {
        StructValue value = StructValue.of(
            "a", PrimitiveValue.newUint32(1),
            "b", PrimitiveValue.newBool(true),
            "c", PrimitiveValue.newText("yes"));
        StructType type = value.getType();

        ValueProtos.Value valuePb = value.toPb();
        Assert.assertEquals(ValueProtos.Value.newBuilder()
                .addItems(ProtoValue.fromUint32(1))
                .addItems(ProtoValue.fromBool(true))
                .addItems(ProtoValue.fromText("yes"))
                .build(), valuePb);

        Value<?> valueX = ProtoValue.fromPb(type, valuePb);
        Assert.assertTrue(valueX.equals(value));
    }

    @Test
    public void toStr() {
        StructValue value1 = StructValue.of("a", PrimitiveValue.newUint32(1));
        Assert.assertEquals("Struct[1]", value1.toString());

        StructValue value2 = StructValue.of(
            "a", PrimitiveValue.newUint32(1),
            "b", PrimitiveValue.newBool(true),
            "c", PrimitiveValue.newText("yes"));
        Assert.assertEquals("Struct[1, true, \"yes\"]", value2.toString());
    }
}
