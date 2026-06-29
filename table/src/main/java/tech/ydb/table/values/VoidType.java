package tech.ydb.table.values;


import tech.ydb.proto.ValueProtos;
import tech.ydb.table.values.proto.ProtoType;


/**
 * @author Sergey Polovko
 */
public final class VoidType implements Type {
    private static final VoidType INSTANCE = new VoidType();
    private static final long serialVersionUID = 2865013061318532939L;

    private VoidType() {
    }

    public static VoidType of() {
        return VoidType.INSTANCE;
    }

    @Override
    public Kind getKind() {
        return Kind.VOID;
    }

    @Override
    public String toString() {
        return "Void";
    }

    @Override
    public ValueProtos.Type toPb() {
        return ProtoType.getVoid();
    }
}
