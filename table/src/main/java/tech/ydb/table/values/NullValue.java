package tech.ydb.table.values;

import tech.ydb.proto.ValueProtos;
import tech.ydb.table.values.proto.ProtoValue;


/**
 * @author Aleksandr Gorshenin
 */
public class NullValue implements Value<NullType> {
    private static final NullValue INSTANCE = new NullValue();
    private static final long serialVersionUID = 7394540932620428882L;

    private NullValue() {
    }

    public static NullValue of() {
        return INSTANCE;
    }

    @Override
    public String toString() {
        return "Null";
    }

    @Override
    public NullType getType() {
        return NullType.of();
    }

    @Override
    public ValueProtos.Value toPb() {
        return ProtoValue.nullValue();
    }

    @Override
    public int compareTo(Value<?> other) {
        if (other == null) {
            throw new IllegalArgumentException("Cannot compare with null value");
        }

        // Handle comparison with OptionalValue
        if (other instanceof OptionalValue) {
            OptionalValue otherOptional = (OptionalValue) other;

            // Check that the item type matches this null type
            if (!getType().equals(otherOptional.getType().getItemType())) {
                throw new IllegalArgumentException(
                    "Cannot compare NullValue with OptionalValue of different item type: " +
                    getType() + " vs " + otherOptional.getType().getItemType());
            }

            // Non-empty value is greater than empty optional
            if (!otherOptional.isPresent()) {
                return 1;
            }

            // Compare with the wrapped value
            return compareTo(otherOptional.get());
        }

        if (!(other instanceof NullValue)) {
            throw new IllegalArgumentException("Cannot compare NullValue with " + other.getClass().getSimpleName());
        }

        // All NullValue instances are equal
        return 0;
    }
}
