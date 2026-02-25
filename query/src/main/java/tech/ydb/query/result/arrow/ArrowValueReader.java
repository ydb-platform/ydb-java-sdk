package tech.ydb.query.result.arrow;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import com.google.protobuf.UnsafeByteOperations;
import io.grpc.ExperimentalApi;
import org.apache.arrow.vector.BigIntVector;
import org.apache.arrow.vector.FieldVector;
import org.apache.arrow.vector.FixedSizeBinaryVector;
import org.apache.arrow.vector.Float4Vector;
import org.apache.arrow.vector.Float8Vector;
import org.apache.arrow.vector.IntVector;
import org.apache.arrow.vector.SmallIntVector;
import org.apache.arrow.vector.TinyIntVector;
import org.apache.arrow.vector.UInt1Vector;
import org.apache.arrow.vector.UInt2Vector;
import org.apache.arrow.vector.UInt4Vector;
import org.apache.arrow.vector.UInt8Vector;
import org.apache.arrow.vector.VarBinaryVector;
import org.apache.arrow.vector.VarCharVector;

import tech.ydb.table.result.ValueReader;
import tech.ydb.table.utils.Hex;
import tech.ydb.table.utils.LittleEndian;
import tech.ydb.table.values.DecimalType;
import tech.ydb.table.values.DecimalValue;
import tech.ydb.table.values.PrimitiveType;
import tech.ydb.table.values.PrimitiveValue;
import tech.ydb.table.values.Type;
import tech.ydb.table.values.Value;

/**
 *
 * @author Aleksandr Gorshenin
 * @param <T> type of FieldVector
 */
@ExperimentalApi("ApacheArrow support is experimental and API may change without notice")
public abstract class ArrowValueReader<T extends FieldVector> implements ValueReader {
    protected final Type type;
    protected final boolean isNullable;
    protected final T vector;
    protected int rowIndex;

    protected ArrowValueReader(T vector, Type type, boolean isNullable) {
        this.vector = vector;
        this.type = type;
        this.isNullable = isNullable;
        this.rowIndex = 0;
    }

    protected RuntimeException error(String method) {
        return new IllegalStateException("cannot call " + method + ", actual type: " + getType());
    }

    protected abstract ArrowValueReader<T> toNotNull();
    protected abstract Value<?> getNotNullValue();
    protected abstract String getNotNullValueAsString();

    public void setRowIndex(int index) {
        this.rowIndex = index;
    }

    public String getName() {
        return vector.getName();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        toString(sb);
        return sb.toString();
    }

    @Override
    public Type getType() {
        return isNullable ? type.makeOptional() : type;
    }

    @Override
    public boolean isOptionalItemPresent() {
        if (!isNullable) {
            throw error("isOptionalItemPresent");
        }
        return !vector.isNull(rowIndex);
    }

    @Override
    public ValueReader getOptionalItem() {
        if (!isNullable) {
            throw error("getOptionalItem");
        }
        if (vector.isNull(rowIndex)) {
            return null;
        }
        ArrowValueReader<T> notNull = toNotNull();
        notNull.setRowIndex(rowIndex);
        return notNull;
    }

    @Override
    public Value<?> getValue() {
        if (!isNullable) {
            return getNotNullValue();
        }

        if (vector.isNull(rowIndex)) {
            return type.makeOptional().emptyValue();
        }

        return getNotNullValue().makeOptional();
    }

    @Override
    public void toString(StringBuilder sb) {
        if (!isNullable) {
            sb.append(getNotNullValueAsString());
        } else {
            if (vector.isNull(rowIndex)) {
                sb.append("Empty[]");
            } else {
                sb.append("Some[").append(getNotNullValueAsString()).append("]");
            }
        }
    }

    @Override
    public boolean getBool() {
        throw error("getBool");
    }

    @Override
    public byte getInt8() {
        throw error("getInt8");
    }

    @Override
    public int getUint8() {
        throw error("getUint8");
    }

    @Override
    public short getInt16() {
        throw error("getInt16");
    }

    @Override
    public int getUint16() {
        throw error("getUint16");
    }

    @Override
    public int getInt32() {
        throw error("getInt32");
    }

    @Override
    public long getUint32() {
        throw error("getUint32");
    }

    @Override
    public long getInt64() {
        throw error("getInt64");
    }

    @Override
    public long getUint64() {
        throw error("getUint64");
    }

    @Override
    public float getFloat() {
        throw error("getFloat");
    }

    @Override
    public double getDouble() {
        throw error("getDouble");
    }

    @Override
    public LocalDate getDate() {
        throw error("getDate");
    }

    @Override
    public LocalDateTime getDatetime() {
        throw error("getDatetime");
    }

    @Override
    public Instant getTimestamp() {
        throw error("getTimestamp");
    }

    @Override
    public Duration getInterval() {
        throw error("getInterval");
    }

    @Override
    public LocalDate getDate32() {
        throw error("getDate32");
    }

    @Override
    public LocalDateTime getDatetime64() {
        throw error("getDatetime64");
    }

    @Override
    public Instant getTimestamp64() {
        throw error("getTimestamp64");
    }

    @Override
    public Duration getInterval64() {
        throw error("getInterval64");
    }

    @Override
    public ZonedDateTime getTzDate() {
        throw error("getTzDate");
    }

    @Override
    public ZonedDateTime getTzDatetime() {
        throw error("getTzDatetime");
    }

    @Override
    public ZonedDateTime getTzTimestamp() {
        throw error("getTzTimestamp");
    }

    @Override
    public byte[] getBytes() {
        throw error("getBytes");
    }

    @Override
    public String getBytesAsString(Charset charset) {
        throw error("getBytesAsString");
    }

    @Override
    public UUID getUuid() {
        throw error("getUuid");
    }

    @Override
    public String getText() {
        throw error("getText");
    }

    @Override
    public byte[] getYson() {
        throw error("getYson");
    }

    @Override
    public String getJson() {
        throw error("getJson");
    }

    @Override
    public String getJsonDocument() {
        throw error("getJsonDocument");
    }

    @Override
    public DecimalValue getDecimal() {
        throw error("getDecimal");
    }

    @Override
    public int getDictItemsCount() {
        throw error("getDictItemsCount");
    }

    @Override
    public ValueReader getDictKey(int index) {
        throw error("getDictKey");
    }

    @Override
    public ValueReader getDictValue(int index) {
        throw error("getDictValue");
    }

    @Override
    public int getListItemsCount() {
        throw error("getListItemsCount");
    }

    @Override
    public ValueReader getListItem(int index) {
        throw error("getListItem");
    }

    @Override
    public int getStructMembersCount() {
        throw error("getStructMembersCount");
    }

    @Override
    public String getStructMemberName(int index) {
        throw error("getStructMemberName");
    }

    @Override
    public ValueReader getStructMember(int index) {
        throw error("getStructMember");
    }

    @Override
    public ValueReader getStructMember(String name) {
        throw error("getStructMember");
    }

    @Override
    public int getTupleElementsCount() {
        throw error("getTupleElementsCount");
    }

    @Override
    public ValueReader getTupleElement(int index) {
        throw error("getTupleElement");
    }

    @Override
    public int getVariantTypeIndex() {
        throw error("getVariantTypeIndex");
    }

    @Override
    public ValueReader getVariantItem() {
        throw error("getVariantItem");
    }

    private static class UInt1VectorReader extends ArrowValueReader<UInt1Vector> {
        UInt1VectorReader(UInt1Vector vector, Type type, boolean isNullable) {
            super(vector, type, isNullable);
        }

        @Override
        public boolean getBool() {
            if (type != PrimitiveType.Bool) {
                throw error("getBool");
            }
            return vector.get(rowIndex) != 0;
        }

        @Override
        public int getUint8() {
            if (type != PrimitiveType.Uint8) {
                throw error("getUint8");
            }
            return vector.get(rowIndex) & 0xFF;
        }

        @Override
        protected Value<?> getNotNullValue() {
            if (type == PrimitiveType.Bool) {
                return PrimitiveValue.newBool(vector.get(rowIndex) != 0);
            }
            if (type == PrimitiveType.Uint8) {
                return PrimitiveValue.newUint8(vector.get(rowIndex));
            }
            throw error("getValue");
        }

        @Override
        protected UInt1VectorReader toNotNull() {
            return new UInt1VectorReader(vector, type, false);
        }

        @Override
        public String getNotNullValueAsString() {
            if (type == PrimitiveType.Bool) {
                return String.valueOf(getBool());
            }
            if (type == PrimitiveType.Uint8) {
                return String.valueOf(getUint8());
            }

            return "Unreadable UInt1Vector[" + type + "]";
        }
    }

    private static class UInt2VectorReader extends ArrowValueReader<UInt2Vector> {
        UInt2VectorReader(UInt2Vector vector, Type type, boolean isNullable) {
            super(vector, type, isNullable);
        }

        @Override
        public int getUint16() {
            if (type != PrimitiveType.Uint16) {
                throw error("getUint16");
            }
            return vector.get(rowIndex) & 0xFFFF;
        }

        @Override
        public LocalDate getDate() {
            if (type != PrimitiveType.Date) {
                throw error("getDate");
            }
            return LocalDate.ofEpochDay(vector.get(rowIndex));
        }

        @Override
        protected Value<?> getNotNullValue() {
            if (type == PrimitiveType.Date) {
                return PrimitiveValue.newDate(vector.get(rowIndex));
            }
            if (type == PrimitiveType.Uint16) {
                return PrimitiveValue.newUint16(vector.get(rowIndex));
            }
            throw error("getValue");
        }

        @Override
        protected UInt2VectorReader toNotNull() {
            return new UInt2VectorReader(vector, type, false);
        }

        @Override
        public String getNotNullValueAsString() {
            if (type == PrimitiveType.Date) {
                return getDate().toString();
            }
            if (type == PrimitiveType.Uint16) {
                return String.valueOf(getUint16());
            }

            return "Unreadable UInt2Vector[" + type + "]";
        }
    }

    private static class UInt4VectorReader extends ArrowValueReader<UInt4Vector> {
        UInt4VectorReader(UInt4Vector vector, Type type, boolean isNullable) {
            super(vector, type, isNullable);
        }

        @Override
        public long getUint32() {
            if (type != PrimitiveType.Uint32) {
                throw error("getUint32");
            }
            return vector.get(rowIndex) & 0xFFFFFFFFL;
        }

        @Override
        public LocalDateTime getDatetime() {
            if (type != PrimitiveType.Datetime) {
                throw error("getDatetime");
            }
            return LocalDateTime.ofEpochSecond(vector.get(rowIndex), 0, ZoneOffset.UTC);
        }

        @Override
        protected Value<?> getNotNullValue() {
            if (type == PrimitiveType.Datetime) {
                return PrimitiveValue.newDatetime(vector.get(rowIndex));
            }
            if (type == PrimitiveType.Uint32) {
                return PrimitiveValue.newUint32(vector.get(rowIndex));
            }
            throw error("getValue");
        }

        @Override
        protected UInt4VectorReader toNotNull() {
            return new UInt4VectorReader(vector, type, false);
        }

        @Override
        public String getNotNullValueAsString() {
            if (type == PrimitiveType.Datetime) {
                return getDatetime().toString();
            }
            if (type == PrimitiveType.Uint32) {
                return String.valueOf(getUint32());
            }
            return "Unreadable UInt4Vector[" + type + "]";
        }
    }

    private static class UInt8VectorReader extends ArrowValueReader<UInt8Vector> {
        UInt8VectorReader(UInt8Vector vector, Type type, boolean isNullable) {
            super(vector, type, isNullable);
        }

        @Override
        public long getUint64() {
            if (type != PrimitiveType.Uint64) {
                throw error("getUint64");
            }
            return vector.get(rowIndex);
        }

        @Override
        public Instant getTimestamp() {
            if (type != PrimitiveType.Timestamp) {
                throw error("getTimestamp");
            }
            long microsSinceEpoch = vector.get(rowIndex);
            long seconds = TimeUnit.MICROSECONDS.toSeconds(microsSinceEpoch);
            long micros = microsSinceEpoch - TimeUnit.SECONDS.toMicros(seconds);
            return Instant.ofEpochSecond(seconds, TimeUnit.MICROSECONDS.toNanos(micros));
        }

        @Override
        protected Value<?> getNotNullValue() {
            if (type == PrimitiveType.Uint64) {
                return PrimitiveValue.newUint64(vector.get(rowIndex));
            }
            if (type == PrimitiveType.Timestamp) {
                return PrimitiveValue.newTimestamp(vector.get(rowIndex));
            }
            throw error("getValue");
        }

        @Override
        protected UInt8VectorReader toNotNull() {
            return new UInt8VectorReader(vector, type, false);
        }

        @Override
        public String getNotNullValueAsString() {
            if (type == PrimitiveType.Uint64) {
                return Long.toUnsignedString(getUint64());
            }
            if (type == PrimitiveType.Timestamp) {
                return getTimestamp().toString();
            }
            return "Unreadable UInt8Vector[" + type + "]";
        }
    }

    private static class TinyIntVectorReader extends ArrowValueReader<TinyIntVector> {
        TinyIntVectorReader(TinyIntVector vector, Type type, boolean isNullable) {
            super(vector, type, isNullable);
        }

        @Override
        public byte getInt8() {
            if (type != PrimitiveType.Int8) {
                throw error("getInt8");
            }
            return vector.get(rowIndex);
        }

        @Override
        protected Value<?> getNotNullValue() {
            if (type == PrimitiveType.Int8) {
                return PrimitiveValue.newInt8(vector.get(rowIndex));
            }
            throw error("getValue");
        }

        @Override
        protected TinyIntVectorReader toNotNull() {
            return new TinyIntVectorReader(vector, type, false);
        }

        @Override
        public String getNotNullValueAsString() {
            if (type == PrimitiveType.Int8) {
                return String.valueOf(getInt8());
            }
            return "Unreadable TinyIntVector[" + type + "]";
        }
    }

    private static class SmallIntVectorReader extends ArrowValueReader<SmallIntVector> {
        SmallIntVectorReader(SmallIntVector vector, Type type, boolean isNullable) {
            super(vector, type, isNullable);
        }

        @Override
        public short getInt16() {
            if (type != PrimitiveType.Int16) {
                throw error("getInt16");
            }
            return vector.get(rowIndex);
        }

        @Override
        protected Value<?> getNotNullValue() {
            if (type == PrimitiveType.Int16) {
                return PrimitiveValue.newInt16(vector.get(rowIndex));
            }
            throw error("getValue");
        }

        @Override
        protected SmallIntVectorReader toNotNull() {
            return new SmallIntVectorReader(vector, type, false);
        }

        @Override
        public String getNotNullValueAsString() {
            if (type == PrimitiveType.Int16) {
                return String.valueOf(getInt16());
            }
            return "Unreadable SmallIntVector[" + type + "]";
        }
    }

    private static class IntVectorReader extends ArrowValueReader<IntVector> {
        IntVectorReader(IntVector vector, Type type, boolean isNullable) {
            super(vector, type, isNullable);
        }

        @Override
        public int getInt32() {
            if (type != PrimitiveType.Int32) {
                throw error("getInt32");
            }
            return vector.get(rowIndex);
        }

        @Override
        public LocalDate getDate32() {
            if (type != PrimitiveType.Date32) {
                throw error("getDate32");
            }
            return LocalDate.ofEpochDay(vector.get(rowIndex));
        }

        @Override
        protected Value<?> getNotNullValue() {
            if (type == PrimitiveType.Int32) {
                return PrimitiveValue.newInt32(vector.get(rowIndex));
            }
            if (type == PrimitiveType.Date32) {
                return PrimitiveValue.newDate32(vector.get(rowIndex));
            }
            throw error("getValue");
        }

        @Override
        protected IntVectorReader toNotNull() {
            return new IntVectorReader(vector, type, false);
        }

        @Override
        public String getNotNullValueAsString() {
            if (type == PrimitiveType.Int32) {
                return String.valueOf(getInt32());
            }
            if (type == PrimitiveType.Date32) {
                return getDate32().toString();
            }
            return "Unreadable IntVector[" + type + "]";
        }
    }

    private static class BigIntVectorReader extends ArrowValueReader<BigIntVector> {
        BigIntVectorReader(BigIntVector vector, Type type, boolean isNullable) {
            super(vector, type, isNullable);
        }

        @Override
        public long getInt64() {
            if (type != PrimitiveType.Int64) {
                throw error("getInt64");
            }
            return vector.get(rowIndex);
        }

        @Override
        public LocalDateTime getDatetime64() {
            if (type != PrimitiveType.Datetime64) {
                throw error("getDatetime64");
            }
            return LocalDateTime.ofEpochSecond(vector.get(rowIndex), 0, ZoneOffset.UTC);
        }

        @Override
        public Instant getTimestamp64() {
            if (type != PrimitiveType.Timestamp64) {
                throw error("getTimestamp64");
            }
            long microsSinceEpoch = vector.get(rowIndex);
            long seconds = TimeUnit.MICROSECONDS.toSeconds(microsSinceEpoch);
            long micros = microsSinceEpoch - TimeUnit.SECONDS.toMicros(seconds);
            return Instant.ofEpochSecond(seconds, TimeUnit.MICROSECONDS.toNanos(micros));
        }

        @Override
        public Duration getInterval() {
            if (type != PrimitiveType.Interval) {
                throw error("getInterval");
            }
            return Duration.ofNanos(TimeUnit.MICROSECONDS.toNanos(vector.get(rowIndex)));
        }

        @Override
        public Duration getInterval64() {
            if (type != PrimitiveType.Interval64) {
                throw error("getInterval64");
            }
            return Duration.ofNanos(TimeUnit.MICROSECONDS.toNanos(vector.get(rowIndex)));
        }

        @Override
        protected Value<?> getNotNullValue() {
            if (type == PrimitiveType.Int64) {
                return PrimitiveValue.newInt64(vector.get(rowIndex));
            }
            if (type == PrimitiveType.Datetime64) {
                return PrimitiveValue.newDatetime64(vector.get(rowIndex));
            }
            if (type == PrimitiveType.Timestamp64) {
                return PrimitiveValue.newTimestamp64(vector.get(rowIndex));
            }
            if (type == PrimitiveType.Interval) {
                return PrimitiveValue.newInterval(vector.get(rowIndex));
            }
            if (type == PrimitiveType.Interval64) {
                return PrimitiveValue.newInterval64(vector.get(rowIndex));
            }
            throw error("getValue");
        }

        @Override
        protected BigIntVectorReader toNotNull() {
            return new BigIntVectorReader(vector, type, false);
        }

        @Override
        public String getNotNullValueAsString() {
            if (type == PrimitiveType.Int64) {
                return String.valueOf(vector.get(rowIndex));
            }
            if (type == PrimitiveType.Datetime64) {
                return getDatetime64().toString();
            }
            if (type == PrimitiveType.Timestamp64) {
                return getTimestamp64().toString();
            }
            if (type == PrimitiveType.Interval) {
                return getInterval().toString();
            }
            if (type == PrimitiveType.Interval64) {
                return getInterval64().toString();
            }
            return "Unreadable BigIntVector[" + type + "]";
        }
    }

    private static class FloatVectorReader extends ArrowValueReader<Float4Vector> {
        FloatVectorReader(Float4Vector vector, Type type, boolean isNullable) {
            super(vector, type, isNullable);
        }

        @Override
        public float getFloat() {
            if (type != PrimitiveType.Float) {
                throw error("getFloat");
            }
            return vector.get(rowIndex);
        }

        @Override
        protected Value<?> getNotNullValue() {
            if (type == PrimitiveType.Float) {
                return PrimitiveValue.newFloat(vector.get(rowIndex));
            }
            throw error("getValue");
        }

        @Override
        protected FloatVectorReader toNotNull() {
            return new FloatVectorReader(vector, type, false);
        }

        @Override
        public String getNotNullValueAsString() {
            if (type == PrimitiveType.Float) {
                return String.valueOf(getFloat());
            }
            return "Unreadable Float4Vector[" + type + "]";
        }
    }

    private static class DoubleVectorReader extends ArrowValueReader<Float8Vector> {
        DoubleVectorReader(Float8Vector vector, Type type, boolean isNullable) {
            super(vector, type, isNullable);
        }

        @Override
        public double getDouble() {
            if (type != PrimitiveType.Double) {
                throw error("getDouble");
            }
            return vector.get(rowIndex);
        }

        @Override
        protected Value<?> getNotNullValue() {
            if (type == PrimitiveType.Double) {
                return PrimitiveValue.newDouble(vector.get(rowIndex));
            }
            throw error("getValue");
        }

        @Override
        protected DoubleVectorReader toNotNull() {
            return new DoubleVectorReader(vector, type, false);
        }

        @Override
        public String getNotNullValueAsString() {
            if (type == PrimitiveType.Double) {
                return String.valueOf(getDouble());
            }
            return "Unreadable Float8Vector[" + type + "]";
        }
    }

    private static class VarCharVectorReader extends ArrowValueReader<VarCharVector> {
        VarCharVectorReader(VarCharVector vector, Type type, boolean isNullable) {
            super(vector, type, isNullable);
        }

        @Override
        public String getText() {
            if (type != PrimitiveType.Text) {
                throw error("getText");
            }
            return new String(vector.get(rowIndex), StandardCharsets.UTF_8);
        }

        @Override
        public String getJson() {
            if (type != PrimitiveType.Json) {
                throw error("getJson");
            }
            return new String(vector.get(rowIndex), StandardCharsets.UTF_8);
        }

        @Override
        public String getJsonDocument() {
            if (type != PrimitiveType.JsonDocument) {
                throw error("getJsonDocument");
            }
            return new String(vector.get(rowIndex), StandardCharsets.UTF_8);
        }

        @Override
        public Value<?> getNotNullValue() {
            if (type == PrimitiveType.Text) {
                return PrimitiveValue.newText(getText());
            }
            if (type == PrimitiveType.Json) {
                return PrimitiveValue.newJson(getJson());
            }
            if (type == PrimitiveType.JsonDocument) {
                return PrimitiveValue.newJsonDocument(getJsonDocument());
            }
            throw error("getValue");
        }

        @Override
        protected VarCharVectorReader toNotNull() {
            return new VarCharVectorReader(vector, type, false);
        }

        @Override
        public String getNotNullValueAsString() {
            if (type == PrimitiveType.Text) {
                return getText();
            }
            if (type == PrimitiveType.Json) {
                return getJson();
            }
            if (type == PrimitiveType.JsonDocument) {
                return getJsonDocument();
            }
            return "Unreadable VarCharVector[" + type + "]";
        }
    }

    private static class VarBinaryVectorReader extends ArrowValueReader<VarBinaryVector> {
        VarBinaryVectorReader(VarBinaryVector vector, Type type, boolean isNullable) {
            super(vector, type, isNullable);
        }

        @Override
        public byte[] getBytes() {
            if (type != PrimitiveType.Bytes) {
                throw error("getBytes");
            }
            return vector.get(rowIndex);
        }

        @Override
        public String getBytesAsString(Charset charset) {
            if (type != PrimitiveType.Bytes) {
                throw error("getBytesAsString");
            }
            return new String(vector.get(rowIndex), charset);
        }

        @Override
        public byte[] getYson() {
            if (type != PrimitiveType.Yson) {
                throw error("getYson");
            }
            return vector.get(rowIndex);
        }

        @Override
        public Value<?> getNotNullValue() {
            if (type == PrimitiveType.Bytes) {
                return PrimitiveValue.newBytesOwn(vector.get(rowIndex));
            }
            if (type == PrimitiveType.Yson) {
                return PrimitiveValue.newYsonOwn(vector.get(rowIndex));
            }
            throw error("getValue");
        }

        @Override
        protected VarBinaryVectorReader toNotNull() {
            return new VarBinaryVectorReader(vector, type, false);
        }

        @Override
        public String getNotNullValueAsString() {
            if (type == PrimitiveType.Bytes) {
                return Hex.toHex(UnsafeByteOperations.unsafeWrap(getBytes()));
            }
            if (type == PrimitiveType.Yson) {
                return Hex.toHex(UnsafeByteOperations.unsafeWrap(getYson()));
            }
            return "Unreadable VarBinaryVector[" + type + "]";
        }
    }

    private static class FixedSizeBinaryVectorReader extends ArrowValueReader<FixedSizeBinaryVector> {
        FixedSizeBinaryVectorReader(FixedSizeBinaryVector vector, Type type, boolean isNullable) {
            super(vector, type, isNullable);
        }

        @Override
        public UUID getUuid() {
            if (type != PrimitiveType.Uuid) {
                throw error("getUuid");
            }

            ByteBuffer buf = ByteBuffer.wrap(vector.get(rowIndex));
            long msb = LittleEndian.bswap(buf.getLong());
            long timeLow = (msb & 0xffffffffL) << 32;
            long timeMid = (msb & 0x0000ffff00000000L) >> 16;
            long timeHighAndVersion = (msb & 0xffff000000000000L) >>> 48;
            long lsb = buf.getLong();

            return new UUID(timeLow | timeMid | timeHighAndVersion, lsb);
        }

        @Override
        public DecimalValue getDecimal() {
            if (type.getKind() != Type.Kind.DECIMAL) {
                throw error("getDecimal");
            }

            ByteBuffer buf = ByteBuffer.wrap(vector.get(rowIndex));
            long low = LittleEndian.bswap(buf.getLong());
            long high = LittleEndian.bswap(buf.getLong());
            return ((DecimalType) type).newValue(high, low);
        }

        @Override
        public Value<?> getNotNullValue() {
            if (type.getKind() == Type.Kind.DECIMAL) {
                return getDecimal();
            }
            if (type == PrimitiveType.Uuid) {
                return PrimitiveValue.newUuid(getUuid());
            }
            throw error("getValue");
        }

        @Override
        protected FixedSizeBinaryVectorReader toNotNull() {
            return new FixedSizeBinaryVectorReader(vector, type, false);
        }

        @Override
        public String getNotNullValueAsString() {
            if (type.getKind() == Type.Kind.DECIMAL) {
                return getDecimal().toString();
            }
            if (type == PrimitiveType.Uuid) {
                return getUuid().toString();
            }
            return "Unreadable FixedSizeBinaryVector[" + type + "]";
        }
    }

    public static ArrowValueReader<?> createReader(FieldVector vector, Type type, boolean optional) {
        switch (vector.getClass().getSimpleName()) {
            case "UInt1Vector": return new UInt1VectorReader((UInt1Vector) vector, type, optional);
            case "UInt2Vector": return new UInt2VectorReader((UInt2Vector) vector, type, optional);
            case "UInt4Vector": return new UInt4VectorReader((UInt4Vector) vector, type, optional);
            case "UInt8Vector": return new UInt8VectorReader((UInt8Vector) vector, type, optional);

            case "TinyIntVector": return new TinyIntVectorReader((TinyIntVector) vector, type, optional);
            case "SmallIntVector": return new SmallIntVectorReader((SmallIntVector) vector, type, optional);
            case "IntVector": return new IntVectorReader((IntVector) vector, type, optional);
            case "BigIntVector": return new BigIntVectorReader((BigIntVector) vector, type, optional);

            case "Float4Vector": return new FloatVectorReader((Float4Vector) vector, type, optional);
            case "Float8Vector": return new DoubleVectorReader((Float8Vector) vector, type, optional);

            case "VarCharVector": return new VarCharVectorReader((VarCharVector) vector, type, optional);
            case "VarBinaryVector": return new VarBinaryVectorReader((VarBinaryVector) vector, type, optional);
            case "FixedSizeBinaryVector":
                return new FixedSizeBinaryVectorReader((FixedSizeBinaryVector) vector, type, optional);
            default:
                throw new IllegalStateException("Unsupported ApacheArrow vector type: " + vector.getClass());
        }
    }
}
