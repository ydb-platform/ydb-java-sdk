package ru.yandex.ydb.table.values;

import ru.yandex.ydb.ValueProtos;
import ru.yandex.ydb.table.types.Type;


/**
 * @author Sergey Polovko
 */
public interface Value<T extends Type> {

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

    default StructValue asStuct() {
        return (StructValue) this;
    }

    default VariantValue asVariant() {
        return (VariantValue) this;
    }

    default VoidValue asVoid() {
        return (VoidValue) this;
    }

    ValueProtos.Value toPb(T type);
}
