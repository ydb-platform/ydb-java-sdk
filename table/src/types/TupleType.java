package ru.yandex.ydb.table.types;

import java.util.Arrays;
import java.util.List;


/**
 * @author Sergey Polovko
 */
public final class TupleType implements Type {

    private static final TupleType EMPTY = new TupleType(new Type[0]);

    private final Type[] elementTypes;

    private TupleType(Type[] elementTypes) {
        this.elementTypes = elementTypes;
    }

    public static TupleType empty() {
        return TupleType.EMPTY;
    }

    public static TupleType of() {
        return TupleType.EMPTY;
    }

    public static TupleType of(Type elementType) {
        return new TupleType(new Type[] { elementType });
    }

    public static TupleType of(Type... elementTypes) {
        return new TupleType(elementTypes.clone());
    }

    /**
     * will not clone given array
     */
    public static TupleType ofOwning(Type... elementTypes) {
        return new TupleType(elementTypes);
    }

    public static TupleType of(List<Type> elementTypes) {
        return new TupleType(elementTypes.toArray(new Type[0]));
    }

    public int getElementsCount() {
        return elementTypes.length;
    }

    public Type getElementType(int index) {
        return elementTypes[index];
    }

    @Override
    public Kind getKind() {
        return Kind.TUPLE;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || o.getClass() != TupleType.class) return false;

        TupleType tupleType = (TupleType) o;
        if (getElementsCount() != tupleType.getElementsCount()) {
            return false;
        }
        for (int i = 0; i < getElementsCount(); i++) {
            if (!elementTypes[i].equals(tupleType.getElementType(i))) {
                return false;
            }
        }
        return true;
    }

    @Override
    public int hashCode() {
        return 31 * Kind.TUPLE.hashCode() + Arrays.hashCode(elementTypes);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Tuple<");
        for (Type elementType : elementTypes) {
            sb.append(elementType).append(", ");
        }
        if (elementTypes.length != 0) {
            sb.setLength(sb.length() - 2); // drop last comma
        }
        sb.append('>');
        return sb.toString();
    }
}
