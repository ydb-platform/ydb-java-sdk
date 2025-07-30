package tech.ydb.table.values;

import java.io.Serializable;

import tech.ydb.proto.ValueProtos;

/**
 * @author Sergey Polovko
 * @param <T> type of value
 */
public interface Value<T extends Type> extends Serializable, Comparable<Value<?>> {

    Value<?>[] EMPTY_ARRAY = {};

    T getType();

    ValueProtos.Value toPb();

    /**
     * Compares this value with another value.
     * The comparison is based on the actual data type of the value stored.
     * For complex types like ListValue and StructValue, the comparison follows lexicographical rules.
     * For OptionalValue, comparison with non-optional values of the same underlying type is supported.
     * 
     * @param other the value to compare with
     * @return a negative integer, zero, or a positive integer as this value is less than, equal to, or greater than the other value
     * @throws IllegalArgumentException if the other value is null or has an incompatible type
     */
    int compareTo(Value<?> other);

    default PrimitiveValue asData() {
        return (PrimitiveValue) this;
    }

    default DictValue asDict() {
        return (DictValue) this;
    }

    default ListValue asList() {
        return (ListValue) this;
    }

    default OptionalValue asOptional() {
        return (OptionalValue) this;
    }

    /**
     * @deprecated Use {{@link #asStruct()}} instead
     */
    @Deprecated
    default StructValue asStuct() {
        return asStruct();
    }

    default StructValue asStruct() {
        return (StructValue) this;
    }

    default VariantValue asVariant() {
        return (VariantValue) this;
    }

    default VoidValue asVoid() {
        return (VoidValue) this;
    }

    default OptionalValue makeOptional() {
        return OptionalValue.of(this);
    }
}
