package tech.ydb.table.result.impl;

import tech.ydb.proto.ValueProtos;
import tech.ydb.table.values.proto.ProtoType;
import tech.ydb.table.values.proto.ProtoValue;


/**
 * @author Sergey Polovko
 */
final class ProtoVoidValueReader extends AbstractValueReader {
    static final ProtoVoidValueReader INSTANCE = new ProtoVoidValueReader();

    private ProtoVoidValueReader() { }

    @Override
    protected ValueProtos.Type getProtoType() {
        return ProtoType.getVoid();
    }

    @Override
    protected ValueProtos.Value getProtoValue() {
        return ProtoValue.voidValue();
    }

    @Override
    protected void setProtoValue(ValueProtos.Value value) {
        // skip
    }

    @Override
    public void toString(StringBuilder sb) {
        sb.append("Void");
    }
}
