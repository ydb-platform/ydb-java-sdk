package tech.ydb.table.values;

import java.nio.charset.Charset;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
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

import tech.ydb.proto.ValueProtos;
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

    public int getUint8() {
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

    /** JVM does not support unsigned long, be careful when using this method
      for numbers greater than Long.MAX_VALUE.For correct work you can use wrappers
      like {@link com.google.common.primitives.UnsignedLong#fromLongBits(long) UnsignedLong }
     * @return signed long value corresponding to a bit representation of unsigned.*/
    public long getUint64() {
        throw new IllegalStateException("expected Uint64, but was " + getClass().getSimpleName());
    }

    public float getFloat() {
        throw new IllegalStateException("expected Float, but was " + getClass().getSimpleName());
    }

    public double getDouble() {
        throw new IllegalStateException("expected Double, but was " + getClass().getSimpleName());
    }

    public byte[] getBytes() {
        throw new IllegalStateException("expected Bytes, but was " + getClass().getSimpleName());
    }

    public byte[] getBytesUnsafe() {
        throw new IllegalStateException("expected Bytes, but was " + getClass().getSimpleName());
    }

    public ByteString getBytesAsByteString() {
        throw new IllegalStateException("expected String, but was " + getClass().getSimpleName());
    }

    public String getBytesAsString(Charset charset) {
        return new String(getBytesUnsafe(), charset);
    }

    public String getText() {
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

    @Deprecated
    public long getUuidHigh() {
        throw new IllegalStateException("expected Uuid, but was " + getClass().getSimpleName());
    }

    @Deprecated
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

    public static PrimitiveValue newBool(boolean value) {
        return value ? Bool.TRUE : Bool.FALSE;
    }

    public static PrimitiveValue newInt8(byte value) {
        return new Int8(value);
    }

    public static PrimitiveValue newUint8(int value) {
        return new Uint8(value);
    }

    public static PrimitiveValue newInt16(short value) {
        return new Int16(value);
    }

    public static PrimitiveValue newUint16(int value) {
        return new Uint16(value);
    }

    public static PrimitiveValue newInt32(int value) {
        return new Int32(value);
    }

    public static PrimitiveValue newUint32(long value) {
        return new Uint32(value);
    }

    public static PrimitiveValue newInt64(long value) {
        return new Int64(value);
    }

    public static PrimitiveValue newUint64(long value) {
        return new Uint64(value);
    }

    public static PrimitiveValue newFloat(float value) {
        return new FloatValue(value);
    }

    public static PrimitiveValue newDouble(double value) {
        return new DoubleValue(value);
    }

    public static PrimitiveValue newBytes(byte[] value) {
        return value.length == 0 ? Bytes.EMPTY_STRING : new Bytes(PrimitiveType.Bytes, value.clone());
    }

    public static PrimitiveValue newBytes(ByteString value) {
        return value.isEmpty() ? Bytes.EMPTY_STRING : new Bytes(PrimitiveType.Bytes, value);
    }

    public static PrimitiveValue newBytesOwn(byte[] value) {
        return value.length == 0 ? Bytes.EMPTY_STRING : new Bytes(PrimitiveType.Bytes, value);
    }

    public static PrimitiveValue newText(String value) {
        return value.isEmpty() ? Text.EMPTY_TEXT : new Text(PrimitiveType.Text, value);
    }

    public static PrimitiveValue newYson(byte[] value) {
        return value.length == 0 ? Bytes.EMPTY_YSON : new Bytes(PrimitiveType.Yson, value.clone());
    }

    public static PrimitiveValue newYson(ByteString value) {
        return value.isEmpty() ? Bytes.EMPTY_YSON : new Bytes(PrimitiveType.Yson, value);
    }

    public static PrimitiveValue newYsonOwn(byte[] value) {
        return value.length == 0 ? Bytes.EMPTY_YSON : new Bytes(PrimitiveType.Yson, value);
    }

    public static PrimitiveValue newJson(String value) {
        return value.isEmpty() ? Text.EMPTY_JSON : new Text(PrimitiveType.Json, value);
    }

    public static PrimitiveValue newJsonDocument(String value) {
        return value.isEmpty() ? Text.EMPTY_JSON_DOCUMENT : new Text(PrimitiveType.JsonDocument, value);
    }

    @Deprecated
    public static PrimitiveValue newUuid(long high, long low) {
        return ProtoValue.newUuid(high, low);
    }

    public static PrimitiveValue newUuid(UUID uuid) {
        return ProtoValue.newUuid(uuid);
    }

    public static PrimitiveValue newUuid(String uuid) {
        return ProtoValue.newUuid(uuid);
    }

    public static PrimitiveValue newDate(long daysSinceEpoch) {
        if (daysSinceEpoch < 0) {
            throw new IllegalArgumentException("negative daysSinceEpoch: " + daysSinceEpoch);
        }
        return new InstantValue(PrimitiveType.Date, TimeUnit.DAYS.toMicros(daysSinceEpoch));
    }

    public static PrimitiveValue newDate(LocalDate value) {
        return newDate(value.toEpochDay());
    }

    public static PrimitiveValue newDate(Instant value) {
        return newDate(TimeUnit.SECONDS.toDays(value.getEpochSecond()));
    }

    public static PrimitiveValue newDatetime(long secondsSinceEpoch) {
        if (secondsSinceEpoch < 0) {
            throw new IllegalArgumentException("negative secondsSinceEpoch: " + secondsSinceEpoch);
        }
        return new InstantValue(PrimitiveType.Datetime, TimeUnit.SECONDS.toMicros(secondsSinceEpoch));
    }

    public static PrimitiveValue newDatetime(Instant value) {
        return newDatetime(value.getEpochSecond());
    }

    public static PrimitiveValue newDatetime(LocalDateTime value) {
        return newDatetime(value.toEpochSecond(ZoneOffset.UTC));
    }

    public static PrimitiveValue newTimestamp(long microsSinceEpoch) {
        if (microsSinceEpoch < 0) {
            throw new IllegalArgumentException("Negative microsSinceEpoch: " + microsSinceEpoch);
        }
        return new InstantValue(PrimitiveType.Timestamp, microsSinceEpoch);
    }

    public static PrimitiveValue newTimestamp(Instant value) {
        long seconds = value.getEpochSecond();
        if (seconds < 0) {
            throw new IllegalArgumentException("Instant before epoch: " + value);
        }
        int nanos = value.getNano();
        long micros = seconds * 1000000L + nanos / 1000;
        return new InstantValue(PrimitiveType.Timestamp, micros);
    }

    public static PrimitiveValue newInterval(long micros) {
        return new IntervalValue(micros);
    }

    public static PrimitiveValue newInterval(Duration value) {
        return newInterval(TimeUnit.NANOSECONDS.toMicros(value.toNanos()));
    }

    public static PrimitiveValue newTzDate(ZonedDateTime dateTime) {
        return new TzDatetime(PrimitiveType.TzDate, dateTime);
    }

    public static PrimitiveValue newTzDatetime(ZonedDateTime dateTime) {
        return new TzDatetime(PrimitiveType.TzDatetime, dateTime);
    }

    public static PrimitiveValue newTzTimestamp(ZonedDateTime dateTime) {
        return new TzDatetime(PrimitiveType.TzTimestamp, dateTime);
    }

    // -- helpers --

    private static void checkType(PrimitiveType expected, PrimitiveType actual) {
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
            return PrimitiveType.Bool;
        }

        @Override
        public boolean getBool() {
            return value;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }

            if (o == null || getClass() != o.getClass()) {
                return false;
            }

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
            return ProtoValue.fromBool(value);
        }
    }

    private static final class Int8 extends PrimitiveValue {
        private final byte value;

        Int8(byte value) {
            this.value = value;
        }

        @Override
        public PrimitiveType getType() {
            return PrimitiveType.Int8;
        }

        @Override
        public byte getInt8() {
            return value;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }

            if (o == null || getClass() != o.getClass()) {
                return false;
            }

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
            return ProtoValue.fromInt8(value);
        }
    }

    private static final class Uint8 extends PrimitiveValue {
        private final int value;

        Uint8(int value) {
            this.value = value & 0xFF;
        }

        @Override
        public PrimitiveType getType() {
            return PrimitiveType.Uint8;
        }

        @Override
        public int getUint8() {
            return value;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }

            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            return value == ((Uint8) o).value;
        }

        @Override
        public int hashCode() {
            return Integer.hashCode(value);
        }

        @Override
        public String toString() {
            return Integer.toUnsignedString(value);
        }

        @Override
        public ValueProtos.Value toPb() {
            return ProtoValue.fromUint8(value);
        }
    }

    private static final class Int16 extends PrimitiveValue {
        private final short value;

        Int16(short value) {
            this.value = value;
        }

        @Override
        public PrimitiveType getType() {
            return PrimitiveType.Int16;
        }

        @Override
        public short getInt16() {
            return value;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }

            if (o == null || getClass() != o.getClass()) {
                return false;
            }

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
            return ProtoValue.fromInt16(value);
        }
    }

    private static final class Uint16 extends PrimitiveValue {
        private final int value;

        Uint16(int value) {
            this.value = value & 0xFFFF;
        }

        @Override
        public PrimitiveType getType() {
            return PrimitiveType.Uint16;
        }

        @Override
        public int getUint16() {
            return value;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }

            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            return value == ((Uint16) o).value;
        }

        @Override
        public int hashCode() {
            return Integer.hashCode(value);
        }

        @Override
        public String toString() {
            return Integer.toUnsignedString(value);
        }

        @Override
        public ValueProtos.Value toPb() {
            return ProtoValue.fromUint16(value);
        }
    }

    private static final class Int32 extends PrimitiveValue {
        private final int value;

        Int32(int value) {
            this.value = value;
        }

        @Override
        public PrimitiveType getType() {
            return PrimitiveType.Int32;
        }

        @Override
        public int getInt32() {
            return value;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }

            if (o == null || getClass() != o.getClass()) {
                return false;
            }

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
            return ProtoValue.fromInt32(value);
        }
    }

    private static final class Uint32 extends PrimitiveValue {
        private final long value;

        Uint32(long value) {
            this.value = value & 0xFFFFFFFFL;
        }

        @Override
        public PrimitiveType getType() {
            return PrimitiveType.Uint32;
        }

        @Override
        public long getUint32() {
            return value;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }

            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            return value == ((Uint32) o).value;
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
            return ProtoValue.fromUint32(value);
        }
    }

    private static final class Int64 extends PrimitiveValue {
        private final long value;

        Int64(long value) {
            this.value = value;
        }

        @Override
        public PrimitiveType getType() {
            return PrimitiveType.Int64;
        }

        @Override
        public long getInt64() {
            return value;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }

            if (o == null || getClass() != o.getClass()) {
                return false;
            }

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
            return ProtoValue.fromInt64(value);
        }
    }

    private static final class Uint64 extends PrimitiveValue {
        private final long value;

        Uint64(long value) {
            this.value = value;
        }

        @Override
        public PrimitiveType getType() {
            return PrimitiveType.Uint64;
        }

        @Override
        public long getUint64() {
            return value;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }

            if (o == null || getClass() != o.getClass()) {
                return false;
            }

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
            return ProtoValue.fromUint64(value);
        }
    }

    private static final class FloatValue extends PrimitiveValue {
        private final float value;

        FloatValue(float value) {
            this.value = value;
        }

        @Override
        public PrimitiveType getType() {
            return PrimitiveType.Float;
        }

        @Override
        public float getFloat() {
            return value;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }

            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            FloatValue that = (FloatValue) o;
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
            return ProtoValue.fromFloat(value);
        }
    }

    private static final class DoubleValue extends PrimitiveValue {
        private final double value;

        DoubleValue(double value) {
            this.value = value;
        }

        @Override
        public PrimitiveType getType() {
            return PrimitiveType.Double;
        }

        @Override
        public double getDouble() {
            return value;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }

            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            DoubleValue that = (DoubleValue) o;
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
            return ProtoValue.fromDouble(value);
        }
    }

    private static final class Bytes extends PrimitiveValue {
        private static final Bytes EMPTY_STRING = new Bytes(PrimitiveType.Bytes, new byte[0]);
        private static final Bytes EMPTY_YSON = new Bytes(PrimitiveType.Yson, new byte[0]);

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
        public byte[] getBytes() {
            return getBytes(PrimitiveType.Bytes);
        }

        @Override
        public byte[] getBytesUnsafe() {
            return getBytesUnsafe(PrimitiveType.Bytes);
        }

        @Override
        public ByteString getBytesAsByteString() {
            return getByteString(PrimitiveType.Bytes);
        }

        @Override
        public byte[] getYson() {
            return getBytes(PrimitiveType.Yson);
        }

        @Override
        public byte[] getYsonUnsafe() {
            return getBytesUnsafe(PrimitiveType.Yson);
        }

        @Override
        public ByteString getYsonBytes() {
            return getByteString(PrimitiveType.Yson);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }

            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            Bytes that = (Bytes) o;
            if (type != that.type) {
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
                ByteString v = (ByteString) this.value;
                for (int i = 0; i < v.size(); i++) {
                    byte b = v.byteAt(i);
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
                ByteString bytes = (ByteString) this.value;
                for (int i = 0; i < bytes.size(); i++) {
                    encodeAsOctal(sb, bytes.byteAt(i));
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
            return ProtoValue.fromBytes(getByteString(type));
        }

        private byte[] getBytes(PrimitiveType expected) {
            checkType(expected, type);

            if (value instanceof byte[]) {
                return ((byte[]) value).clone();
            }
            return ((ByteString) value).toByteArray();
        }

        private byte[] getBytesUnsafe(PrimitiveType expected) {
            checkType(expected, type);

            if (value instanceof byte[]) {
                return (byte[]) value;
            }
            return ((ByteString) value).toByteArray();
        }

        private ByteString getByteString(PrimitiveType expected) {
            checkType(expected, type);

            if (value instanceof byte[]) {
                return UnsafeByteOperations.unsafeWrap((byte[]) value);
            }
            return (ByteString) value;
        }
    }

    private static final class Text extends PrimitiveValue {
        private static final Text EMPTY_TEXT = new Text(PrimitiveType.Text, "");
        private static final Text EMPTY_JSON = new Text(PrimitiveType.Json, "");
        private static final Text EMPTY_JSON_DOCUMENT = new Text(PrimitiveType.JsonDocument, "");

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
        public String getText() {
            checkType(PrimitiveType.Text, type);
            return value;
        }

        @Override
        public String getJson() {
            checkType(PrimitiveType.Json, type);
            return value;
        }

        @Override
        public String getJsonDocument() {
            checkType(PrimitiveType.JsonDocument, type);
            return value;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }

            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            Text that = (Text) o;
            if (type != that.type) {
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
            return ProtoValue.fromText(value);
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
            checkType(PrimitiveType.Date, type);
            return ProtoValue.toDate(TimeUnit.MICROSECONDS.toDays(microsSinceEpoch));
        }

        @Override
        public LocalDateTime getDatetime() {
            checkType(PrimitiveType.Datetime, type);
            return ProtoValue.toDatetime(TimeUnit.MICROSECONDS.toSeconds(microsSinceEpoch));
        }

        @Override
        public Instant getTimestamp() {
            checkType(PrimitiveType.Timestamp, type);
            return ProtoValue.toTimestamp(microsSinceEpoch);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }

            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            InstantValue that = (InstantValue) o;
            return microsSinceEpoch == that.microsSinceEpoch && type == that.type;
        }

        @Override
        public int hashCode() {
            return 31 * type.hashCode() + (int) (microsSinceEpoch ^ (microsSinceEpoch >>> 32));
        }

        @Override
        public String toString() {
            switch (type) {
                case Date: return DateTimeFormatter.ISO_DATE.format(getDate());
                case Datetime: return DateTimeFormatter.ISO_DATE_TIME.format(getDatetime());
                case Timestamp: return DateTimeFormatter.ISO_INSTANT.format(getTimestamp());
                default:
                    throw new IllegalStateException("unsupported type: " + type);
            }
        }

        @Override
        public ValueProtos.Value toPb() {
            switch (type) {
                case Date: return ProtoValue.fromDate(TimeUnit.MICROSECONDS.toDays(microsSinceEpoch));
                case Datetime: return ProtoValue.fromDatetime(TimeUnit.MICROSECONDS.toSeconds(microsSinceEpoch));
                case Timestamp: return ProtoValue.fromTimestamp(microsSinceEpoch);
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
            return PrimitiveType.Interval;
        }

        @Override
        public Duration getInterval() {
            return Duration.of(micros, ChronoUnit.MICROS);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }

            if (o == null || getClass() != o.getClass()) {
                return false;
            }

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
            return ProtoValue.fromInterval(micros);
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
            checkType(PrimitiveType.TzDate, type);
            return dateTime;
        }

        @Override
        public ZonedDateTime getTzDatetime() {
            checkType(PrimitiveType.TzDatetime, type);
            return dateTime;
        }

        @Override
        public ZonedDateTime getTzTimestamp() {
            checkType(PrimitiveType.TzTimestamp, type);
            return dateTime;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }

            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            TzDatetime that = (TzDatetime) o;
            return type == that.type && dateTime.equals(that.dateTime);
        }

        @Override
        public int hashCode() {
            int result = type.hashCode();
            result = 31 * result + dateTime.hashCode();
            return result;
        }

        @Override
        public String toString() {
            String timeStr = (type == PrimitiveType.TzDate)
                ? dateTime.toLocalDate().toString()
                : dateTime.toLocalDateTime().toString();

            return timeStr + ',' + dateTime.getZone().getId();
        }

        @Override
        public ValueProtos.Value toPb() {
            return ProtoValue.fromText(toString());
        }
    }
}
