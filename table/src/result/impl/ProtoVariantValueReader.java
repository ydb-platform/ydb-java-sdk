package ru.yandex.ydb.table.result.impl;

import ru.yandex.ydb.ValueProtos;
import ru.yandex.ydb.table.result.ValueReader;


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
    protected ValueProtos.Type getType() {
        return type;
    }

    @Override
    protected void setValue(ValueProtos.Value value) {
        this.value = value;
    }

    @Override
    public int getVariantTypeIndex() {
        return value.getVariantIndex();
    }

    @Override
    public ValueReader getVariantItem() {
        AbstractValueReader itemReader = itemReaders[value.getVariantIndex()];
        itemReader.setValue(value.getNestedValue());
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
