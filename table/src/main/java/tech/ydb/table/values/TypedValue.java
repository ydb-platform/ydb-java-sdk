package tech.ydb.table.values;

import tech.ydb.ValueProtos;
import tech.ydb.table.values.proto.ProtoType;


/**
 * @author Sergey Polovko
 */
public class TypedValue<T extends Type> {

    private final T type;
    private final Value<T> value;

    public TypedValue(T type, Value<T> value) {
        this.type = type;
        this.value = value;
    }

    public T getType() {
        return type;
    }

    public Value<T> getValue() {
        return value;
    }

    public ValueProtos.TypedValue toPb() {
        return ValueProtos.TypedValue.newBuilder()
            .setType(ProtoType.toPb(type))
            .setValue(value.toPb())
            .build();
    }
}