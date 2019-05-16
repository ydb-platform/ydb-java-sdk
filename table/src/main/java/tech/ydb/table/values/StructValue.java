package tech.ydb.table.values;

import java.util.Arrays;
import java.util.Collection;
import java.util.Map;

import tech.ydb.ValueProtos;
import tech.ydb.table.types.StructType;
import tech.ydb.table.types.Type;


/**
 * @author Sergey Polovko
 */
public class StructValue implements Value<StructType> {

    private final Value[] members;

    private StructValue(Value[] members) {
        this.members = members;
    }

    public static StructValue of(Value a) {
        return new StructValue(new Value[] { a });
    }

    public static StructValue of(Value a, Value b) {
        return new StructValue(new Value[] { a, b });
    }

    public static StructValue of(Value a, Value b, Value c) {
        return new StructValue(new Value[] { a, b, c });
    }

    public static StructValue of(Value a, Value b, Value c, Value d) {
        return new StructValue(new Value[] { a, b, c, d });
    }

    public static StructValue of(Value a, Value b, Value c, Value d, Value e) {
        return new StructValue(new Value[] { a, b, c, d, e });
    }

    public static StructValue ofCopy(Value... members) {
        return new StructValue(members.clone());
    }

    /**
     * will not clone given arrays
     */
    public static StructValue ofOwn(Value... members) {
        return new StructValue(members);
    }

    public static StructValue of(Collection<Value> members) {
        return new StructValue(members.toArray(new Value[members.size()]));
    }

    public static StructValue of(StructType type, Map<String, Value> membersMap) {
        if (type.getMembersCount() != membersMap.size()) {
            throw new IllegalArgumentException(
                "incompatible struct type " + type +
                " and values names " + membersMap.keySet());
        }

        Value[] members = new Value[membersMap.size()];
        for (int i = 0; i < type.getMembersCount(); i++) {
            String name = type.getMemberName(i);
            Value value = membersMap.get(name);
            if (value == null) {
                throw new IllegalArgumentException(
                    "incompatible struct type " + type +
                    " and values names " + membersMap.keySet());
            }
            members[i] = value;
        }

        return new StructValue(members);
    }

    public int getMembersCount() {
        return members.length;
    }

    public Value getMemberValue(int index) {
        return members[index];
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

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
    public ValueProtos.Value toPb(StructType type) {
        ValueProtos.Value.Builder builder = ValueProtos.Value.newBuilder();
        for (int i = 0; i < members.length; i++) {
            Type memberType = type.getMemberType(i);
            @SuppressWarnings("unchecked")
            ValueProtos.Value memberValue = members[i].toPb(memberType);
            builder.addItems(memberValue);
        }
        return builder.build();
    }
}
