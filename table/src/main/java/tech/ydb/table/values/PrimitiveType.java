package tech.ydb.table.values;


import tech.ydb.ValueProtos;
import tech.ydb.table.values.proto.ProtoType;


/**
 * @author Sergey Polovko
 */
public enum PrimitiveType implements Type {
    Bool(ProtoType.getBool()),
    Int8(ProtoType.getInt8()),
    Uint8(ProtoType.getUint8()),
    Int16(ProtoType.getInt16()),
    Uint16(ProtoType.getUint16()),
    Int32(ProtoType.getInt32()),
    Uint32(ProtoType.getUint32()),
    Int64(ProtoType.getInt64()),
    Uint64(ProtoType.getUint64()),
    Float(ProtoType.getFloat()),
    Double(ProtoType.getDouble()),
    String(ProtoType.getString()),
    Utf8(ProtoType.getUtf8()),
    Yson(ProtoType.getYson()),
    Json(ProtoType.getJson()),
    Uuid(ProtoType.getUuid()),
    Date(ProtoType.getDate()),
    Datetime(ProtoType.getDatetime()),
    Timestamp(ProtoType.getTimestamp()),
    Interval(ProtoType.getInterval()),
    TzDate(ProtoType.getTzDate()),
    TzDatetime(ProtoType.getTzDatetime()),
    TzTimestamp(ProtoType.getTzTimestamp()),
    JsonDocument(ProtoType.getJsonDocument()),
    DyNumber(ProtoType.getDyNumber())
    ;

    private final ValueProtos.Type pbType;

    private PrimitiveType(ValueProtos.Type pbType) {
        this.pbType = pbType;
    }

    @Override
    public Kind getKind() {
        return Kind.PRIMITIVE;
    }

    @Override
    public ValueProtos.Type toPb() {
        return pbType;
    }
}
