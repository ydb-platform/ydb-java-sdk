package tech.ydb.table.types;

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
}
