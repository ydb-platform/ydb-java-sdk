package tech.ydb.table.values;

import com.google.common.collect.ImmutableMap;
import com.google.common.truth.extensions.proto.ProtoTruth;
import tech.ydb.ValueProtos;
import tech.ydb.table.values.proto.ProtoValue;
import org.junit.Test;

import static com.google.common.truth.Truth.assertThat;


/**
 * @author Sergey Polovko
 */
public class StructValueTest {

    private final StructType employeeType = StructType.of(
        "name", PrimitiveType.Utf8,
        "age", PrimitiveType.Uint32,
        "salary", PrimitiveType.Double);

    @Test
    public void newInstanceWithIndexes() {
        int nameIdx = employeeType.getMemberIndex("name");
        int ageIdx = employeeType.getMemberIndex("age");
        int salaryIdx = employeeType.getMemberIndex("salary");

        Value<?>[] members = new Value<?>[employeeType.getMembersCount()];
        members[nameIdx] = PrimitiveValue.newUtf8("William");
        members[ageIdx] = PrimitiveValue.newUint32(99);
        members[salaryIdx] = PrimitiveValue.newDouble(1234.56);

        StructValue employee = employeeType.newValueUnsafe(members);

        assertThat(employee.getMembersCount())
            .isEqualTo(employeeType.getMembersCount());

        assertThat(employee.getMemberValue(nameIdx))
            .isEqualTo(PrimitiveValue.newUtf8("William"));
        assertThat(employee.getMemberValue(ageIdx))
            .isEqualTo(PrimitiveValue.newUint32(99));
        assertThat(employee.getMemberValue(salaryIdx))
            .isEqualTo(PrimitiveValue.newDouble(1234.56));
    }

    @Test
    public void newInstanceWithNames() {
        StructValue employee = employeeType.newValue(ImmutableMap.of(
            "age", PrimitiveValue.newUint32(99),
            "salary", PrimitiveValue.newDouble(1234.56),
            "name", PrimitiveValue.newUtf8("William")));

        assertThat(employee.getMembersCount())
            .isEqualTo(employeeType.getMembersCount());

        assertThat(employee.getMemberValue(employeeType.getMemberIndex("name")))
            .isEqualTo(PrimitiveValue.newUtf8("William"));
        assertThat(employee.getMemberValue(employeeType.getMemberIndex("age")))
            .isEqualTo(PrimitiveValue.newUint32(99));
        assertThat(employee.getMemberValue(employeeType.getMemberIndex("salary")))
            .isEqualTo(PrimitiveValue.newDouble(1234.56));
    }

    @Test
    public void oneMemberProtobuf() {
        StructValue value = StructValue.of("a", PrimitiveValue.newUint32(1));
        StructType type = value.getType();

        ValueProtos.Value valuePb = value.toPb();
        ProtoTruth.assertThat(valuePb)
            .isEqualTo(ValueProtos.Value.newBuilder()
                .addItems(ProtoValue.fromUint32(1))
                .build());

        Value<?> valueX = ProtoValue.fromPb(type, valuePb);
        assertThat(valueX).isEqualTo(value);
    }

    @Test
    public void manyMembersProtobuf() {
        StructValue value = StructValue.of(
            "a", PrimitiveValue.newUint32(1),
            "b", PrimitiveValue.newBool(true),
            "c", PrimitiveValue.newUtf8("yes"));
        StructType type = value.getType();

        ValueProtos.Value valuePb = value.toPb();
        ProtoTruth.assertThat(valuePb)
            .isEqualTo(ValueProtos.Value.newBuilder()
                .addItems(ProtoValue.fromUint32(1))
                .addItems(ProtoValue.fromBool(true))
                .addItems(ProtoValue.fromUtf8("yes"))
                .build());

        Value<?> valueX = ProtoValue.fromPb(type, valuePb);
        assertThat(valueX).isEqualTo(value);
    }

    @Test
    public void toStr() {
        StructValue value1 = StructValue.of("a", PrimitiveValue.newUint32(1));
        assertThat(value1.toString())
            .isEqualTo("Struct[1]");

        StructValue value2 = StructValue.of(
            "a", PrimitiveValue.newUint32(1),
            "b", PrimitiveValue.newBool(true),
            "c", PrimitiveValue.newUtf8("yes"));
        assertThat(value2.toString())
            .isEqualTo("Struct[1, true, \"yes\"]");
    }
}
