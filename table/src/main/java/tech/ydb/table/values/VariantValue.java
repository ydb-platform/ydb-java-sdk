package tech.ydb.table.values;

import java.util.Objects;

import tech.ydb.ValueProtos;
import tech.ydb.table.types.Type;
import tech.ydb.table.types.VariantType;


/**
 * @author Sergey Polovko
 */
public class VariantValue implements Value<VariantType> {
    private final int typeIndex;
    private final Value item;

    private VariantValue(int typeIndex, Value item) {
        this.typeIndex = typeIndex;
        this.item = Objects.requireNonNull(item, "item");
    }

    public static VariantValue of(int typeIndex, Value item) {
        return new VariantValue(typeIndex, item);
    }

    public int getTypeIndex() {
        return typeIndex;
    }

    public Value getItem() {
        return item;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

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
        return "Variant[" + typeIndex + "; " + item.toString() + ']';
    }

    @Override
    public ValueProtos.Value toPb(VariantType type) {
        Type itemType = type.getItemType(typeIndex);
        @SuppressWarnings("unchecked")
        ValueProtos.Value itemValue = item.toPb(itemType);
        ValueProtos.Value.Builder builder = ValueProtos.Value.newBuilder();
        builder.setVariantIndex(typeIndex);
        builder.setNestedValue(itemValue);
        return builder.build();
    }
}
