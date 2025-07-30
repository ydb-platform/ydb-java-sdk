package tech.ydb.table.values;

import java.util.Arrays;
import java.util.Map;

import tech.ydb.proto.ValueProtos;
import tech.ydb.table.utils.Arrays2;


/**
 * @author Sergey Polovko
 */
public class StructValue implements Value<StructType> {
    private static final long serialVersionUID = -8795243395989641536L;

    private final StructType type;
    private final Value<?>[] members;

    StructValue(StructType type, Value<?>... members) {
        this.type = type;
        this.members = members;
    }

    public static StructValue of(String memberName, Value<?> memberValue) {
        StructType type = StructType.of(memberName, memberValue.getType());
        return new StructValue(type, memberValue);
    }

    public static StructValue of(
        String member1Name, Value<?> member1Value,
        String member2Name, Value<?> member2Value) {
        String[] names = {member1Name, member2Name};
        Value<?>[] values = {member1Value, member2Value};
        return newStruct(names, values);
    }

    public static StructValue of(
        String member1Name, Value<?> member1Value,
        String member2Name, Value<?> member2Value,
        String member3Name, Value<?> member3Value) {
        String[] names = {member1Name, member2Name, member3Name};
        Value<?>[] values = {member1Value, member2Value, member3Value};
        return newStruct(names, values);
    }

    @SuppressWarnings("checkstyle:ParameterNumber")
    public static StructValue of(
        String member1Name, Value<?> member1Value,
        String member2Name, Value<?> member2Value,
        String member3Name, Value<?> member3Value,
        String member4Name, Value<?> member4Value) {
        String[] names = {member1Name, member2Name, member3Name, member4Name};
        Value<?>[] values = {member1Value, member2Value, member3Value, member4Value};
        return newStruct(names, values);
    }

    @SuppressWarnings("checkstyle:ParameterNumber")
    public static StructValue of(
        String member1Name, Value<?> member1Value,
        String member2Name, Value<?> member2Value,
        String member3Name, Value<?> member3Value,
        String member4Name, Value<?> member4Value,
        String member5Name, Value<?> member5Value) {
        String[] names = {member1Name, member2Name, member3Name, member4Name, member5Name};
        Value<?>[] values = {member1Value, member2Value, member3Value, member4Value, member5Value};
        return newStruct(names, values);
    }

    public static StructValue of(Map<String, Value<?>> members) {
        final int size = members.size();
        final String[] names = new String[size];
        final Value<?>[] values = new Value<?>[size];

        int i = 0;
        for (Map.Entry<String, Value<?>> e : members.entrySet()) {
            names[i] = e.getKey();
            values[i] = e.getValue();
            i++;
        }

        return newStruct(names, values);
    }

    public int getMembersCount() {
        return members.length;
    }

    public Value<?> getMemberValue(int index) {
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
        for (Value<?> member : members) {
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
        for (Value<?> member : members) {
            builder.addItems(member.toPb());
        }
        return builder.build();
    }

    @Override
    public int compareTo(Value<?> other) {
        if (other == null) {
            throw new IllegalArgumentException("Cannot compare with null value");
        }

        // Handle comparison with OptionalValue
        if (other instanceof OptionalValue) {
            OptionalValue otherOptional = (OptionalValue) other;

            // Check that the item type matches this struct type
            if (!getType().equals(otherOptional.getType().getItemType())) {
                throw new IllegalArgumentException(
                    "Cannot compare StructValue with OptionalValue of different item type: " +
                    getType() + " vs " + otherOptional.getType().getItemType());
            }

            // Non-empty value is greater than empty optional
            if (!otherOptional.isPresent()) {
                return 1;
            }

            // Compare with the wrapped value
            return compareTo(otherOptional.get());
        }

        if (!(other instanceof StructValue)) {
            throw new IllegalArgumentException("Cannot compare StructValue with " + other.getClass().getSimpleName());
        }

        StructValue otherStruct = (StructValue) other;

        // Compare members lexicographically
        int minLength = Math.min(members.length, otherStruct.members.length);
        for (int i = 0; i < minLength; i++) {
            Value<?> thisMember = members[i];
            Value<?> otherMember = otherStruct.members[i];

            int memberComparison = compareValues(thisMember, otherMember);
            if (memberComparison != 0) {
                return memberComparison;
            }
        }

        // If we reach here, one struct is a prefix of the other
        // The shorter struct comes first
        return Integer.compare(members.length, otherStruct.members.length);
    }

    private static int compareValues(Value<?> a, Value<?> b) {
        // Handle null values
        if (a == null && b == null) {
            return 0;
        }
        if (a == null) {
            return -1;
        }
        if (b == null) {
            return 1;
        }

        // Check that the types are the same
        if (!a.getType().equals(b.getType())) {
            throw new IllegalArgumentException("Cannot compare values of different types: " +
                a.getType() + " vs " + b.getType());
        }

        // Use the actual compareTo method of the values
        if (a instanceof Comparable && b instanceof Comparable) {
            try {
                return ((Comparable<Value<?>>) a).compareTo((Value<?>) b);
            } catch (ClassCastException e) {
                // Fall back to error
            }
        }

        throw new IllegalArgumentException("Cannot compare values of different types: " +
            a.getClass().getSimpleName() + " vs " + b.getClass().getSimpleName());
    }

    private static StructValue newStruct(String[] names, Value<?>[] values) {
        Arrays2.sortBothByFirst(names, values);
        final Type[] types = new Type[values.length];
        for (int i = 0; i < values.length; i++) {
            types[i] = values[i].getType();
        }
        return new StructValue(new StructType(names, types), values);
    }
}
