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
    private static final long serialVersionUID = -6012287716342869258L;

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

    @Override
    public int compareTo(Value<?> other) {
        if (other == null) {
            throw new IllegalArgumentException("Cannot compare with null value");
        }

        // Handle comparison with another OptionalValue
        if (other instanceof OptionalValue) {
            OptionalValue otherOptional = (OptionalValue) other;

            // Check that the item types are the same
            if (!type.getItemType().equals(otherOptional.type.getItemType())) {
                throw new IllegalArgumentException("Cannot compare OptionalValue with different item types: " +
                    type.getItemType() + " vs " + otherOptional.type.getItemType());
            }

            // Handle empty values: empty values are considered less than non-empty values
            if (value == null && otherOptional.value == null) {
                return 0;
            }
            if (value == null) {
                return -1;
            }
            if (otherOptional.value == null) {
                return 1;
            }

            // Both values are non-null and have the same type, compare them using their compareTo method
            return compareValues(value, otherOptional.value);
        }

        // Handle comparison with non-optional values of the same underlying type
        if (type.getItemType().equals(other.getType())) {
            // This OptionalValue is empty, so it's less than any non-optional value
            if (value == null) {
                return -1;
            }

            // This OptionalValue has a value, compare it with the non-optional value
            return compareValues(value, other);
        }

        // Types are incompatible
        throw new IllegalArgumentException("Cannot compare OptionalValue with incompatible type: " +
            type.getItemType() + " vs " + other.getType());
    }

    private static int compareValues(Value<?> a, Value<?> b) {
        // Since we've already verified the types are the same, we can safely cast
        // and use the compareTo method of the actual value type
        if (a instanceof Comparable && b instanceof Comparable) {
            try {
                return ((Comparable<Value<?>>) a).compareTo((Value<?>) b);
            } catch (ClassCastException e) {
                // Fall back to error
            }
        }
        throw new IllegalArgumentException("Cannot compare values of different types: " +
            a.getClass().getSimpleName() + " vs " + b.getClass().getSimpleName());
    }
}
