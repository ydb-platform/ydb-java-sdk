package tech.ydb.table.values;

import java.util.NoSuchElementException;
import java.util.Objects;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import tech.ydb.proto.ValueProtos;
import tech.ydb.table.values.proto.ProtoValue;


/**
 * @author Sergey Polovko
 */
@ParametersAreNonnullByDefault
public class OptionalValue implements Value<OptionalType> {

    private final OptionalType type;
    @Nullable
    private final Value<?> value;

    OptionalValue(OptionalType type, @Nullable Value<?> value) {
        this.type = type;
        this.value = value;
    }

    public static OptionalValue of(Value<?> value) {
        return new OptionalValue(
            OptionalType.of(value.getType()),
            Objects.requireNonNull(value, "value"));
    }

    public boolean isPresent() {
        return value != null;
    }

    public Value<?> get() {
        if (value == null) {
            throw new NoSuchElementException("No value present");
        }
        return value;
    }

    public Value<?> orElse(Value<?> other) {
        return value != null ? value : other;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

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
        return "Some[" + value + ']';
    }

    @Override
    public OptionalType getType() {
        return type;
    }

    @Override
    public ValueProtos.Value toPb() {
        if (isPresent()) {
            ValueProtos.Value pb = get().toPb();
            boolean implicit = type.getItemType().getKind() == Type.Kind.PRIMITIVE
                    || type.getItemType().getKind() == Type.Kind.DECIMAL;
            return implicit ? pb : ProtoValue.optional(pb);
        }

        return ProtoValue.optional();
    }
}
