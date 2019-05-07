package ru.yandex.ydb.table.result.impl;

import ru.yandex.ydb.ValueProtos;
import ru.yandex.ydb.table.result.ValueReader;


/**
 * @author Sergey Polovko
 */
final class ProtoOptionalValueReader extends AbstractValueReader {

    private final ValueProtos.Type type;
    private final AbstractValueReader itemReader;
    private boolean present = false;

    ProtoOptionalValueReader(ValueProtos.Type type, AbstractValueReader itemReader) {
        this.type = type;
        this.itemReader = itemReader;
    }

    @Override
    protected ValueProtos.Type getType() {
        return type;
    }

    @Override
    protected void setValue(ValueProtos.Value value) {
        switch (value.getValueCase()) {
            case NESTED_VALUE:
                present = true;
                itemReader.setValue(value.getNestedValue());
                break;
            case NULL_FLAG_VALUE:
                present = false;
                itemReader.setValue(ValueProtos.Value.getDefaultInstance()); // for cleanup
                break;
            default:
                present = true;
                itemReader.setValue(value);
                break;
        }
    }

    @Override
    public boolean isOptionalItemPresent() {
        return present;
    }

    @Override
    public ValueReader getOptionalItem() {
        // TODO: return empty optional if present == false
        return itemReader;
    }

    @Override
    public void toString(StringBuilder sb) {
        if (isOptionalItemPresent()) {
            sb.append("Some[");
            getOptionalItem().toString(sb);
            sb.append(']');
        } else {
            sb.append("Empty[]");
        }
    }
}
