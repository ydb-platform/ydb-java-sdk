package tech.ydb.table.values;

import java.util.Arrays;
import java.util.List;


/**
 * @author Sergey Polovko
 */
public final class TupleType implements Type {

    private static final TupleType EMPTY = new TupleType(Type.EMPTY_ARRAY);

    private final Type[] elementTypes;

    private TupleType(Type... elementTypes) {
        this.elementTypes = elementTypes;
    }

    public static TupleType empty() {
        return TupleType.EMPTY;
    }

    public static TupleType of(Type elementType) {
        return new TupleType(elementType);
    }

    public static TupleType ofCopy(Type... elementTypes) {
        return new TupleType(elementTypes.clone());
    }

    /**
     * will not clone given array
     */
    public static TupleType ofOwn(Type... elementTypes) {
        return new TupleType(elementTypes);
    }

    public static TupleType of(List<Type> elementTypes) {
        return new TupleType(elementTypes.toArray(Type.EMPTY_ARRAY));
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

    public TupleValue newValue(Value item) {
        return new TupleValue(this, item);
    }

    public TupleValue newValue(Value a, Value b) {
        return new TupleValue(this, a, b);
    }

    public TupleValue newValue(Value a, Value b, Value c) {
        return new TupleValue(this, a, b, c);
    }

    public TupleValue newValue(Value a, Value b, Value c, Value d) {
        return new TupleValue(this, a, b, c, d);
    }

    public TupleValue newValue(Value a, Value b, Value c, Value d, Value e) {
        return new TupleValue(this, a, b, c, d, e);
    }

    public TupleValue newValue(List<Value> items) {
        return new TupleValue(this, items.toArray(Value.EMPTY_ARRAY));
    }

    public TupleValue newValueCopy(Value... items) {
        return new TupleValue(this, items.clone());
    }

    public TupleValue newValueOwn(Value... items) {
        return new TupleValue(this, items);
    }
}
