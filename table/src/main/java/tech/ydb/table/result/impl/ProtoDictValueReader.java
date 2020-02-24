package tech.ydb.table.result.impl;

import tech.ydb.ValueProtos;
import tech.ydb.table.result.ValueReader;


/**
 * @author Sergey Polovko
 */
final class ProtoDictValueReader extends AbstractValueReader {

    private final ValueProtos.Type type;
    private final AbstractValueReader keyReader;
    private final AbstractValueReader valueReader;
    private ValueProtos.Value value;

    ProtoDictValueReader(ValueProtos.Type type, AbstractValueReader keyReader, AbstractValueReader valueReader) {
        this.type = type;
        this.keyReader = keyReader;
        this.valueReader = valueReader;
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
    public int getDictItemsCount() {
        return value.getPairsCount();
    }

    @Override
    public ValueReader getDictKey(int index) {
        ValueProtos.ValuePair pair = value.getPairs(index);
        keyReader.setProtoValue(pair.getKey());
        return keyReader;
    }

    @Override
    public ValueReader getDictValue(int index) {
        ValueProtos.ValuePair pair = value.getPairs(index);
        valueReader.setProtoValue(pair.getPayload());
        return valueReader;
    }

    @Override
    public void toString(StringBuilder sb) {
        sb.append("Dict[");
        for (int i = 0; i < getDictItemsCount(); i++) {
            getDictKey(i).toString(sb);
            sb.append(": ");
            getDictValue(i).toString(sb);
            sb.append(", ");
        }
        if (getDictItemsCount() > 0) {
            sb.setLength(sb.length() - 2);
        }
        sb.append(']');
    }
}
