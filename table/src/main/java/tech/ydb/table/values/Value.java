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

    default TupleValue asTuple() {
        return (TupleValue) this;
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
