package tech.ydb.table.values;

import java.util.Arrays;
import java.util.List;

import tech.ydb.proto.ValueProtos;
import tech.ydb.table.values.proto.ProtoValue;


/**
 * @author Sergey Polovko
 */
public class TupleValue implements Value<TupleType> {
    private static final TupleValue EMPTY = new TupleValue(TupleType.empty(), Value.EMPTY_ARRAY);
    private static final long serialVersionUID = -8142947550399877474L;

    private final TupleType type;
    private final Value<?>[] items;

    TupleValue(TupleType type, Value<?>... items) {
        this.type = type;
        this.items = items;
    }

    public static TupleValue empty() {
        return EMPTY;
    }

    public static TupleValue of(Value<?> item) {
        return new TupleValue(TupleType.of(item.getType()), item);
    }

    public static TupleValue of(Value<?> a, Value<?> b) {
        TupleType type = TupleType.ofOwn(a.getType(), b.getType());
        return new TupleValue(type, a, b);
    }

    public static TupleValue of(Value<?> a, Value<?> b, Value<?> c) {
        TupleType type = TupleType.ofOwn(a.getType(), b.getType(), c.getType());
        return new TupleValue(type, a, b, c);
    }

    public static TupleValue of(Value<?> a, Value<?> b, Value<?> c, Value<?> d) {
        TupleType type = TupleType.ofOwn(a.getType(), b.getType(), c.getType(), d.getType());
        return new TupleValue(type, a, b, c, d);
    }

    public static TupleValue of(Value<?> a, Value<?> b, Value<?> c, Value<?> d, Value<?> e) {
        TupleType type = TupleType.ofOwn(a.getType(), b.getType(), c.getType(), d.getType(), e.getType());
        return new TupleValue(type, a, b, c, d, e);
    }

    public static TupleValue of(List<Value<?>> items) {
        if (items.isEmpty()) {
            return EMPTY;
        }
        return fromArray(items.toArray(Value.EMPTY_ARRAY));
    }

    public static TupleValue ofCopy(Value<?>... items) {
        if (items.length == 0) {
            return EMPTY;
        }
        return fromArray(items.clone());
    }

    /**
     * will not clone given array
     */
    public static TupleValue ofOwn(Value<?>... items) {
        if (items.length == 0) {
            return EMPTY;
        }
        return fromArray(items);
    }

    public int size() {
        return items.length;
    }

    public boolean isEmpty() {
        return items.length == 0;
    }

    public Value<?> get(int index) {
        return items[index];
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

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
        for (Value<?> item : items) {
            sb.append(item).append(", ");
        }
        if (items.length > 0) {
            sb.setLength(sb.length() - 2);
        }
        sb.append(']');
        return sb.toString();
    }

    @Override
    public TupleType getType() {
        return type;
    }

    @Override
    public ValueProtos.Value toPb() {
        if (isEmpty()) {
            return ProtoValue.tuple();
        }

        ValueProtos.Value.Builder builder = ValueProtos.Value.newBuilder();
        for (Value<?> item : items) {
            builder.addItems(item.toPb());
        }
        return builder.build();
    }

    private static TupleValue fromArray(Value<?>... items) {
        final int size = items.length;
        final Type[] types = new Type[size];
        for (int i = 0; i < size; i++) {
            types[i] = items[i].getType();
        }
        return new TupleValue(TupleType.ofOwn(types), items);
    }

    @Override
    public int compareTo(Value<?> other) {
        if (other == null) {
            throw new NullPointerException("Cannot compare with null value");
        }

        if (other instanceof OptionalValue) {
            OptionalValue optional = (OptionalValue) other;
            if (!optional.isPresent()) {
                throw new NullPointerException("Cannot compare value " + this + " with NULL");
            }
            return compareTo(optional.get());
        }

        if (!type.equals(other.getType())) {
            throw new IllegalArgumentException("Cannot compare value " + type + " with " + other.getType());
        }

        TupleValue otherTuple = (TupleValue) other;

        for (int i = 0; i < getType().getElementsCount(); i++) {
            int itemComparison = items[i].compareTo(otherTuple.items[i]);
            if (itemComparison != 0) {
                return itemComparison;
            }
        }

        return 0;
    }
}
