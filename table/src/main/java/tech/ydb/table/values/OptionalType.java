package tech.ydb.table.values;

import java.util.Objects;

import tech.ydb.ValueProtos;
import tech.ydb.table.values.proto.ProtoType;


/**
 * @author Sergey Polovko
 */
public final class OptionalType implements Type {

    private final Type itemType;

    private OptionalType(Type itemType) {
        this.itemType = itemType;
    }

    public static OptionalType of(Type itemType) {
        return new OptionalType(itemType);
    }

    public Type getItemType() {
        return itemType;
    }

    @Override
    public Kind getKind() {
        return Kind.OPTIONAL;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || o.getClass() != OptionalType.class) return false;

        OptionalType optionalType = (OptionalType) o;
        return itemType.equals(optionalType.getItemType());
    }

    @Override
    public int hashCode() {
        return 31 * Kind.OPTIONAL.hashCode() + itemType.hashCode();
    }

    @Override
    public String toString() {
        return String.valueOf(itemType) + '?';
    }

    @Override
    public ValueProtos.Type toPb() {
        return ProtoType.optional(itemType.toPb());
    }

    public OptionalValue emptyValue() {
        return new OptionalValue(this, null);
    }

    public OptionalValue newValue(Value item) {
        return new OptionalValue(this, Objects.requireNonNull(item, "item"));
    }
}
