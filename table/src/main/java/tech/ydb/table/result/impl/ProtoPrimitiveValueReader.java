package tech.ydb.table.result.impl;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.UUID;

import com.google.protobuf.ByteString;
import tech.ydb.ValueProtos;
import tech.ydb.ValueProtos.Type.PrimitiveTypeId;
import tech.ydb.table.result.ValueReader;
import tech.ydb.table.types.Type;
import tech.ydb.table.types.proto.ProtoType;
import tech.ydb.table.utils.Hex;
import tech.ydb.table.values.DecimalValue;
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
    protected ValueProtos.Type getType() {
        return type;
    }

    protected ValueProtos.Value getValue() {
        return value;
    }

    @Override
    protected void setValue(ValueProtos.Value value) {
        this.value = value;
    }

    @Override
    public boolean getBool() {
        checkPrimitive(PrimitiveTypeId.BOOL);
        return ProtoValue.toBool(value);
    }

    @Override
    public byte getInt8() {
        checkPrimitive(PrimitiveTypeId.INT8);
        return ProtoValue.toInt8(value);
    }

    @Override
    public int getUint8() {
        checkPrimitive(PrimitiveTypeId.UINT8);
        return ProtoValue.toUint8(value);
    }

    @Override
    public short getInt16() {
        checkPrimitive(PrimitiveTypeId.INT16);
        return ProtoValue.toInt16(value);
    }

    @Override
    public int getUint16() {
        checkPrimitive(PrimitiveTypeId.UINT16);
        return ProtoValue.toUint16(value);
    }

    @Override
    public int getInt32() {
        checkPrimitive(PrimitiveTypeId.INT32);
        return ProtoValue.toInt32(value);
    }

    @Override
    public long getUint32() {
        checkPrimitive(PrimitiveTypeId.UINT32);
        return ProtoValue.toUint32(value);
    }

    @Override
    public long getInt64() {
        checkPrimitive(PrimitiveTypeId.INT64);
        return ProtoValue.toInt64(value);
    }

    @Override
    public long getUint64() {
        checkPrimitive(PrimitiveTypeId.UINT64);
        return ProtoValue.toUint64(value);
    }

    @Override
    public float getFloat32() {
        checkPrimitive(PrimitiveTypeId.FLOAT);
        return ProtoValue.toFloat32(value);
    }

    @Override
    public double getFloat64() {
        checkPrimitive(PrimitiveTypeId.DOUBLE);
        return ProtoValue.toFloat64(value);
    }

    @Override
    public LocalDate getDate() {
        checkPrimitive(PrimitiveTypeId.DATE);
        return ProtoValue.toDate(value);
    }

    @Override
    public LocalDateTime getDatetime() {
        checkPrimitive(PrimitiveTypeId.DATETIME);
        return ProtoValue.toDatetime(value);
    }

    @Override
    public Instant getTimestamp() {
        checkPrimitive(PrimitiveTypeId.TIMESTAMP);
        return ProtoValue.toTimestamp(value);
    }

    @Override
    public Duration getInterval() {
        checkPrimitive(PrimitiveTypeId.INTERVAL);
        return ProtoValue.toInterval(value);
    }

    @Override
    public ZonedDateTime getTzDate() {
        checkPrimitive(PrimitiveTypeId.TZ_DATE);
        return ProtoValue.toTzDate(value);
    }

    @Override
    public ZonedDateTime getTzDatetime() {
        checkPrimitive(PrimitiveTypeId.TZ_DATETIME);
        return ProtoValue.toTzDatetime(value);
    }

    @Override
    public ZonedDateTime getTzTimestamp() {
        checkPrimitive(PrimitiveTypeId.TZ_TIMESTAMP);
        return ProtoValue.toTzTimestamp(value);
    }

    @Override
    public byte[] getString() {
        checkPrimitive(PrimitiveTypeId.STRING);
        return ProtoValue.toString(value);
    }

    @Override
    public UUID getUuid() {
        checkPrimitive(PrimitiveTypeId.UUID);
        return ProtoValue.toUuid(value);
    }

    @Override
    public String getUtf8() {
        checkPrimitive(PrimitiveTypeId.UTF8);
        return ProtoValue.toUtf8(value);
    }

    @Override
    public byte[] getYson() {
        checkPrimitive(PrimitiveTypeId.YSON);
        return ProtoValue.toYson(value);
    }

    @Override
    public String getJson() {
        checkPrimitive(PrimitiveTypeId.JSON);
        return ProtoValue.toJson(value);
    }

    @Override
    public DecimalValue getDecimal() {
        if (type.getTypeCase() != ValueProtos.Type.TypeCase.DECIMAL_TYPE) {
            throw new IllegalStateException(
                "types mismatch, expected Decimal" +
                    ", but was " + ProtoType.toString(getType()));
        }
        return ProtoValue.toDecimal(type, value);
    }

    private void checkPrimitive(PrimitiveTypeId expected) {
        if (primitiveTypeId != expected) {
            throw new IllegalStateException(
                "types mismatch, expected " + expected +
                ", but was " + ProtoType.toString(getType()));
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
                getDecimal().toString(sb);
                break;

            default:
                throw new IllegalStateException("unsupported type case: " + type.getTypeCase());
        }
    }

    /**
     * Too common case to be implemented separately.
     */
    static final class Optional extends ProtoPrimitiveValueReader {

        private final ValueProtos.Type optionalType;
        private boolean present = false;

        Optional(ValueProtos.Type type) {
            // unwrap one level of optional
            super(type.getOptionalType().getItem());
            this.optionalType = type;
        }

        @Override
        public Type getValueType() {
            return ProtoType.fromPb(optionalType);
        }

        @Override
        protected ValueProtos.Value getValue() {
            return super.getValue();
        }

        @Override
        public boolean isOptionalItemPresent() {
            return present;
        }

        @Override
        public ValueReader getOptionalItem() {
            return this;
        }

        @Override
        protected void setValue(ValueProtos.Value value) {
            if (value.getValueCase() == ValueProtos.Value.ValueCase.NULL_FLAG_VALUE) {
                present = false;
                value = ValueProtos.Value.getDefaultInstance(); // for cleanup
            } else {
                present = true;
            }
            super.setValue(value);
        }

        @Override
        public void toString(StringBuilder sb) {
            if (present) {
                sb.append("Some[");
                super.toString(sb);
                sb.append(']');
            } else {
                sb.append("Empty[]");
            }
        }
    }
}
