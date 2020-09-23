package tech.ydb.table.values;

import java.nio.charset.Charset;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import com.google.common.escape.Escaper;
import com.google.common.escape.Escapers;
import com.google.protobuf.ByteString;
import com.google.protobuf.UnsafeByteOperations;
import tech.ydb.ValueProtos;
import tech.ydb.table.utils.LittleEndian;
import tech.ydb.table.values.proto.ProtoValue;


/**
 * @author Sergey Polovko
 */
public abstract class PrimitiveValue implements Value<PrimitiveType> {

    // -- unboxing --

    public boolean getBool() {
        throw new IllegalStateException("expected Bool, but was " + getClass().getSimpleName());
    }

    public byte getInt8() {
        throw new IllegalStateException("expected Int8, but was " + getClass().getSimpleName());
    }

    public short getUint8() {
        throw new IllegalStateException("expected Uint8, but was " + getClass().getSimpleName());
    }

    public short getInt16() {
        throw new IllegalStateException("expected Int16, but was " + getClass().getSimpleName());
    }

    public int getUint16() {
        throw new IllegalStateException("expected Uint16, but was " + getClass().getSimpleName());
    }

    public int getInt32() {
        throw new IllegalStateException("expected Int32, but was " + getClass().getSimpleName());
    }

    public long getUint32() {
        throw new IllegalStateException("expected Uint32, but was " + getClass().getSimpleName());
    }

    public long getInt64() {
        throw new IllegalStateException("expected Int64, but was " + getClass().getSimpleName());
    }

    public long getUint64() {
        throw new IllegalStateException("expected Uint64, but was " + getClass().getSimpleName());
    }

    public float getFloat32() {
        throw new IllegalStateException("expected Float32, but was " + getClass().getSimpleName());
    }

    public double getFloat64() {
        throw new IllegalStateException("expected Float64, but was " + getClass().getSimpleName());
    }

    public byte[] getString() {
        throw new IllegalStateException("expected String, but was " + getClass().getSimpleName());
    }

    public byte[] getStringUnsafe() {
        throw new IllegalStateException("expected String, but was " + getClass().getSimpleName());
    }

    public ByteString getStringBytes() {
        throw new IllegalStateException("expected String, but was " + getClass().getSimpleName());
    }

    public String getString(Charset charset) {
        return new String(getStringUnsafe(), charset);
    }

    public String getUtf8() {
        throw new IllegalStateException("expected Utf8, but was " + getClass().getSimpleName());
    }

    public byte[] getYson() {
        throw new IllegalStateException("expected Yson, but was " + getClass().getSimpleName());
    }

    public byte[] getYsonUnsafe() {
        throw new IllegalStateException("expected Yson, but was " + getClass().getSimpleName());
    }

    public ByteString getYsonBytes() {
        throw new IllegalStateException("expected Yson, but was " + getClass().getSimpleName());
    }

    public String getJson() {
        throw new IllegalStateException("expected Json, but was " + getClass().getSimpleName());
    }

    public String getJsonDocument() {
        throw new IllegalStateException("expected JsonDocument, but was " + getClass().getSimpleName());
    }

    public String getUuidString() {
        throw new IllegalStateException("expected Uuid, but was " + getClass().getSimpleName());
    }

    public long getUuidHigh() {
        throw new IllegalStateException("expected Uuid, but was " + getClass().getSimpleName());
    }

    public long getUuidLow() {
        throw new IllegalStateException("expected Uuid, but was " + getClass().getSimpleName());
    }

    public UUID getUuidJdk() {
        throw new IllegalStateException("expected Uuid, but was " + getClass().getSimpleName());
    }

    public LocalDate getDate() {
        throw new IllegalStateException("expected Date, but was " + getClass().getSimpleName());
    }

    public LocalDateTime getDatetime() {
        throw new IllegalStateException("expected Datetime, but was " + getClass().getSimpleName());
    }

    public Instant getTimestamp() {
        throw new IllegalStateException("expected Timestamp, but was " + getClass().getSimpleName());
    }

    public Duration getInterval() {
        throw new IllegalStateException("expected Interval, but was " + getClass().getSimpleName());
    }

    public ZonedDateTime getTzDate() {
        throw new IllegalStateException("expected TzDate, but was " + getClass().getSimpleName());
    }

    public ZonedDateTime getTzDatetime() {
        throw new IllegalStateException("expected TzDatetime, but was " + getClass().getSimpleName());
    }

    public ZonedDateTime getTzTimestamp() {
        throw new IllegalStateException("expected TzTimestamp, but was " + getClass().getSimpleName());
    }

    // -- constructors --

    public static PrimitiveValue bool(boolean value) { return value ? Bool.TRUE : Bool.FALSE; }
    public static PrimitiveValue int8(byte value) { return new Int8(value); }
    public static PrimitiveValue uint8(byte value) { return new Uint8(value); }
    public static PrimitiveValue int16(short value) { return new Int16(value); }
    public static PrimitiveValue uint16(short value) { return new Uint16(value); }
    public static PrimitiveValue int32(int value) { return new Int32(value); }
    public static PrimitiveValue uint32(int value) { return new Uint32(value); }
    public static PrimitiveValue int64(long value) { return new Int64(value); }
    public static PrimitiveValue uint64(long value) { return new Uint64(value); }
    public static PrimitiveValue float32(float value) { return new Float32(value); }
    public static PrimitiveValue float64(double value) { return new Float64(value); }
    public static PrimitiveValue string(byte[] value) {
        return value.length == 0 ? Bytes.EMPTY_STRING : new Bytes(PrimitiveType.string(), value.clone());
    }
    public static PrimitiveValue string(ByteString value) {
        return value.isEmpty() ? Bytes.EMPTY_STRING : new Bytes(PrimitiveType.string(), value);
    }
    public static PrimitiveValue stringOwn(byte[] value) {
        return value.length == 0 ? Bytes.EMPTY_STRING : new Bytes(PrimitiveType.string(), value);
    }
    public static PrimitiveValue utf8(String value) {
        return value.isEmpty() ? Text.EMPTY_UTF8 : new Text(PrimitiveType.utf8(), value);
    }
    public static PrimitiveValue yson(byte[] value) {
        return value.length == 0 ? Bytes.EMPTY_YSON : new Bytes(PrimitiveType.yson(), value.clone());
    }
    public static PrimitiveValue yson(ByteString value) {
        return value.isEmpty() ? Bytes.EMPTY_YSON : new Bytes(PrimitiveType.yson(), value);
    }
    public static PrimitiveValue ysonOwn(byte[] value) {
        return value.length == 0 ? Bytes.EMPTY_YSON : new Bytes(PrimitiveType.yson(), value);
    }
    public static PrimitiveValue json(String value) {
        return value.isEmpty() ? Text.EMPTY_JSON : new Text(PrimitiveType.json(), value);
    }
    public static PrimitiveValue jsonDocument(String value) {
        return value.isEmpty() ? Text.EMPTY_JSON_DOCUMENT : new Text(PrimitiveType.jsonDocument(), value);
    }
    public static PrimitiveValue uuid(long high, long low) { return new Uuid(high, low); }
    public static PrimitiveValue uuid(UUID uuid) { return new Uuid(uuid); }
    public static PrimitiveValue uuid(String uuid) { return new Uuid(uuid); }

    public static PrimitiveValue date(long daysSinceEpoch) {
        if (daysSinceEpoch < 0) {
            throw new IllegalArgumentException("negative daysSinceEpoch: " + daysSinceEpoch);
        }
        return new InstantValue(PrimitiveType.date(), TimeUnit.DAYS.toMicros(daysSinceEpoch));
    }

    public static PrimitiveValue date(LocalDate value) {
        return date(value.toEpochDay());
    }

    public static PrimitiveValue date(Instant value) {
        return date(TimeUnit.SECONDS.toDays(value.getEpochSecond()));
    }

    public static PrimitiveValue datetime(long secondsSinceEpoch) {
        if (secondsSinceEpoch < 0) {
            throw new IllegalArgumentException("negative secondsSinceEpoch: " + secondsSinceEpoch);
        }
        return new InstantValue(PrimitiveType.datetime(), TimeUnit.SECONDS.toMicros(secondsSinceEpoch));
    }

    public static PrimitiveValue datetime(Instant value) {
        return datetime(value.getEpochSecond());
    }

    public static PrimitiveValue timestamp(long microsSinceEpoch) {
        if (microsSinceEpoch < 0) {
            throw new IllegalArgumentException("negative microsSinceEpoch: " + microsSinceEpoch);
        }
        return new InstantValue(PrimitiveType.timestamp(), microsSinceEpoch);
    }

    public static PrimitiveValue timestamp(Instant value) {
        long micros = TimeUnit.SECONDS.toMicros(value.getEpochSecond()) +
            TimeUnit.NANOSECONDS.toMicros(value.getNano());
        return new InstantValue(PrimitiveType.timestamp(), micros);
    }

    public static PrimitiveValue interval(long micros) { return new IntervalValue(micros); }
    public static PrimitiveValue interval(Duration value) { return interval(TimeUnit.NANOSECONDS.toMicros(value.toNanos())); }

    public static PrimitiveValue tzDate(ZonedDateTime dateTime) {
        return new TzDatetime(PrimitiveType.tzDate(), dateTime);
    }

    public static PrimitiveValue tzDatetime(ZonedDateTime dateTime) {
        return new TzDatetime(PrimitiveType.tzDatetime(), dateTime);
    }

    public static PrimitiveValue tzTimestamp(ZonedDateTime dateTime) {
        return new TzDatetime(PrimitiveType.tzTimestamp(), dateTime);
    }

    // -- helpers --

    private static void checkType(PrimitiveType expected, PrimitiveType.Id actual) {
        checkType(expected.getId(), actual);
    }

    private static void checkType(PrimitiveType.Id expected, PrimitiveType.Id actual) {
        if (expected != actual) {
            throw new IllegalStateException("types mismatch, expected " + expected + ", but was " + actual);
        }
    }

    // -- implementations --

    private static final class Bool extends PrimitiveValue {
        private static final Bool TRUE = new Bool(true);
        private static final Bool FALSE = new Bool(false);

        private final boolean value;

        private Bool(boolean value) {
            this.value = value;
        }

        @Override
        public PrimitiveType getType() {
            return PrimitiveType.bool();
        }

        @Override
        public boolean getBool() {
            return value;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            return value == ((Bool) o).value;
        }

        @Override
        public int hashCode() {
            return Boolean.hashCode(value);
        }

        @Override
        public String toString() {
            return Boolean.toString(value);
        }

        @Override
        public ValueProtos.Value toPb() {
            return ProtoValue.bool(value);
        }
    }

    private static final class Int8 extends PrimitiveValue {
        private final byte value;

        Int8(byte value) {
            this.value = value;
        }

        @Override
        public PrimitiveType getType() {
            return PrimitiveType.int8();
        }

        @Override
        public byte getInt8() {
            return value;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            return value == ((Int8) o).value;
        }

        @Override
        public int hashCode() {
            return Byte.hashCode(value);
        }

        @Override
        public String toString() {
            return Byte.toString(value);
        }

        @Override
        public ValueProtos.Value toPb() {
            return ProtoValue.int8(value);
        }
    }

    private static final class Uint8 extends PrimitiveValue {
        private final byte value;

        Uint8(byte value) {
            this.value = value;
        }

        @Override
        public PrimitiveType getType() {
            return PrimitiveType.uint8();
        }

        @Override
        public short getUint8() {
            return (short) Byte.toUnsignedInt(value);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            return value == ((Uint8) o).value;
        }

        @Override
        public int hashCode() {
            return Byte.hashCode(value);
        }

        @Override
        public String toString() {
            return Integer.toString(Byte.toUnsignedInt(value));
        }

        @Override
        public ValueProtos.Value toPb() {
            return ProtoValue.uint8(value);
        }
    }

    private static final class Int16 extends PrimitiveValue {
        private final short value;

        Int16(short value) {
            this.value = value;
        }

        @Override
        public PrimitiveType getType() {
            return PrimitiveType.int16();
        }

        @Override
        public short getInt16() {
            return value;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            return value == ((Int16) o).value;
        }

        @Override
        public int hashCode() {
            return Short.hashCode(value);
        }

        @Override
        public String toString() {
            return Short.toString(value);
        }

        @Override
        public ValueProtos.Value toPb() {
            return ProtoValue.int16(value);
        }
    }

    private static final class Uint16 extends PrimitiveValue {
        private final short value;

        Uint16(short value) {
            this.value = value;
        }

        @Override
        public PrimitiveType getType() {
            return PrimitiveType.uint16();
        }

        @Override
        public int getUint16() {
            return Short.toUnsignedInt(value);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            return value == ((Uint16) o).value;
        }

        @Override
        public int hashCode() {
            return Short.hashCode(value);
        }

        @Override
        public String toString() {
            return Integer.toString(Short.toUnsignedInt(value));
        }

        @Override
        public ValueProtos.Value toPb() {
            return ProtoValue.uint16(value);
        }
    }

    private static final class Int32 extends PrimitiveValue {
        private final int value;

        Int32(int value) {
            this.value = value;
        }

        @Override
        public PrimitiveType getType() {
            return PrimitiveType.int32();
        }

        @Override
        public int getInt32() {
            return value;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            return value == ((Int32) o).value;
        }

        @Override
        public int hashCode() {
            return Integer.hashCode(value);
        }

        @Override
        public String toString() {
            return Integer.toString(value);
        }

        @Override
        public ValueProtos.Value toPb() {
            return ProtoValue.int32(value);
        }
    }

    private static final class Uint32 extends PrimitiveValue {
        private final int value;

        Uint32(int value) {
            this.value = value;
        }

        @Override
        public PrimitiveType getType() {
            return PrimitiveType.uint32();
        }

        @Override
        public long getUint32() {
            return Integer.toUnsignedLong(value);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            return value == ((Uint32) o).value;
        }

        @Override
        public int hashCode() {
            return Integer.hashCode(value);
        }

        @Override
        public String toString() {
            return Long.toString(Integer.toUnsignedLong(value));
        }

        @Override
        public ValueProtos.Value toPb() {
            return ProtoValue.uint32(value);
        }
    }

    private static final class Int64 extends PrimitiveValue {
        private final long value;

        Int64(long value) {
            this.value = value;
        }

        @Override
        public PrimitiveType getType() {
            return PrimitiveType.int64();
        }

        @Override
        public long getInt64() {
            return value;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            return value == ((Int64) o).value;
        }

        @Override
        public int hashCode() {
            return Long.hashCode(value);
        }

        @Override
        public String toString() {
            return Long.toString(value);
        }

        @Override
        public ValueProtos.Value toPb() {
            return ProtoValue.int64(value);
        }
    }

    private static final class Uint64 extends PrimitiveValue {
        private final long value;

        Uint64(long value) {
            this.value = value;
        }

        @Override
        public PrimitiveType getType() {
            return PrimitiveType.uint64();
        }

        @Override
        public long getUint64() {
            return value;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            return value == ((Uint64) o).value;
        }

        @Override
        public int hashCode() {
            return Long.hashCode(value);
        }

        @Override
        public String toString() {
            return Long.toUnsignedString(value);
        }

        @Override
        public ValueProtos.Value toPb() {
            return ProtoValue.uint64(value);
        }
    }

    private static final class Float32 extends PrimitiveValue {
        private final float value;

        Float32(float value) {
            this.value = value;
        }

        @Override
        public PrimitiveType getType() {
            return PrimitiveType.float32();
        }

        @Override
        public float getFloat32() {
            return value;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Float32 that = (Float32) o;
            return Float.compare(that.value, value) == 0;
        }

        @Override
        public int hashCode() {
            return Float.hashCode(value);
        }

        @Override
        public String toString() {
            return Float.toString(value);
        }

        @Override
        public ValueProtos.Value toPb() {
            return ProtoValue.float32(value);
        }
    }

    private static final class Float64 extends PrimitiveValue {
        private final double value;

        Float64(double value) {
            this.value = value;
        }

        @Override
        public PrimitiveType getType() {
            return PrimitiveType.float64();
        }

        @Override
        public double getFloat64() {
            return value;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Float64 that = (Float64) o;
            return Double.compare(that.value, value) == 0;
        }

        @Override
        public int hashCode() {
            return Double.hashCode(value);
        }

        @Override
        public String toString() {
            return Double.toString(value);
        }

        @Override
        public ValueProtos.Value toPb() {
            return ProtoValue.float64(value);
        }
    }

    private static final class Bytes extends PrimitiveValue {
        private static final Bytes EMPTY_STRING = new Bytes(PrimitiveType.string(), new byte[0]);
        private static final Bytes EMPTY_YSON = new Bytes(PrimitiveType.yson(), new byte[0]);

        private final PrimitiveType type;
        private final Object value;

        private Bytes(PrimitiveType type, byte[] value) {
            this.type = type;
            this.value = value;
        }

        private Bytes(PrimitiveType type, ByteString value) {
            this.type = type;
            this.value = value;
        }

        @Override
        public PrimitiveType getType() {
            return type;
        }

        @Override
        public byte[] getString() {
            return getBytes(PrimitiveType.string());
        }

        @Override
        public byte[] getStringUnsafe() {
            return getBytesUnsafe(PrimitiveType.string());
        }

        @Override
        public ByteString getStringBytes() {
            return getByteString(PrimitiveType.string());
        }

        @Override
        public byte[] getYson() {
            return getBytes(PrimitiveType.yson());
        }

        @Override
        public byte[] getYsonUnsafe() {
            return getBytesUnsafe(PrimitiveType.yson());
        }

        @Override
        public ByteString getYsonBytes() {
            return getByteString(PrimitiveType.yson());
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Bytes that = (Bytes) o;
            if (type.getId() != that.type.getId()) {
                return false;
            }

            if (value instanceof byte[]) {
                if (that.value instanceof byte[]) {
                    return Arrays.equals((byte[]) value, (byte[]) that.value);
                }
                return that.value.equals(UnsafeByteOperations.unsafeWrap((byte[]) value));
            }

            if (that.value instanceof byte[]) {
                return value.equals(UnsafeByteOperations.unsafeWrap((byte[]) that.value));
            }
            return value.equals(that.value);
        }

        @Override
        public int hashCode() {
            int result = type.hashCode();

            if (value instanceof byte[]) {
                for (byte b : (byte[]) value) {
                    result = 31 * result + b;
                }
            } else {
                ByteString value = (ByteString) this.value;
                for (int i = 0; i < value.size(); i++) {
                    byte b = value.byteAt(i);
                    result = 31 * result + b;
                }
            }

            return result;
        }

        @Override
        public String toString() {
            final int length = (value instanceof byte[])
                ? ((byte[]) value).length
                : ((ByteString) value).size();

            if (length == 0) {
                return "\"\"";
            }

            // bytes are escaped as \nnn (octal value)
            StringBuilder sb = new StringBuilder(length * 4 + 2);
            sb.append('\"');

            if (value instanceof byte[]) {
                for (byte b : (byte[]) value) {
                    encodeAsOctal(sb, b);
                }
            } else {
                ByteString value = (ByteString) this.value;
                for (int i = 0; i < value.size(); i++) {
                    encodeAsOctal(sb, value.byteAt(i));
                }
            }
            sb.append('\"');
            return sb.toString();
        }

        private static void encodeAsOctal(StringBuilder sb, byte b) {
            final int i = Byte.toUnsignedInt(b);
            sb.append('\\');
            if (i < 64) {
                sb.append('0');
                if (i < 8) {
                    sb.append('0');
                }
            }
            sb.append(Integer.toString(i, 8));
        }

        @Override
        public ValueProtos.Value toPb() {
            return ProtoValue.bytes(getByteString(type));
        }

        private byte[] getBytes(PrimitiveType expected) {
            checkType(expected, type.getId());

            if (value instanceof byte[]) {
                return ((byte[]) value).clone();
            }
            return ((ByteString) value).toByteArray();
        }

        private byte[] getBytesUnsafe(PrimitiveType expected) {
            checkType(expected, type.getId());

            if (value instanceof byte[]) {
                return (byte[]) value;
            }
            return ((ByteString) value).toByteArray();
        }

        private ByteString getByteString(PrimitiveType expected) {
            checkType(expected, type.getId());

            if (value instanceof byte[]) {
                return UnsafeByteOperations.unsafeWrap((byte[]) value);
            }
            return (ByteString) value;
        }
    }

    private static final class Text extends PrimitiveValue {
        private static final Text EMPTY_UTF8 = new Text(PrimitiveType.utf8(), "");
        private static final Text EMPTY_JSON = new Text(PrimitiveType.json(), "");
        private static final Text EMPTY_JSON_DOCUMENT = new Text(PrimitiveType.jsonDocument(), "");

        private static final Escaper ESCAPER = Escapers.builder()
            .addEscape('\\', "\\\\")
            .addEscape('\"', "\\\"")
            .build();

        private final PrimitiveType type;
        private final String value;

        Text(PrimitiveType type, String value) {
            this.type = type;
            this.value = value;
        }

        @Override
        public PrimitiveType getType() {
            return type;
        }

        @Override
        public String getUtf8() {
            checkType(PrimitiveType.utf8(), type.getId());
            return value;
        }

        @Override
        public String getJson() {
            checkType(PrimitiveType.json(), type.getId());
            return value;
        }

        @Override
        public String getJsonDocument() {
            checkType(PrimitiveType.jsonDocument(), type.getId());
            return value;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Text that = (Text) o;
            if (type.getId() != that.type.getId()) {
                return false;
            }
            return that.value.equals(value);
        }

        @Override
        public int hashCode() {
            return 31 * type.hashCode() + value.hashCode();
        }

        @Override
        public String toString() {
            if (value.isEmpty()) {
                return "\"\"";
            }

            return '\"' + ESCAPER.escape(value) + '\"';
        }

        @Override
        public ValueProtos.Value toPb() {
            return ProtoValue.text(value);
        }
    }

    private static final class Uuid extends PrimitiveValue {
        private final long high;
        private final long low;

        Uuid(long high, long low) {
            this.high = high;
            this.low = low;
        }

        @Override
        public PrimitiveType getType() {
            return PrimitiveType.uuid();
        }

        Uuid(String value) {
            String[] components = value.split("-");
            if (components.length != 5) {
                throw new IllegalArgumentException("invalid UUID string: " + value);
            }

            long timeLow = Long.parseLong(components[0], 16);
            long timeMid = Long.parseLong(components[1], 16) << 32;
            long timeHighAndVersion = Long.parseLong(components[2], 16) << 48;
            this.low = timeLow | timeMid | timeHighAndVersion;

            long lsb = Long.parseLong(components[3], 16) << 48;
            lsb |= Long.parseLong(components[4], 16);
            this.high = LittleEndian.bswap(lsb);
        }

        Uuid(UUID uuid) {
            long msb = uuid.getMostSignificantBits();
            long timeLow  = (msb & 0xffffffff00000000L) >>> 32;
            long timeMid = (msb & 0x00000000ffff0000L) << 16;
            long timeHighAndVersion = (msb & 0x000000000000ffffL) << 48;

            this.low = timeLow | timeMid | timeHighAndVersion;
            this.high = LittleEndian.bswap(uuid.getLeastSignificantBits());
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Uuid uuid = (Uuid) o;

            if (high != uuid.high) return false;
            return low == uuid.low;
        }

        @Override
        public int hashCode() {
            int result = (int) (high ^ (high >>> 32));
            result = 31 * result + (int) (low ^ (low >>> 32));
            return result;
        }

        @Override
        public String toString() {
            return '\"' + getUuidString() + '\"';
        }

        @Override
        public String getUuidString() {
            long hiBe = LittleEndian.bswap(high);
            return
                digits(low, 8) + "-" + digits(low >>> 32, 4) + "-" + digits(low >>> 48, 4) + "-" +
                digits(hiBe >> 48, 4) + "-" + digits(hiBe, 12);
        }

        @Override
        public long getUuidHigh() {
            return high;
        }

        @Override
        public long getUuidLow() {
            return low;
        }

        public UUID getUuidJdk() {
            long timeLow = (low & 0x00000000ffffffffL) << 32;
            long timeMid = (low & 0x0000ffff00000000L) >>> 16;
            long timeHighAndVersion = (low & 0xffff000000000000L) >>> 48;

            long hiBe = LittleEndian.bswap(high);
            return new UUID(timeLow | timeMid | timeHighAndVersion, hiBe);
        }

        @Override
        public ValueProtos.Value toPb() {
            return ProtoValue.uuid(high, low);
        }

        /** Returns val represented by the specified number of hex digits. */
        private static String digits(long val, int digits) {
            long high = 1L << (digits * 4);
            return Long.toHexString(high | (val & (high - 1))).substring(1);
        }
    }

    private static final class InstantValue extends PrimitiveValue {
        private final PrimitiveType type;
        private final long microsSinceEpoch;

        InstantValue(PrimitiveType type, long microsSinceEpoch) {
            this.type = type;
            this.microsSinceEpoch = microsSinceEpoch;
        }

        @Override
        public PrimitiveType getType() {
            return type;
        }

        @Override
        public LocalDate getDate() {
            checkType(PrimitiveType.Id.Date, type.getId());
            return ProtoValue.toDate(TimeUnit.MICROSECONDS.toDays(microsSinceEpoch));
        }

        @Override
        public LocalDateTime getDatetime() {
            checkType(PrimitiveType.Id.Datetime, type.getId());
            return ProtoValue.toDatetime(TimeUnit.MICROSECONDS.toSeconds(microsSinceEpoch));
        }

        @Override
        public Instant getTimestamp() {
            checkType(PrimitiveType.Id.Timestamp, type.getId());
            return ProtoValue.toTimestamp(microsSinceEpoch);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            InstantValue that = (InstantValue) o;
            if (microsSinceEpoch != that.microsSinceEpoch) return false;
            return type.getId() == that.type.getId();
        }

        @Override
        public int hashCode() {
            return 31 * type.hashCode() + (int) (microsSinceEpoch ^ (microsSinceEpoch >>> 32));
        }

        @Override
        public String toString() {
            switch (type.getId()) {
                case Date: return DateTimeFormatter.ISO_DATE.format(getDate());
                case Datetime: return DateTimeFormatter.ISO_DATE_TIME.format(getDatetime());
                case Timestamp: return DateTimeFormatter.ISO_INSTANT.format(getTimestamp());
                default:
                    throw new IllegalStateException("unsupported type: " + type);
            }
        }

        @Override
        public ValueProtos.Value toPb() {
            switch (type.getId()) {
                case Date: return ProtoValue.date(TimeUnit.MICROSECONDS.toDays(microsSinceEpoch));
                case Datetime: return ProtoValue.datetime(TimeUnit.MICROSECONDS.toSeconds(microsSinceEpoch));
                case Timestamp: return ProtoValue.timestamp(microsSinceEpoch);
                default:
                    throw new IllegalStateException("unsupported type: " + type);
            }
        }
    }

    private static final class IntervalValue extends PrimitiveValue {
        private final long micros;

        IntervalValue(long micros) {
            this.micros = micros;
        }

        @Override
        public PrimitiveType getType() {
            return PrimitiveType.interval();
        }

        @Override
        public Duration getInterval() {
            return Duration.of(micros, ChronoUnit.MICROS);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            IntervalValue that = (IntervalValue) o;
            return micros == that.micros;
        }

        @Override
        public int hashCode() {
            return (int) (micros ^ (micros >>> 32));
        }

        @Override
        public String toString() {
            return getInterval().toString();
        }

        @Override
        public ValueProtos.Value toPb() {
            return ProtoValue.interval(micros);
        }
    }

    private static final class TzDatetime extends PrimitiveValue {
        private final PrimitiveType type;
        private final ZonedDateTime dateTime;

        TzDatetime(PrimitiveType type, ZonedDateTime dateTime) {
            this.type = type;
            this.dateTime = dateTime;
        }

        @Override
        public PrimitiveType getType() {
            return type;
        }

        @Override
        public ZonedDateTime getTzDate() {
            checkType(PrimitiveType.Id.TzDate, type.getId());
            return dateTime;
        }

        @Override
        public ZonedDateTime getTzDatetime() {
            checkType(PrimitiveType.Id.TzDatetime, type.getId());
            return dateTime;
        }

        @Override
        public ZonedDateTime getTzTimestamp() {
            checkType(PrimitiveType.Id.TzTimestamp, type.getId());
            return dateTime;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            TzDatetime that = (TzDatetime) o;
            if (type.getId() != that.type.getId()) return false;
            return dateTime.equals(that.dateTime);
        }

        @Override
        public int hashCode() {
            int result = type.hashCode();
            result = 31 * result + dateTime.hashCode();
            return result;
        }

        @Override
        public String toString() {
            String timeStr = (type.getId() == PrimitiveType.Id.TzDate)
                ? dateTime.toLocalDate().toString()
                : dateTime.toLocalDateTime().toString();

            return timeStr + ',' + dateTime.getZone().getId();
        }

        @Override
        public ValueProtos.Value toPb() {
            return ProtoValue.text(toString());
        }
    }
}
