package tech.ydb.table.values;

import java.util.Arrays;
import java.util.Collection;

import tech.ydb.ValueProtos;
import tech.ydb.table.types.ListType;
import tech.ydb.table.types.Type;
import tech.ydb.table.values.proto.ProtoValue;


/**
 * @author Sergey Polovko
 */
public class ListValue implements Value<ListType> {

    private static final ListValue EMPTY = new ListValue(new Value[0]);

    private final Value[] items;

    private ListValue(Value[] items) {
        this.items = items;
    }

    public static ListValue of() {
        return EMPTY;
    }

    public static ListValue of(Value item) {
        return new ListValue(new Value[] { item });
    }

    public static ListValue of(Value a, Value b) {
        return new ListValue(new Value[] { a, b });
    }

    public static ListValue of(Value a, Value b, Value c) {
        return new ListValue(new Value[] { a, b, c });
    }

    public static ListValue of(Value a, Value b, Value c, Value d) {
        return new ListValue(new Value[] { a, b, c, d });
    }

    public static ListValue of(Value a, Value b, Value c, Value d, Value e) {
        return new ListValue(new Value[] { a, b, c, d, e });
    }

    public static ListValue of(Collection<Value> items) {
        if (items.isEmpty()) {
            return EMPTY;
        }
        return new ListValue(items.toArray(new Value[items.size()]));
    }

    public static ListValue fromArrayCopy(Value... items) {
        if (items.length == 0) {
            return EMPTY;
        }
        return new ListValue(items.clone());
    }

    public static ListValue fromArrayOwn(Value... items) {
        if (items.length == 0) {
            return EMPTY;
        }
        return new ListValue(items);
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

        ListValue that = (ListValue) o;
        return Arrays.equals(items, that.items);
    }

    @Override
    public int hashCode() {
        return 31 * Type.Kind.LIST.hashCode() + Arrays.hashCode(items);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("List[");
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
    public ValueProtos.Value toPb(ListType type) {
        if (isEmpty()) {
            return ProtoValue.list();
        }

        Type itemType = type.getItemType();

        ValueProtos.Value.Builder builder = ValueProtos.Value.newBuilder();
        for (Value item : items) {
            @SuppressWarnings("unchecked")
            ValueProtos.Value value = item.toPb(itemType);
            builder.addItems(value);
        }
        return builder.build();
    }
}
