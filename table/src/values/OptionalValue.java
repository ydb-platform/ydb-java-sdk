package ru.yandex.ydb.table.values;

import java.util.NoSuchElementException;
import java.util.Objects;

import ru.yandex.ydb.ValueProtos;
import ru.yandex.ydb.table.types.OptionalType;
import ru.yandex.ydb.table.types.Type;
import ru.yandex.ydb.table.values.proto.ProtoValue;


/**
 * @author Sergey Polovko
 */
public class OptionalValue implements Value<OptionalType> {

    private static final OptionalValue EMPTY = new OptionalValue();

    private final Value value;

    private OptionalValue() {
        this.value = null;
    }

    private OptionalValue(Value value) {
        this.value = Objects.requireNonNull(value, "value");
    }

    public static OptionalValue empty() {
        return EMPTY;
    }

    public static OptionalValue of(Value value) {
        return new OptionalValue(value);
    }

    public boolean isPresent() {
        return value != null;
    }

    public Value get() {
        if (value == null) {
            throw new NoSuchElementException("No value present");
        }
        return value;
    }

    public Value orElse(Value other) {
        return value != null ? value : other;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        OptionalValue that = (OptionalValue) o;
        if (value == null) {
            return that.value == null;
        }
        return value.equals(that.value);
    }

    @Override
    public int hashCode() {
        if (value == null) {
            return 2017;
        }
        return 31 * Type.Kind.OPTIONAL.hashCode() + value.hashCode();
    }

    @Override
    public String toString() {
        if (value == null) {
            return "Empty[]";
        }
        return "Some[" + value.toString() + ']';
    }

    @Override
    public ValueProtos.Value toPb(OptionalType type) {
        if (isPresent()) {
            Type itemType = type.getItemType();
            @SuppressWarnings("unchecked")
            ValueProtos.Value value = get().toPb(itemType);
            return ProtoValue.optional(value);
        }

        return ProtoValue.optional();
    }
}
