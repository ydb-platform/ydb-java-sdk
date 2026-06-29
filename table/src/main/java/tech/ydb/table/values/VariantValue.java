package tech.ydb.table.values;

import java.util.Objects;

import tech.ydb.proto.ValueProtos;


/**
 * @author Sergey Polovko
 */
public class VariantValue implements Value<VariantType> {
    private static final long serialVersionUID = 5689895941787378526L;

    private final VariantType type;
    private final Value<?> item;
    private final int typeIndex;

    VariantValue(VariantType type, Value<?> item, int typeIndex) {
        this.type = type;
        this.item = Objects.requireNonNull(item, "item");
        this.typeIndex = typeIndex;
    }

    public int getTypeIndex() {
        return typeIndex;
    }

    public Value<?> getItem() {
        return item;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        VariantValue that = (VariantValue) o;
        if (typeIndex != that.typeIndex) {
            return false;
        }
        return item.equals(that.item);
    }

    @Override
    public int hashCode() {
        int h = Type.Kind.VARIANT.hashCode();
        h = 31 * h + typeIndex;
        return 31 * h + item.hashCode();
    }

    @Override
    public String toString() {
        return "Variant[" + typeIndex + "; " + item + ']';
    }

    @Override
    public VariantType getType() {
        return type;
    }

    @Override
    public ValueProtos.Value toPb() {
        ValueProtos.Value.Builder builder = ValueProtos.Value.newBuilder();
        builder.setNestedValue(item.toPb());
        builder.setVariantIndex(typeIndex);
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

        if (!getType().equals(other.getType())) {
            throw new IllegalArgumentException("Cannot compare value " + getType() + " with " + other.getType());
        }

        VariantValue variant = (VariantValue) other;

        // Compare type indices first
        int indexComparison = Integer.compare(typeIndex, variant.typeIndex);
        if (indexComparison != 0) {
            return indexComparison;
        }

        return item.compareTo(variant.item);
    }
}
