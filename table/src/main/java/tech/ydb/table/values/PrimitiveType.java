package tech.ydb.table.values;


import tech.ydb.ValueProtos;
import tech.ydb.table.values.proto.ProtoType;


/**
 * @author Sergey Polovko
 */
public enum PrimitiveType implements Type {
    /** Boolean value. */
    Bool(ProtoType.getBool()),
    /** A signed integer. Acceptable values: from -2^7 to 2^7–1. Not supported for table columns */
    Int8(ProtoType.getInt8()),
    /** An unsigned integer. Acceptable values: from 0 to 2^8–1. */
    Uint8(ProtoType.getUint8()),
    /** A signed integer. Acceptable values: from –2^15 to 2^15–1. Not supported for table columns */
    Int16(ProtoType.getInt16()),
    /** An unsigned integer. Acceptable values: from 0 to 2^16–1. Not supported for table columns */
    Uint16(ProtoType.getUint16()),
    /** A signed integer. Acceptable values: from –2^31 to 2^31–1. */
    Int32(ProtoType.getInt32()),
    /** An unsigned integer. Acceptable values: from 0 to 2^32–1. */
    Uint32(ProtoType.getUint32()),
    /** A signed integer. Acceptable values: from –2^63 to 2^63–1. */
    Int64(ProtoType.getInt64()),
    /** An unsigned integer. Acceptable values: from 0 to 2^64–1. */
    Uint64(ProtoType.getUint64()),
    /** A real number with variable precision, 4 bytes in size. Can't be used in the primary key */
    Float(ProtoType.getFloat()),
    /** A real number with variable precision, 8 bytes in size. Can't be used in the primary key */
    Double(ProtoType.getDouble()),
    /** A binary data, synonym for YDB type String */
    Bytes(ProtoType.getBytes()),
    /** Text encoded in UTF-8, synonym for YDB type Utf8 */
    Text(ProtoType.getText()),
    /** YSON in a textual or binary representation. Doesn't support matching, can't be used in the primary key */
    Yson(ProtoType.getYson()),
    /** JSON represented as text. Doesn't support matching, can't be used in the primary key */
    Json(ProtoType.getJson()),
    /** Universally unique identifier UUID. Not supported for table columns */
    Uuid(ProtoType.getUuid()),
    /** Date, precision to the day */
    Date(ProtoType.getDate()),
    /** Date/time, precision to the second */
    Datetime(ProtoType.getDatetime()),
    /** Date/time, precision to the microsecond */
    Timestamp(ProtoType.getTimestamp()),
    /** Time interval (signed), precision to microseconds */
    Interval(ProtoType.getInterval()),
    /** Date with time zone label, precision to the day */
    TzDate(ProtoType.getTzDate()),
    /** Date/time with time zone label, precision to the second */
    TzDatetime(ProtoType.getTzDatetime()),
    /** Date/time with time zone label, precision to the microsecond */
    TzTimestamp(ProtoType.getTzTimestamp()),
    /** JSON in an indexed binary representation. Doesn't support matching, can't be used in the primary key */
    JsonDocument(ProtoType.getJsonDocument()),
    /** A binary representation of a real number with an accuracy of up to 38 digits.
      * Acceptable values: positive numbers from 1×10-130 up to 1×10126–1,
      * negative numbers from -1×10126–1 to -1×10-130, and 0.
      * Compatible with the Number type in AWS DynamoDB.
      * It's not recommended for ydb-native applications.
      */
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
