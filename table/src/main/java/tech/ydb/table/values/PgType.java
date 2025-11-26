package tech.ydb.table.values;

import java.util.Objects;

import tech.ydb.proto.ValueProtos;
import tech.ydb.table.values.proto.ProtoType;

/**
 *
 * @author Aleksandr Gorshenin
 */
public class PgType implements Type {
    private static final long serialVersionUID = -645693159447347308L;
    private final int oid;
    private final int typlen;
    private final int typmod;

    private PgType(int oid, int typlen, int typmod) {
        this.oid = oid;
        this.typlen = typlen;
        this.typmod = typmod;
    }

    public static PgType of(int oid) {
        return new PgType(oid, 0, 0);
    }

    public static PgType of(int oid, int typlen, int typmod) {
        return new PgType(oid, typlen, typmod);
    }

    public int getOid() {
        return oid;
    }

    @Override
    public Kind getKind() {
        return Kind.PG_TYPE;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        PgType that = (PgType) o;
        return oid == that.oid && typlen == that.typlen && typmod == that.typmod;
    }

    @Override
    public int hashCode() {
        return Objects.hash(oid, typlen, typmod);
    }

    @Override
    public String toString() {
        return "PgType[" + oid + "]";
    }

    @Override
    public ValueProtos.Type toPb() {
        return ProtoType.getPgType(oid, typlen, typmod);
    }
}
