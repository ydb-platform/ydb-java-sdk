package tech.ydb.table.result.impl;

import tech.ydb.ValueProtos;
import tech.ydb.table.result.ValueReader;


/**
 * @author Sergey Polovko
 */
final class ProtoVariantValueReader extends AbstractValueReader {

    private final ValueProtos.Type type;
    private final AbstractValueReader[] itemReaders;
    private ValueProtos.Value value;

    ProtoVariantValueReader(ValueProtos.Type type, AbstractValueReader[] itemReaders) {
        this.type = type;
        this.itemReaders = itemReaders;
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
    public int getVariantTypeIndex() {
        return value.getVariantIndex();
    }

    @Override
    public ValueReader getVariantItem() {
        AbstractValueReader itemReader = itemReaders[value.getVariantIndex()];
        itemReader.setProtoValue(value.getNestedValue());
        return itemReader;
    }

    @Override
    public void toString(StringBuilder sb) {
        sb.append("Variant[");
        sb.append(getVariantTypeIndex());
        sb.append("; ");
        getVariantItem().toString(sb);
        sb.append(']');
    }
}
