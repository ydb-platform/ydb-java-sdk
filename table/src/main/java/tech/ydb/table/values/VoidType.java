package tech.ydb.table.values;


/**
 * @author Sergey Polovko
 */
public final class VoidType implements Type {

    private static final VoidType INSTANCE = new VoidType();

    private VoidType() {
    }

    public static VoidType of() {
        return VoidType.INSTANCE;
    }

    @Override
    public Kind getKind() {
        return Kind.VOID;
    }

    @Override
    public boolean equals(Object o) {
        return this == o;
    }

    @Override
    public int hashCode() {
        return 31 * Kind.VOID.hashCode();
    }

    @Override
    public String toString() {
        return "Void";
    }
}
