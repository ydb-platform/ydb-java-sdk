package tech.ydb.table.values;

import tech.ydb.proto.ValueProtos;
import tech.ydb.table.values.proto.ProtoValue;


/**
 * @author Sergey Polovko
 */
public class VoidValue implements Value<VoidType> {
    private static final VoidValue INSTANCE = new VoidValue();
    private static final long serialVersionUID = -6324130072423013933L;

    private VoidValue() {
    }

    public static VoidValue of() {
        return INSTANCE;
    }

    @Override
    public String toString() {
        return "Void";
    }

    @Override
    public VoidType getType() {
        return VoidType.of();
    }

    @Override
    public ValueProtos.Value toPb() {
        return ProtoValue.voidValue();
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

        // All VoidValue instances are equal
        return 0;
    }
}
