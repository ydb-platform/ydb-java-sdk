package tech.ydb.table.result.impl;

import tech.ydb.ValueProtos;
import tech.ydb.table.result.ValueReader;


/**
 * @author Sergey Polovko
 */
final class ProtoTupleValueReader extends AbstractValueReader {

    private final ValueProtos.Type type;
    private final AbstractValueReader[] elementReaders;
    private ValueProtos.Value value;

    ProtoTupleValueReader(ValueProtos.Type type, AbstractValueReader[] elementReaders) {
        this.type = type;
        this.elementReaders = elementReaders;
    }

    @Override
    protected ValueProtos.Type getType() {
        return type;
    }

    @Override
    protected ValueProtos.Value getValue() {
        return value;
    }

    @Override
    protected void setValue(ValueProtos.Value value) {
        this.value = value;
    }

    @Override
    public int getTupleElementsCount() {
        return elementReaders.length;
    }

    @Override
    public ValueReader getTupleElement(int index) {
        AbstractValueReader elementReader = elementReaders[index];
        elementReader.setValue(value.getItems(index));
        return elementReader;
    }

    @Override
    public void toString(StringBuilder sb) {
        sb.append("Tuple[");
        for (int i = 0; i < getTupleElementsCount(); i++) {
            getTupleElement(i).toString(sb);
            sb.append(", ");
        }
        if (getTupleElementsCount() > 0) {
            sb.setLength(sb.length() - 2);
        }
        sb.append(']');
    }
}
