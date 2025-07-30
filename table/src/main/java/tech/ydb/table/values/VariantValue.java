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
            throw new IllegalArgumentException("Cannot compare with null value");
        }

        // Handle comparison with OptionalValue
        if (other instanceof OptionalValue) {
            OptionalValue otherOptional = (OptionalValue) other;

            // Check that the item type matches this variant type
            if (!getType().equals(otherOptional.getType().getItemType())) {
                throw new IllegalArgumentException(
                    "Cannot compare VariantValue with OptionalValue of different item type: " +
                    getType() + " vs " + otherOptional.getType().getItemType());
            }

            // Non-empty value is greater than empty optional
            if (!otherOptional.isPresent()) {
                return 1;
            }

            // Compare with the wrapped value
            return compareTo(otherOptional.get());
        }

        if (!(other instanceof VariantValue)) {
            throw new IllegalArgumentException("Cannot compare VariantValue with " + other.getClass().getSimpleName());
        }

        VariantValue otherVariant = (VariantValue) other;

        // Compare type indices first
        int indexComparison = Integer.compare(typeIndex, otherVariant.typeIndex);
        if (indexComparison != 0) {
            return indexComparison;
        }

        // If type indices are the same, compare the items
        return compareValues(item, otherVariant.item);
    }

    private static int compareValues(Value<?> a, Value<?> b) {
        // Handle null values
        if (a == null && b == null) {
            return 0;
        }
        if (a == null) {
            return -1;
        }
        if (b == null) {
            return 1;
        }

        // Check that the types are the same
        if (!a.getType().equals(b.getType())) {
            throw new IllegalArgumentException("Cannot compare values of different types: " +
                a.getType() + " vs " + b.getType());
        }

        // Use the actual compareTo method of the values
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
