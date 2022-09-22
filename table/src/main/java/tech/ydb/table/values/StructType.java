package tech.ydb.table.values;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import tech.ydb.ValueProtos;
import tech.ydb.table.utils.Arrays2;

import static com.google.common.base.Preconditions.checkArgument;


/**
 * @author Sergey Polovko
 */
public final class StructType implements Type {

    private final String[] names;
    private final Type[] types;
    private final Map<String, Integer> namesIdx;

    StructType(String[] names, Type[] types) {
        if (names.length == 0 || types.length == 0) {
            throw new IllegalStateException("names or types cannot be empty");
        }
        if (names.length != types.length) {
            throw new IllegalStateException("names and types count mismatch");
        }

        Arrays2.sortBothByFirst(names, types);

        this.names = names;
        this.types = types;
        this.namesIdx = buildNamesIdx(names);
    }

    public static StructType of(String memberName, Type memberType) {
        return new StructType(new String[] {memberName}, new Type[] {memberType});
    }

    public static StructType of(
        String member1Name, Type member1Type,
        String member2Name, Type member2Type) {
        return new StructType(
            new String[] {member1Name, member2Name},
            new Type[] {member1Type, member2Type});
    }

    public static StructType of(
        String member1Name, Type member1Type,
        String member2Name, Type member2Type,
        String member3Name, Type member3Type) {
        return new StructType(
            new String[] {member1Name, member2Name, member3Name},
            new Type[] {member1Type, member2Type, member3Type});
    }

    @SuppressWarnings("checkstyle:ParameterNumber")
    public static StructType of(
        String member1Name, Type member1Type,
        String member2Name, Type member2Type,
        String member3Name, Type member3Type,
        String member4Name, Type member4Type) {
        return new StructType(
            new String[] {member1Name, member2Name, member3Name, member4Name},
            new Type[] {member1Type, member2Type, member3Type, member4Type});
    }

    @SuppressWarnings("checkstyle:ParameterNumber")
    public static StructType of(
        String member1Name, Type member1Type,
        String member2Name, Type member2Type,
        String member3Name, Type member3Type,
        String member4Name, Type member4Type,
        String member5Name, Type member5Type) {
        return new StructType(
            new String[] {member1Name, member2Name, member3Name, member4Name, member5Name},
            new Type[] {member1Type, member2Type, member3Type, member4Type, member5Type});
    }

    public static StructType of(Map<String, Type> members) {
        final int size = members.size();
        final String[] names = new String[size];
        final Type[] types = new Type[size];

        int i = 0;
        for (Map.Entry<String, Type> e : members.entrySet()) {
            names[i] = e.getKey();
            types[i] = e.getValue();
            i++;
        }
        return new StructType(names, types);
    }

    public static StructType of(List<String> names, List<Type> types) {
        String[] namesArray = names.toArray(new String[0]);
        Type[] typesArray = types.toArray(Type.EMPTY_ARRAY);
        return new StructType(namesArray, typesArray);
    }

    public static StructType ofCopy(String[] names, Type[] types) {
        return new StructType(names.clone(), types.clone());
    }

    /**
     * will not clone given arrays
     */
    public static StructType ofOwn(String[] names, Type[] types) {
        return new StructType(names, types);
    }

    public int getMembersCount() {
        return names.length;
    }

    public String getMemberName(int index) {
        return names[index];
    }

    public Type getMemberType(int index) {
        return types[index];
    }

    public int getMemberIndex(String name) {
        Integer index = namesIdx.get(name);
        return index == null ? -1 : index;
    }

    @Override
    public Kind getKind() {
        return Kind.STRUCT;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        StructType structType = (StructType) o;
        if (getMembersCount() != structType.getMembersCount()) {
            return false;
        }
        for (int i = 0; i < getMembersCount(); i++) {
            if (!getMemberName(i).equals(structType.getMemberName(i))) {
                return false;
            }
            if (!getMemberType(i).equals(structType.getMemberType(i))) {
                return false;
            }
        }
        return true;
    }

    @Override
    public int hashCode() {
        int result = Kind.STRUCT.hashCode();
        result = 31 * result + Arrays.hashCode(names);
        result = 31 * result + Arrays.hashCode(types);
        return result;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Struct<");
        for (int i = 0; i < names.length; i++) {
            sb.append('\'').append(names[i]).append('\'');
            sb.append(": ").append(types[i]).append(", ");
        }
        if (names.length != 0) {
            sb.setLength(sb.length() - 2); // drop last comma
        }
        sb.append('>');
        return sb.toString();
    }

    @Override
    public ValueProtos.Type toPb() {
        ValueProtos.StructType.Builder structType = ValueProtos.StructType.newBuilder();
        for (int i = 0; i < names.length; i++) {
            structType.addMembersBuilder()
                .setName(names[i])
                .setType(types[i].toPb());
        }
        return ValueProtos.Type.newBuilder().setStructType(structType).build();
    }

    public StructValue newValue(String memberName, Value memberValue) {
        checkArgument(getMembersCount() == 1, "struct type %s has different members count", this);
        checkArgument(getMemberName(0).equals(memberName), "struct type %s has no member %s", this, memberValue);
        return new StructValue(this, memberValue);
    }

    public StructValue newValue(
        String member1Name, Value member1Value,
        String member2Name, Value member2Value) {
        checkArgument(getMembersCount() == 2, "struct type %s has different members count", this);
        Value[] values = new Value[2];
        setValue(values, member1Name, member1Value);
        setValue(values, member2Name, member2Value);
        return new StructValue(this, values);
    }

    public StructValue newValue(
        String member1Name, Value member1Value,
        String member2Name, Value member2Value,
        String member3Name, Value member3Value) {
        checkArgument(getMembersCount() == 3, "struct type %s has different members count", this);
        Value[] values = new Value[3];
        setValue(values, member1Name, member1Value);
        setValue(values, member2Name, member2Value);
        setValue(values, member3Name, member3Value);
        return new StructValue(this, values);
    }

    @SuppressWarnings("checkstyle:ParameterNumber")
    public StructValue newValue(
        String member1Name, Value member1Value,
        String member2Name, Value member2Value,
        String member3Name, Value member3Value,
        String member4Name, Value member4Value) {
        checkArgument(getMembersCount() == 4, "struct type %s has different members count", this);
        Value[] values = new Value[4];
        setValue(values, member1Name, member1Value);
        setValue(values, member2Name, member2Value);
        setValue(values, member3Name, member3Value);
        setValue(values, member4Name, member4Value);
        return new StructValue(this, values);
    }

    @SuppressWarnings("checkstyle:ParameterNumber")
    public StructValue newValue(
        String member1Name, Value member1Value,
        String member2Name, Value member2Value,
        String member3Name, Value member3Value,
        String member4Name, Value member4Value,
        String member5Name, Value member5Value) {
        checkArgument(getMembersCount() == 5, "struct type %s has different members count", this);
        Value[] values = new Value[5];
        setValue(values, member1Name, member1Value);
        setValue(values, member2Name, member2Value);
        setValue(values, member3Name, member3Value);
        setValue(values, member4Name, member4Value);
        setValue(values, member5Name, member5Value);
        return new StructValue(this, values);
    }

    private void setValue(Value[] values, String name, Value value) {
        final int idx = getMemberIndex(name);
        checkArgument(idx != -1, "struct type %s has no member %s", this, name);
        values[idx] = value;
    }

    public StructValue newValue(Map<String, Value> membersMap) {
        checkArgument(
            getMembersCount() == membersMap.size(),
            "incompatible struct type %s and values names %s",
            this, membersMap.keySet());

        Value[] members = new Value[membersMap.size()];
        for (int i = 0; i < getMembersCount(); i++) {
            String name = getMemberName(i);
            Value value = membersMap.get(name);
            checkArgument(value != null, "given map %s has no member with name %s", membersMap.keySet(), name);
            members[i] = value;
        }

        return new StructValue(this, members);
    }

    /**
     * will not clone given array
     */
    public StructValue newValueUnsafe(Value... members) {
        return new StructValue(this, members);
    }

    private static Map<String, Integer> buildNamesIdx(String[] names) {
        // TODO: use structure with lower memory usage
        HashMap<String, Integer> namesIdx = new HashMap<>(names.length);
        for (int i = 0; i < names.length; i++) {
            if (namesIdx.put(names[i], i) != null) {
                throw new IllegalArgumentException("duplicate member name in struct: " + names[i]);
            }
        }
        return namesIdx;
    }
}
