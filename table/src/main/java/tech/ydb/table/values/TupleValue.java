package tech.ydb.table.values;

import java.util.Arrays;
import java.util.Collection;

import tech.ydb.ValueProtos;
import tech.ydb.table.types.TupleType;
import tech.ydb.table.types.Type;
import tech.ydb.table.values.proto.ProtoValue;


/**
 * @author Sergey Polovko
 */
public class TupleValue implements Value<TupleType> {

    private static final TupleValue EMPTY = new TupleValue(new Value[0]);

    private final Value[] items;

    private TupleValue(Value[] items) {
        this.items = items;
    }

    public static TupleValue of() {
        return EMPTY;
    }

    public static TupleValue of(Value item) {
        return new TupleValue(new Value[] { item });
    }

    public static TupleValue of(Value a, Value b) {
        return new TupleValue(new Value[] { a, b });
    }

    public static TupleValue of(Value a, Value b, Value c) {
        return new TupleValue(new Value[] { a, b, c });
    }

    public static TupleValue of(Value a, Value b, Value c, Value d) {
        return new TupleValue(new Value[] { a, b, c, d });
    }

    public static TupleValue of(Value a, Value b, Value c, Value d, Value e) {
        return new TupleValue(new Value[] { a, b, c, d, e });
    }

    public static TupleValue of(Collection<Value> items) {
        if (items.isEmpty()) {
            return EMPTY;
        }
        return new TupleValue(items.toArray(new Value[items.size()]));
    }

    public static TupleValue fromArrayCopy(Value... items) {
        if (items.length == 0) {
            return EMPTY;
        }
        return new TupleValue(items.clone());
    }

    public static TupleValue fromArrayOwn(Value... items) {
        if (items.length == 0) {
            return EMPTY;
        }
        return new TupleValue(items);
    }

    public int size() {
        return items.length;
    }

    public boolean isEmpty() {
        return items.length == 0;
    }

    public Value get(int index) {
        return items[index];
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        TupleValue that = (TupleValue) o;
        return Arrays.equals(items, that.items);
    }

    @Override
    public int hashCode() {
        return 31 * Type.Kind.TUPLE.hashCode() + Arrays.hashCode(items);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Tuple[");
        for (Value item : items) {
            sb.append(item).append(", ");
        }
        if (items.length > 0) {
            sb.setLength(sb.length() - 2);
        }
        sb.append(']');
        return sb.toString();
    }

    @Override
    public ValueProtos.Value toPb(TupleType type) {
        if (isEmpty()) {
            return ProtoValue.tuple();
        }

        ValueProtos.Value.Builder builder = ValueProtos.Value.newBuilder();
        for (int i = 0; i < items.length; i++) {
            Type elementType = type.getElementType(i);
            @SuppressWarnings("unchecked")
            ValueProtos.Value elementValue = items[i].toPb(elementType);
            builder.addItems(elementValue);
        }
        return builder.build();
    }
}
