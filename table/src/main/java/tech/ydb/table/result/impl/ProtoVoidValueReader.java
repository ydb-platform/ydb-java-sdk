package tech.ydb.table.result.impl;

import tech.ydb.ValueProtos;
import tech.ydb.table.values.proto.ProtoType;
import tech.ydb.table.values.proto.ProtoValue;


/**
 * @author Sergey Polovko
 */
final class ProtoVoidValueReader extends AbstractValueReader {
    private ProtoVoidValueReader() {}

    static final ProtoVoidValueReader INSTANCE = new ProtoVoidValueReader();

    @Override
    protected ValueProtos.Type getType() {
        return ProtoType.voidType();
    }

    @Override
    protected ValueProtos.Value getValue() {
        return ProtoValue.voidValue();
    }

    @Override
    protected void setValue(ValueProtos.Value value) {
        // skip
    }

    @Override
    public void toString(StringBuilder sb) {
        sb.append("Void");
    }
}
