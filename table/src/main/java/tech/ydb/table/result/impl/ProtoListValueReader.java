package tech.ydb.table.result.impl;

import tech.ydb.ValueProtos;
import tech.ydb.table.result.ValueReader;


/**
 * @author Sergey Polovko
 */
final class ProtoListValueReader extends AbstractValueReader {

    private final ValueProtos.Type type;
    private final AbstractValueReader itemReader;
    private ValueProtos.Value value;

    ProtoListValueReader(ValueProtos.Type type, AbstractValueReader itemReader) {
        this.type = type;
        this.itemReader = itemReader;
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
    public int getListItemsCount() {
        return value.getItemsCount();
    }

    @Override
    public ValueReader getListItem(int index) {
        itemReader.setValue(value.getItems(index));
        return itemReader;
    }

    @Override
    public void toString(StringBuilder sb) {
        sb.append("List[");
        for (int i = 0; i < getListItemsCount(); i++) {
            getListItem(i).toString(sb);
            sb.append(", ");
        }
        if (getListItemsCount() > 0) {
            sb.setLength(sb.length() - 2);
        }
        sb.append(']');
    }
}
