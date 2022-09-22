package tech.ydb.table.values;

import java.util.Arrays;
import java.util.Map;

import tech.ydb.ValueProtos;
import tech.ydb.table.utils.Arrays2;


/**
 * @author Sergey Polovko
 */
public class StructValue implements Value<StructType> {

    private final StructType type;
    private final Value[] members;

    StructValue(StructType type, Value... members) {
        this.type = type;
        this.members = members;
    }

    public static StructValue of(String memberName, Value memberValue) {
        StructType type = StructType.of(memberName, memberValue.getType());
        return new StructValue(type, memberValue);
    }

    public static StructValue of(
        String member1Name, Value member1Value,
        String member2Name, Value member2Value) {
        String[] names = {member1Name, member2Name};
        Value[] values = {member1Value, member2Value};
        return newStruct(names, values);
    }

    public static StructValue of(
        String member1Name, Value member1Value,
        String member2Name, Value member2Value,
        String member3Name, Value member3Value) {
        String[] names = {member1Name, member2Name, member3Name};
        Value[] values = {member1Value, member2Value, member3Value};
        return newStruct(names, values);
    }

    @SuppressWarnings("checkstyle:ParameterNumber")
    public static StructValue of(
        String member1Name, Value member1Value,
        String member2Name, Value member2Value,
        String member3Name, Value member3Value,
        String member4Name, Value member4Value) {
        String[] names = {member1Name, member2Name, member3Name, member4Name};
        Value[] values = {member1Value, member2Value, member3Value, member4Value};
        return newStruct(names, values);
    }

    @SuppressWarnings("checkstyle:ParameterNumber")
    public static StructValue of(
        String member1Name, Value member1Value,
        String member2Name, Value member2Value,
        String member3Name, Value member3Value,
        String member4Name, Value member4Value,
        String member5Name, Value member5Value) {
        String[] names = {member1Name, member2Name, member3Name, member4Name, member5Name};
        Value[] values = {member1Value, member2Value, member3Value, member4Value, member5Value};
        return newStruct(names, values);
    }

    public static StructValue of(Map<String, Value> members) {
        final int size = members.size();
        final String[] names = new String[size];
        final Value[] values = new Value[size];

        int i = 0;
        for (Map.Entry<String, Value> e : members.entrySet()) {
            names[i] = e.getKey();
            values[i] = e.getValue();
            i++;
        }

        return newStruct(names, values);
    }

    public int getMembersCount() {
        return members.length;
    }

    public Value getMemberValue(int index) {
        return members[index];
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        StructValue that = (StructValue) o;
        return Arrays.equals(members, that.members);
    }

    @Override
    public int hashCode() {
        return 31 * Type.Kind.STRUCT.hashCode() + Arrays.hashCode(members);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Struct[");
        for (Value member : members) {
            sb.append(member).append(", ");
        }
        sb.setLength(sb.length() - 2);
        sb.append(']');
        return sb.toString();
    }

    @Override
    public StructType getType() {
        return type;
    }

    @Override
    public ValueProtos.Value toPb() {
        ValueProtos.Value.Builder builder = ValueProtos.Value.newBuilder();
        for (Value member : members) {
            builder.addItems(member.toPb());
        }
        return builder.build();
    }

    private static StructValue newStruct(String[] names, Value[] values) {
        Arrays2.sortBothByFirst(names, values);
        final Type[] types = new Type[values.length];
        for (int i = 0; i < values.length; i++) {
            types[i] = values[i].getType();
        }
        return new StructValue(new StructType(names, types), values);
    }
}
