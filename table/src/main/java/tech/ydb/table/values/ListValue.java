package tech.ydb.table.values;

import java.util.Arrays;

import tech.ydb.proto.ValueProtos;
import tech.ydb.table.values.proto.ProtoValue;


/**
 * @author Sergey Polovko
 */
public class ListValue implements Value<ListType> {
    private static final long serialVersionUID = 8672625561012902978L;

    private final ListType type;
    private final Value<?>[] items;

    ListValue(ListType type, Value<?>... items) {
        this.type = type;
        this.items = items;
    }

    public static ListValue empty(Type type) {
        return new ListValue(ListType.of(type));
    }

    public static ListValue of(Value<?>... items) {
        if (items.length == 0) {
            return new ListValue(ListType.of(VoidType.of()));
        }
        return new ListValue(ListType.of(items[0].getType()), items);
    }

    public static ListValue of(Value<?> item) {
        return new ListValue(ListType.of(item.getType()), item);
    }

    public static ListValue of(Value<?> a, Value<?> b) {
        return new ListValue(ListType.of(a.getType()), a, b);
    }

    public static ListValue of(Value<?> a, Value<?> b, Value<?> c) {
        return new ListValue(ListType.of(a.getType()), a, b, c);
    }

    public static ListValue of(Value<?> a, Value<?> b, Value<?> c, Value<?> d) {
        return new ListValue(ListType.of(a.getType()), a, b, c, d);
    }

    public static ListValue of(Value<?> a, Value<?> b, Value<?> c, Value<?> d, Value<?> e) {
        return new ListValue(ListType.of(a.getType()), a, b, c, d, e);
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
    public ListType getType() {
        return type;
    }

    @Override
    public ValueProtos.Value toPb() {
        if (isEmpty()) {
            return ProtoValue.list();
        }

        ValueProtos.Value.Builder builder = ValueProtos.Value.newBuilder();
        for (Value<?> item : items) {
            builder.addItems(item.toPb());
        }
        return builder.build();
    }

    @Override
    public int compareTo(Value<?> other) {
        if (other == null) {
            throw new IllegalArgumentException("Cannot compare with null value");
        }
        
        if (!(other instanceof ListValue)) {
            throw new IllegalArgumentException("Cannot compare ListValue with " + other.getClass().getSimpleName());
        }
        
        ListValue otherList = (ListValue) other;
        
        // Compare elements lexicographically
        int minLength = Math.min(items.length, otherList.items.length);
        for (int i = 0; i < minLength; i++) {
            Value<?> thisItem = items[i];
            Value<?> otherItem = otherList.items[i];
            
            int itemComparison = compareValues(thisItem, otherItem);
            if (itemComparison != 0) {
                return itemComparison;
            }
        }
        
        // If we reach here, one list is a prefix of the other
        // The shorter list comes first
        return Integer.compare(items.length, otherList.items.length);
    }
    
    private static int compareValues(Value<?> a, Value<?> b) {
        // Handle null values
        if (a == null && b == null) return 0;
        if (a == null) return -1;
        if (b == null) return 1;
        
        // Check that the types are the same
        if (!a.getType().equals(b.getType())) {
            throw new IllegalArgumentException("Cannot compare values of different types: " + 
                a.getType() + " vs " + b.getType());
        }
        
        // Use the actual compareTo method of the values
        if (a instanceof Comparable && b instanceof Comparable) {
            try {
                return ((Comparable<Value<?>>) a).compareTo((Value<?>) b);
            } catch (ClassCastException e) {}
        }
        
        throw new IllegalArgumentException("Cannot compare values of different types: " + 
            a.getClass().getSimpleName() + " vs " + b.getClass().getSimpleName());
    }
}
