package tech.ydb.table.values;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import javax.annotation.Nullable;

import tech.ydb.proto.ValueProtos;
import tech.ydb.table.values.proto.ProtoValue;


/**
 * @author Sergey Polovko
 */
public class DictValue implements Value<DictType> {
    private static final long serialVersionUID = 6205349432501922070L;

    private final DictType type;
    private final HashMap<Value<?>, Value<?>> items;

    DictValue(DictType type, HashMap<Value<?>, Value<?>> items) {
        this.type = type;
        this.items = items;
    }

    public static DictValue of(Value<?> key, Value<?> value) {
        HashMap<Value<?>, Value<?>> map = new HashMap<>();
        map.put(key, value);
        return new DictValue(DictType.of(key.getType(), value.getType()), map);
    }

    public int size() {
        return items.size();
    }

    public boolean isEmpty() {
        return items.isEmpty();
    }

    public boolean contains(Value<?> key) {
        return items.containsKey(key);
    }

    @Nullable
    public Value<?> get(Value<?> key) {
        return items.get(key);
    }

    public Set<Value<?>> keySet() {
        return items.keySet();
    }

    public Collection<Value<?>> values() {
        return items.values();
    }

    public Set<Map.Entry<Value<?>, Value<?>>> entrySet() {
        return items.entrySet();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        DictValue that = (DictValue) o;
        return items.equals(that.items);
    }

    @Override
    public int hashCode() {
        return 31 * Type.Kind.DICT.hashCode() + items.hashCode();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Dict[");
        for (Map.Entry<Value<?>, Value<?>> e : items.entrySet()) {
            sb.append(e.getKey()).append(": ");
            sb.append(e.getValue()).append(", ");
        }
        if (!items.isEmpty()) {
            sb.setLength(sb.length() - 2);
        }
        sb.append(']');
        return sb.toString();
    }

    @Override
    public DictType getType() {
        return type;
    }

    @Override
    public ValueProtos.Value toPb() {
        if (isEmpty()) {
            return ProtoValue.dict();
        }

        ValueProtos.Value.Builder builder = ValueProtos.Value.newBuilder();
        for (Map.Entry<Value<?>, Value<?>> e : items.entrySet()) {
            ValueProtos.Value key = e.getKey().toPb();
            ValueProtos.Value value = e.getValue().toPb();

            builder.addPairsBuilder()
                .setKey(key)
                .setPayload(value);
        }
        return builder.build();
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

        DictValue otherDict = (DictValue) other;

        // Sort entries by keys
        Set<Value<?>> keys = new TreeSet<>();
        keys.addAll(items.keySet());
        keys.addAll(otherDict.keySet());

        for (Value<?> key: keys) {
            if (!otherDict.items.containsKey(key)) {
                return 1;
            }
            if (!items.containsKey(key)) {
                return -1;
            }

            int valueComparison = items.get(key).compareTo(otherDict.items.get(key));
            if (valueComparison != 0) {
                return valueComparison;
            }
        }

        return 0;
    }
}
