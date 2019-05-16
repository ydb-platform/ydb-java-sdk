package tech.ydb.table.types;

/**
 * @author Sergey Polovko
 */
public interface Type {

    Kind getKind();

    @Override
    boolean equals(Object o);

    @Override
    int hashCode();

    @Override
    String toString();

    default OptionalType makeOptional() {
        return OptionalType.of(this);
    }

    default Type unwrapOptional() {
        if (this instanceof OptionalType) {
            return ((OptionalType) this).getItemType();
        }
        throw new IllegalStateException("expected OptionalType, but was: " + this);
    }

    /**
     * KIND
     */
    enum Kind {
        PRIMITIVE,
        DECIMAL,
        OPTIONAL,
        LIST,
        TUPLE,
        STRUCT,
        DICT,
        VARIANT,
        VOID,
        ;
    }
}
