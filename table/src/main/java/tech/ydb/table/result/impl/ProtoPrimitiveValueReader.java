package tech.ydb.table.result.impl;

import java.nio.charset.Charset;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.UUID;

import com.google.protobuf.ByteString;

import tech.ydb.proto.ValueProtos;
import tech.ydb.proto.ValueProtos.Type.PrimitiveTypeId;
import tech.ydb.table.utils.Hex;
import tech.ydb.table.values.DecimalValue;
import tech.ydb.table.values.PrimitiveType;
import tech.ydb.table.values.proto.ProtoValue;


/**
 * @author Sergey Polovko
 */
class ProtoPrimitiveValueReader extends AbstractValueReader {

    private final ValueProtos.Type type;
    private final PrimitiveTypeId primitiveTypeId;
    private ValueProtos.Value value;

    ProtoPrimitiveValueReader(ValueProtos.Type type) {
        this.type = type;
        this.primitiveTypeId = type.getTypeId();
    }

    @Override
    protected ValueProtos.Type getProtoType() {
        return type;
    }

    @Override
    protected ValueProtos.Value getProtoValue() {
        return value;
    }

    @Override
    protected void setProtoValue(ValueProtos.Value value) {
        this.value = value;
    }

    @Override
    public boolean getBool() {
        checkPrimitive(PrimitiveTypeId.BOOL, PrimitiveType.Bool.name());
        return value.getBoolValue();
    }

    @Override
    public byte getInt8() {
        checkPrimitive(PrimitiveTypeId.INT8, PrimitiveType.Int8.name());
        return (byte) value.getInt32Value();
    }

    @Override
    public int getUint8() {
        checkPrimitive(PrimitiveTypeId.UINT8, PrimitiveType.Uint8.name());
        return 0xFF & value.getUint32Value();
    }

    @Override
    public short getInt16() {
        checkPrimitive(PrimitiveTypeId.INT16, PrimitiveType.Int16.name());
        return (short) value.getInt32Value();
    }

    @Override
    public int getUint16() {
        checkPrimitive(PrimitiveTypeId.UINT16, PrimitiveType.Uint16.name());
        return 0xFFFF & value.getUint32Value();
    }

    @Override
    public int getInt32() {
        checkPrimitive(PrimitiveTypeId.INT32, PrimitiveType.Int32.name());
        return value.getInt32Value();
    }

    @Override
    public long getUint32() {
        checkPrimitive(PrimitiveTypeId.UINT32, PrimitiveType.Uint32.name());
        return 0xFFFFFFFFL & value.getUint32Value();
    }

    @Override
    public long getInt64() {
        checkPrimitive(PrimitiveTypeId.INT64, PrimitiveType.Int64.name());
        return value.getInt64Value();
    }

    @Override
    public long getUint64() {
        checkPrimitive(PrimitiveTypeId.UINT64, PrimitiveType.Uint64.name());
        return value.getUint64Value();
    }

    @Override
    public float getFloat() {
        checkPrimitive(PrimitiveTypeId.FLOAT, PrimitiveType.Float.name());
        return value.getFloatValue();
    }

    @Override
    public double getDouble() {
        checkPrimitive(PrimitiveTypeId.DOUBLE, PrimitiveType.Double.name());
        return value.getDoubleValue();
    }

    @Override
    public LocalDate getDate() {
        checkPrimitive(PrimitiveTypeId.DATE, PrimitiveType.Date.name());
        return ProtoValue.toDate(value);
    }

    @Override
    public LocalDateTime getDatetime() {
        checkPrimitive(PrimitiveTypeId.DATETIME, PrimitiveType.Datetime.name());
        return ProtoValue.toDatetime(value);
    }

    @Override
    public Instant getTimestamp() {
        checkPrimitive(PrimitiveTypeId.TIMESTAMP, PrimitiveType.Timestamp.name());
        return ProtoValue.toTimestamp(value);
    }

    @Override
    public Duration getInterval() {
        checkPrimitive(PrimitiveTypeId.INTERVAL, PrimitiveType.Interval.name());
        return ProtoValue.toInterval(value);
    }

    @Override
    public LocalDate getDate32() {
        checkPrimitive(PrimitiveTypeId.DATE32, PrimitiveType.Date32.name());
        return ProtoValue.toDate32(value);
    }

    @Override
    public LocalDateTime getDatetime64() {
        checkPrimitive(PrimitiveTypeId.DATETIME64, PrimitiveType.Datetime64.name());
        return ProtoValue.toDatetime64(value);
    }

    @Override
    public Instant getTimestamp64() {
        checkPrimitive(PrimitiveTypeId.TIMESTAMP64, PrimitiveType.Timestamp64.name());
        return ProtoValue.toTimestamp64(value);
    }

    @Override
    public Duration getInterval64() {
        checkPrimitive(PrimitiveTypeId.INTERVAL64, PrimitiveType.Interval64.name());
        return ProtoValue.toInterval64(value);
    }

    @Override
    public ZonedDateTime getTzDate() {
        checkPrimitive(PrimitiveTypeId.TZ_DATE, PrimitiveType.TzDate.name());
        return ProtoValue.toTzDate(value);
    }

    @Override
    public ZonedDateTime getTzDatetime() {
        checkPrimitive(PrimitiveTypeId.TZ_DATETIME, PrimitiveType.TzDatetime.name());
        return ProtoValue.toTzDatetime(value);
    }

    @Override
    public ZonedDateTime getTzTimestamp() {
        checkPrimitive(PrimitiveTypeId.TZ_TIMESTAMP, PrimitiveType.TzTimestamp.name());
        return ProtoValue.toTzTimestamp(value);
    }

    @Override
    public byte[] getBytes() {
        checkPrimitive(PrimitiveTypeId.STRING, PrimitiveType.Bytes.name());
        return ProtoValue.toBytes(value);
    }

    @Override
    public String getBytesAsString(Charset charset) {
        checkPrimitive(PrimitiveTypeId.STRING, PrimitiveType.Bytes.name());
        return ProtoValue.toBytesAsString(value, charset);
    }

    @Override
    public UUID getUuid() {
        checkPrimitive(PrimitiveTypeId.UUID, PrimitiveType.Uuid.name());
        return ProtoValue.toUuid(value);
    }

    @Override
    public String getText() {
        checkPrimitive(PrimitiveTypeId.UTF8, PrimitiveType.Text.name());
        return ProtoValue.toText(value);
    }

    @Override
    public byte[] getYson() {
        checkPrimitive(PrimitiveTypeId.YSON, PrimitiveType.Yson.name());
        return ProtoValue.toYson(value);
    }

    @Override
    public String getJson() {
        checkPrimitive(PrimitiveTypeId.JSON, PrimitiveType.Json.name());
        return ProtoValue.toJson(value);
    }

    @Override
    public String getJsonDocument() {
        checkPrimitive(PrimitiveTypeId.JSON_DOCUMENT, PrimitiveType.JsonDocument.name());
        return ProtoValue.toJsonDocument(value);
    }

    @Override
    public DecimalValue getDecimal() {
        if (type.getTypeCase() != ValueProtos.Type.TypeCase.DECIMAL_TYPE) {
            throw new IllegalStateException("types mismatch, expected Decimal, but was " + getType());
        }
        return ProtoValue.toDecimal(type, value);
    }

    private void checkPrimitive(PrimitiveTypeId typeId, String expected) {
        if (primitiveTypeId != typeId) {
            throw new IllegalStateException("types mismatch, expected " + expected + ", but was " + getType());
        }
    }

    @Override
    public void toString(StringBuilder sb) {
        switch (type.getTypeCase()) {
            case TYPE_ID:
                switch (value.getValueCase()) {
                    case BOOL_VALUE:
                        sb.append(value.getBoolValue());
                        break;
                    case INT32_VALUE:
                        sb.append(value.getInt32Value());
                        break;
                    case UINT32_VALUE:
                        sb.append(value.getUint32Value());
                        break;
                    case INT64_VALUE:
                        sb.append(value.getInt64Value());
                        break;
                    case UINT64_VALUE:
                        sb.append(value.getUint64Value());
                        break;
                    case FLOAT_VALUE:
                        sb.append(value.getFloatValue());
                        break;
                    case DOUBLE_VALUE:
                        sb.append(value.getDoubleValue());
                        break;
                    case BYTES_VALUE: {
                        ByteString bytes = value.getBytesValue();
                        if (bytes.isValidUtf8()) {
                            sb.append('\"');
                            sb.append(bytes.toStringUtf8());
                            sb.append('\"');
                        } else {
                            Hex.toHex(bytes, sb);
                        }
                        break;
                    }
                    case TEXT_VALUE:
                        sb.append('\"');
                        sb.append(value.getTextValue());
                        sb.append('\"');
                        break;

                    default:
                        throw new IllegalStateException("unsupported value case: " + value.getValueCase());
                }
                break;

            case DECIMAL_TYPE:
                sb.append(getDecimal().toString());
                break;

            default:
                throw new IllegalStateException("unsupported type case: " + type.getTypeCase());
        }
    }
}
