package tech.ydb.table.values;

import tech.ydb.proto.ValueProtos;
import tech.ydb.table.values.proto.ProtoValue;


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
    public VoidType getType() {
        return VoidType.of();
    }

    @Override
    public ValueProtos.Value toPb() {
        return ProtoValue.voidValue();
    }
}
