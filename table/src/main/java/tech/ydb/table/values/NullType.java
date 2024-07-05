package tech.ydb.table.values;


import tech.ydb.proto.ValueProtos;
import tech.ydb.table.values.proto.ProtoType;


/**
 * @author Aleksandr Gorshenin
 */
public final class NullType implements Type {

    private static final NullType INSTANCE = new NullType();

    private NullType() {
    }

    public static NullType of() {
        return NullType.INSTANCE;
    }

    @Override
    public Kind getKind() {
        return Kind.NULL;
    }

    @Override
    public String toString() {
        return "Null";
    }

    @Override
    public ValueProtos.Type toPb() {
        return ProtoType.getNull();
    }
}
