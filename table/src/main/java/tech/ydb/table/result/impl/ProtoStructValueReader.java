package tech.ydb.table.result.impl;

import java.util.HashMap;
import java.util.Map;

import tech.ydb.ValueProtos;
import tech.ydb.table.result.ValueReader;


/**
 * @author Sergey Polovko
 */
final class ProtoStructValueReader extends AbstractValueReader {

    private final ValueProtos.Type type;
    private final AbstractValueReader[] memberReaders;
    private final Map<String, Integer> nameIdx;
    private ValueProtos.Value value;

    ProtoStructValueReader(ValueProtos.Type type, AbstractValueReader[] memberReaders) {
        this.type = type;
        this.memberReaders = memberReaders;
        this.nameIdx = buildNameIdx(type.getStructType());
    }

    private static HashMap<String, Integer> buildNameIdx(ValueProtos.StructType structType) {
        HashMap<String, Integer> nameIdx = new HashMap<>(structType.getMembersCount());
        for (int i = 0; i < structType.getMembersCount(); i++) {
            nameIdx.put(structType.getMembers(i).getName(), i);
        }
        return nameIdx;
    }

    @Override
    protected ValueProtos.Type getProtoType() {
        return type;
    }

    @Override
    protected ValueProtos.Value getProtoValue() {
        return value;
    }

    @Override
    protected void setProtoValue(ValueProtos.Value value) {
        this.value = value;
    }

    @Override
    public int getStructMembersCount() {
        return memberReaders.length;
    }

    @Override
    public String getStructMemberName(int index) {
        return type.getStructType().getMembers(index).getName();
    }

    @Override
    public ValueReader getStructMember(int index) {
        AbstractValueReader memberReader = memberReaders[index];
        memberReader.setProtoValue(value.getItems(index));
        return memberReader;
    }

    @Override
    public ValueReader getStructMember(String name) {
        Integer index = nameIdx.get(name);
        if (index == null) {
            throw new IllegalArgumentException("unknown member name: '" + name + '\'');
        }
        return getStructMember(index);
    }

    @Override
    public void toString(StringBuilder sb) {
        sb.append("Struct[");
        for (int i = 0; i < getStructMembersCount(); i++) {
            String name = getStructMemberName(i);
            sb.append(name).append(": ");
            getStructMember(i).toString(sb);
            sb.append(", ");
        }
        if (getStructMembersCount() > 0) {
            sb.setLength(sb.length() - 2);
        }
        sb.append(']');
    }
}
