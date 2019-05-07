package ru.yandex.ydb.table.types;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ru.yandex.ydb.table.utils.Arrays2;
import ru.yandex.ydb.table.values.StructValue;
import ru.yandex.ydb.table.values.Value;


/**
 * @author Sergey Polovko
 */
public final class StructType implements Type {

    private final String[] names;
    private final Type[] types;
    private final Map<String, Integer> namesIdx;

    private StructType(String[] names, Type[] types) {
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
        return new StructType(new String[] { memberName }, new Type[] { memberType });
    }

    public static StructType of(
        String member1Name, Type member1Type,
        String member2Name, Type member2Type)
    {
        return new StructType(
            new String[] { member1Name, member2Name },
            new Type[] { member1Type, member2Type });
    }

    public static StructType of(
        String member1Name, Type member1Type,
        String member2Name, Type member2Type,
        String member3Name, Type member3Type)
    {
        return new StructType(
            new String[] { member1Name, member2Name, member3Name },
            new Type[] { member1Type, member2Type, member3Type });
    }

    public static StructType of(
        String member1Name, Type member1Type,
        String member2Name, Type member2Type,
        String member3Name, Type member3Type,
        String member4Name, Type member4Type)
    {
        return new StructType(
            new String[] { member1Name, member2Name, member3Name, member4Name },
            new Type[] { member1Type, member2Type, member3Type, member4Type });
    }

    public static StructType of(
        String member1Name, Type member1Type,
        String member2Name, Type member2Type,
        String member3Name, Type member3Type,
        String member4Name, Type member4Type,
        String member5Name, Type member5Type)
    {
        return new StructType(
            new String[] { member1Name, member2Name, member3Name, member4Name, member5Name },
            new Type[] { member1Type, member2Type, member3Type, member4Type, member5Type });
    }

    /**
     * MEMBER
     */
    public class Member {
        final String name;
        final Type type;

        public Member(String name, Type type) {
            this.name = name;
            this.type = type;
        }
    }

    public static StructType of(Member firstMember, Member... members) {
        String[] names = new String[members.length + 1];
        Type[] types = new Type[members.length + 1];
        names[0] = firstMember.name;
        types[0] = firstMember.type;
        for (int i = 0; i < members.length; i++) {
            names[i + 1] = members[i].name;
            types[i + 1] = members[i].type;
        }
        return new StructType(names, types);
    }

    public static StructType of(String[] names, Type[] types) {
        return new StructType(names.clone(), types.clone());
    }

    /**
     * will not clone given arrays
     */
    public static StructType ofOwning(String[] names, Type[] types) {
        return new StructType(names, types);
    }

    public static StructType of(List<String> names, List<Type> types) {
        if (names.size() != types.size()) {
            throw new IllegalStateException("names and types count mismatch");
        }

        String[] namesArray = names.toArray(new String[0]);
        Type[] typesArray = types.toArray(new Type[0]);
        return new StructType(namesArray, typesArray);
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
        if (this == o) return true;
        if (o == null || o.getClass() != StructType.class) return false;

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

    public StructValue newInstance(Map<String, Value> members) {
        return StructValue.of(this, members);
    }
}
