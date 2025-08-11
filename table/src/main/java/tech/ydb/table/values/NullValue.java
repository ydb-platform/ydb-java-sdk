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
            throw new NullPointerException("Cannot compare with null value");
        }

        if (other instanceof OptionalValue) {
            OptionalValue optional = (OptionalValue) other;
            if (!optional.isPresent()) {
                return 0;
            }
            return compareTo(optional.get());
        }

        if (!getType().equals(other.getType())) {
            throw new IllegalArgumentException("Cannot compare value " + getType() + " with " + other.getType());
        }

        // All NullValue instances are equal
        return 0;
    }
}
