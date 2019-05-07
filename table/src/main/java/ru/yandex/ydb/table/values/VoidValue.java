package ru.yandex.ydb.table.values;

import ru.yandex.ydb.ValueProtos;
import ru.yandex.ydb.table.types.VoidType;
import ru.yandex.ydb.table.values.proto.ProtoValue;


/**
 * @author Sergey Polovko
 */
public class VoidValue implements Value<VoidType> {

    private static final VoidValue INSTANCE = new VoidValue();

    private VoidValue() {
    }

    public static VoidValue of() {
        return INSTANCE;
    }

    @Override
    public boolean equals(Object o) {
        return o == this;
    }

    @Override
    public int hashCode() {
        return 1987;
    }

    @Override
    public String toString() {
        return "Void";
    }

    @Override
    public ValueProtos.Value toPb(VoidType type) {
        return ProtoValue.voidValue();
    }
}
