package tech.ydb.table.values;


import java.util.List;

import tech.ydb.ValueProtos;
import tech.ydb.table.values.proto.ProtoType;


/**
 * @author Sergey Polovko
 */
public final class ListType implements Type {

    private final Type itemType;

    private ListType(Type itemType) {
        this.itemType = itemType;
    }

    public static ListType of(Type itemType) {
        return new ListType(itemType);
    }

    public Type getItemType() {
        return itemType;
    }

    @Override
    public Kind getKind() {
        return Kind.LIST;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || o.getClass() != ListType.class) return false;

        ListType listType = (ListType) o;
        return itemType.equals(listType.getItemType());
    }

    @Override
    public int hashCode() {
        return 31 * Kind.LIST.hashCode() + itemType.hashCode();
    }

    @Override
    public String toString() {
        return "List<" + itemType + '>';
    }

    @Override
    public ValueProtos.Type toPb() {
        return ProtoType.getList(itemType.toPb());
    }

    public ListValue emptyValue() {
        return new ListValue(this, Value.EMPTY_ARRAY);
    }

    public ListValue newValue(List<Value<?>> items) {
        if (items.isEmpty()) {
            return emptyValue();
        }
        return new ListValue(this, items.toArray(Value.EMPTY_ARRAY));
    }

    public ListValue newValueCopy(Value<?>[] items) {
        if (items.length == 0) {
            return emptyValue();
        }
        return newValueOwn(items.clone());
    }

    public ListValue newValueOwn(Value<?>... items) {
        if (items.length == 0) {
            return emptyValue();
        }
        return new ListValue(this, items);
    }
}
