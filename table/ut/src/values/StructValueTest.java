package ru.yandex.ydb.table.values;

import com.google.common.collect.ImmutableMap;
import com.google.common.truth.extensions.proto.ProtoTruth;
import org.junit.Test;

import ru.yandex.ydb.ValueProtos;
import ru.yandex.ydb.table.types.PrimitiveType;
import ru.yandex.ydb.table.types.StructType;
import ru.yandex.ydb.table.values.proto.ProtoValue;

import static com.google.common.truth.Truth.assertThat;


/**
 * @author Sergey Polovko
 */
public class StructValueTest {

    private final StructType employeeType = StructType.of(
        "name", PrimitiveType.utf8(),
        "age", PrimitiveType.uint32(),
        "salary", PrimitiveType.float64());

    @Test
    public void newInstanceWithIndexes() {
        int nameIdx = employeeType.getMemberIndex("name");
        int ageIdx = employeeType.getMemberIndex("age");
        int salaryIdx = employeeType.getMemberIndex("salary");

        Value[] members = new Value[employeeType.getMembersCount()];
        members[nameIdx] = PrimitiveValue.utf8("William");
        members[ageIdx] = PrimitiveValue.uint32(99);
        members[salaryIdx] = PrimitiveValue.float64(1234.56);

        StructValue employee = StructValue.ofOwn(members);

        assertThat(employee.getMembersCount())
            .isEqualTo(employeeType.getMembersCount());

        assertThat(employee.getMemberValue(nameIdx))
            .isEqualTo(PrimitiveValue.utf8("William"));
        assertThat(employee.getMemberValue(ageIdx))
            .isEqualTo(PrimitiveValue.uint32(99));
        assertThat(employee.getMemberValue(salaryIdx))
            .isEqualTo(PrimitiveValue.float64(1234.56));
    }

    @Test
    public void newInstanceWithNames() {
        StructValue employee = employeeType.newInstance(ImmutableMap.of(
            "age", PrimitiveValue.uint32(99),
            "salary", PrimitiveValue.float64(1234.56),
            "name", PrimitiveValue.utf8("William")));

        assertThat(employee.getMembersCount())
            .isEqualTo(employeeType.getMembersCount());

        assertThat(employee.getMemberValue(employeeType.getMemberIndex("name")))
            .isEqualTo(PrimitiveValue.utf8("William"));
        assertThat(employee.getMemberValue(employeeType.getMemberIndex("age")))
            .isEqualTo(PrimitiveValue.uint32(99));
        assertThat(employee.getMemberValue(employeeType.getMemberIndex("salary")))
            .isEqualTo(PrimitiveValue.float64(1234.56));
    }

    @Test
    public void oneMemberProtobuf() {
        StructType type = StructType.of("a", PrimitiveType.uint32());
        StructValue value = StructValue.of(PrimitiveValue.uint32(1));

        ValueProtos.Value valuePb = value.toPb(type);
        ProtoTruth.assertThat(valuePb)
            .isEqualTo(ValueProtos.Value.newBuilder()
                .addItems(ProtoValue.uint32(1))
                .build());

        Value valueX = ProtoValue.fromPb(type, valuePb);
        assertThat(valueX).isEqualTo(value);
    }

    @Test
    public void manyMembersProtobuf() {
        StructType type = StructType.of("a", PrimitiveType.uint32(), "b", PrimitiveType.bool(), "c", PrimitiveType.utf8());
        StructValue value = StructValue.of(PrimitiveValue.uint32(1), PrimitiveValue.bool(true), PrimitiveValue.utf8("yes"));

        ValueProtos.Value valuePb = value.toPb(type);
        ProtoTruth.assertThat(valuePb)
            .isEqualTo(ValueProtos.Value.newBuilder()
                .addItems(ProtoValue.uint32(1))
                .addItems(ProtoValue.bool(true))
                .addItems(ProtoValue.utf8("yes"))
                .build());

        Value valueX = ProtoValue.fromPb(type, valuePb);
        assertThat(valueX).isEqualTo(value);
    }

    @Test
    public void toStr() {
        StructValue value1 = StructValue.of(PrimitiveValue.uint32(1));
        assertThat(value1.toString())
            .isEqualTo("Struct[1]");

        StructValue value2 = StructValue.of(PrimitiveValue.uint32(1), PrimitiveValue.bool(true), PrimitiveValue.utf8("yes"));
        assertThat(value2.toString())
            .isEqualTo("Struct[1, true, \"yes\"]");
    }
}
