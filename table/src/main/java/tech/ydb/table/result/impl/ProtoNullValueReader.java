package tech.ydb.table.result.impl;

import tech.ydb.proto.ValueProtos;
import tech.ydb.table.values.proto.ProtoType;
import tech.ydb.table.values.proto.ProtoValue;


/**
 * @author Aleksandr Gorshenin
 */
final class ProtoNullValueReader extends AbstractValueReader {
    static final ProtoNullValueReader INSTANCE = new ProtoNullValueReader();

    private ProtoNullValueReader() { }

    @Override
    protected ValueProtos.Type getProtoType() {
        return ProtoType.getNull();
    }

    @Override
    protected ValueProtos.Value getProtoValue() {
        return ProtoValue.nullValue();
    }

    @Override
    protected void setProtoValue(ValueProtos.Value value) {
        // skip
    }

    @Override
    public void toString(StringBuilder sb) {
        sb.append("Null");
    }
}
