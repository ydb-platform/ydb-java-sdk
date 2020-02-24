package tech.ydb.table.result.impl;

import tech.ydb.ValueProtos;
import tech.ydb.table.result.ValueReader;


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
    protected ValueProtos.Type getProtoType() {
        return type;
    }

    @Override
    protected ValueProtos.Value getProtoValue() {
        return itemReader.getProtoValue();
    }

    @Override
    protected void setProtoValue(ValueProtos.Value value) {
        switch (value.getValueCase()) {
            case NESTED_VALUE:
                present = true;
                itemReader.setProtoValue(value.getNestedValue());
                break;
            case NULL_FLAG_VALUE:
                present = false;
                itemReader.setProtoValue(ValueProtos.Value.getDefaultInstance()); // for cleanup
                break;
            default:
                present = true;
                itemReader.setProtoValue(value);
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
