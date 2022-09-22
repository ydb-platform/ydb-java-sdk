package tech.ydb.table.values;

import java.util.List;

import tech.ydb.ValueProtos;
import tech.ydb.table.values.proto.ProtoType;


/**
 * @author Sergey Polovko
 */
public final class VariantType implements Type {

    private final Type[] itemTypes;

    private VariantType(Type... itemTypes) {
        this.itemTypes = itemTypes;
    }

    public static VariantType ofCopy(Type... itemTypes) {
        return new VariantType(itemTypes.clone());
    }

    /**
     * will not clone given array
     */
    public static VariantType ofOwn(Type... itemTypes) {
        return new VariantType(itemTypes);
    }

    public static VariantType of(List<Type> itemTypes) {
        return new VariantType(itemTypes.toArray(Type.EMPTY_ARRAY));
    }

    public int getItemsCount() {
        return itemTypes.length;
    }

    public Type getItemType(int index) {
        return itemTypes[index];
    }

    @Override
    public Kind getKind() {
        return Kind.VARIANT;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || o.getClass() != VariantType.class) {
            return false;
        }

        VariantType variantType = (VariantType) o;
        int itemsCount = getItemsCount();
        if (itemsCount != variantType.getItemsCount()) {
            return false;
        }
        for (int i = 0; i < itemsCount; i++) {
            if (!getItemType(i).equals(variantType.getItemType(i))) {
                return false;
            }
        }
        return true;
    }

    @Override
    public int hashCode() {
        int h = 31 * Kind.VARIANT.hashCode();
        for (int i = 0, count = getItemsCount(); i < count; i++) {
            h = 31 * h + getItemType(i).hashCode();
        }
        return h;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(128);
        sb.append("Variant<");
        int count = getItemsCount();
        for (int i = 0; i < count; i++) {
            sb.append(getItemType(i)).append(", ");
        }
        if (count != 0) {
            sb.setLength(sb.length() - 1); // cut last comma
        }
        sb.append('>');
        return sb.toString();
    }

    @Override
    public ValueProtos.Type toPb() {
        ValueProtos.TupleType.Builder tupleType = ValueProtos.TupleType.newBuilder();
        for (Type itemType : itemTypes) {
            tupleType.addElements(itemType.toPb());
        }
        return ProtoType.getVariant(tupleType.build());
    }

    public VariantValue newValue(Value item, int typeIndex) {
        return new VariantValue(this, item, typeIndex);
    }
}
