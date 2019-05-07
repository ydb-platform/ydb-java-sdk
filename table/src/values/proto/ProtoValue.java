package ru.yandex.ydb.table.values.proto;

import java.nio.charset.Charset;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import com.google.protobuf.ByteString;
import com.google.protobuf.NullValue;
import com.google.protobuf.UnsafeByteOperations;

import ru.yandex.ydb.ValueProtos;
import ru.yandex.ydb.table.types.DecimalType;
import ru.yandex.ydb.table.types.DictType;
import ru.yandex.ydb.table.types.ListType;
import ru.yandex.ydb.table.types.OptionalType;
import ru.yandex.ydb.table.types.PrimitiveType;
import ru.yandex.ydb.table.types.StructType;
import ru.yandex.ydb.table.types.TupleType;
import ru.yandex.ydb.table.types.Type;
import ru.yandex.ydb.table.types.VariantType;
import ru.yandex.ydb.table.utils.LittleEndian;
import ru.yandex.ydb.table.values.DecimalValue;
import ru.yandex.ydb.table.values.DictValue;
import ru.yandex.ydb.table.values.ListValue;
import ru.yandex.ydb.table.values.OptionalValue;
import ru.yandex.ydb.table.values.PrimitiveValue;
import ru.yandex.ydb.table.values.StructValue;
import ru.yandex.ydb.table.values.TupleValue;
import ru.yandex.ydb.table.values.Value;
import ru.yandex.ydb.table.values.VariantValue;
import ru.yandex.ydb.table.values.VoidValue;


/**
 * @author Sergey Polovko
 *
 * TODO: check sizes of types and values
 */
public class ProtoValue {

    private static final ValueProtos.Value TRUE = ValueProtos.Value.newBuilder().setBoolValue(true).build();
    private static final ValueProtos.Value FALSE = ValueProtos.Value.newBuilder().setBoolValue(false).build();

    private static final ValueProtos.Value EMPTY = ValueProtos.Value.newBuilder().build();

    private static final ValueProtos.Value EMPTY_OPTIONAL = ValueProtos.Value.newBuilder()
        .setNullFlagValue(NullValue.NULL_VALUE)
        .build();

    private static final ValueProtos.Value VOID = ValueProtos.Value.newBuilder()
        .setNullFlagValue(NullValue.NULL_VALUE)
        .build();

    // -- integers --

    public static ValueProtos.Value bool(boolean value) {
        return value ? TRUE : FALSE;
    }

    public static boolean toBool(ValueProtos.Value value) {
        return value.getBoolValue();
    }

    public static ValueProtos.Value int8(byte value) {
        return ValueProtos.Value.newBuilder().setInt32Value(value).build();
    }

    public static byte toInt8(ValueProtos.Value value) {
        return (byte) value.getInt32Value();
    }

    public static ValueProtos.Value uint8(byte value) {
        return ValueProtos.Value.newBuilder().setUint32Value(Byte.toUnsignedInt(value)).build();
    }

    public static int toUint8(ValueProtos.Value value) {
        return value.getUint32Value();
    }

    public static ValueProtos.Value int16(short value) {
        return ValueProtos.Value.newBuilder().setInt32Value(value).build();
    }

    public static short toInt16(ValueProtos.Value value) {
        return (short) value.getInt32Value();
    }

    public static ValueProtos.Value uint16(short value) {
        return ValueProtos.Value.newBuilder().setUint32Value(Short.toUnsignedInt(value)).build();
    }

    public static int toUint16(ValueProtos.Value value) {
        return value.getUint32Value();
    }

    public static ValueProtos.Value int32(int value) {
        return ValueProtos.Value.newBuilder().setInt32Value(value).build();
    }

    public static int toInt32(ValueProtos.Value value) {
        return value.getInt32Value();
    }

    public static ValueProtos.Value uint32(int value) {
        return ValueProtos.Value.newBuilder().setUint32Value(value).build();
    }

    public static long toUint32(ValueProtos.Value value) {
        return Integer.toUnsignedLong(value.getUint32Value());
    }

    public static ValueProtos.Value uint32(long value) {
        return ValueProtos.Value.newBuilder().setUint32Value((int) (value & 0xffffffffL)).build();
    }

    public static ValueProtos.Value int64(long value) {
        return ValueProtos.Value.newBuilder().setInt64Value(value).build();
    }

    public static long toInt64(ValueProtos.Value value) {
        return value.getInt64Value();
    }

    public static ValueProtos.Value uint64(long value) {
        return ValueProtos.Value.newBuilder().setUint64Value(value).build();
    }

    public static long toUint64(ValueProtos.Value value) {
        return value.getUint64Value();
    }

    // - float32, float64 -

    public static ValueProtos.Value float32(float value) {
        return ValueProtos.Value.newBuilder().setFloatValue(value).build();
    }

    public static float toFloat32(ValueProtos.Value value) {
        return value.getFloatValue();
    }

    public static ValueProtos.Value float64(double value) {
        return ValueProtos.Value.newBuilder().setDoubleValue(value).build();
    }

    public static double toFloat64(ValueProtos.Value value) {
        return value.getDoubleValue();
    }

    // - string -

    public static ValueProtos.Value bytes(ByteString value) {
        return ValueProtos.Value.newBuilder().setBytesValue(value).build();
    }

    public static ValueProtos.Value string(byte[] value) {
        return bytes(ByteString.copyFrom(value));
    }

    /**
     * will not copy given array
     */
    public static ValueProtos.Value stringOwn(byte[] value) {
        return bytes(UnsafeByteOperations.unsafeWrap(value));
    }

    public static ValueProtos.Value string(String value, Charset charset) {
        return bytes(UnsafeByteOperations.unsafeWrap(charset.encode(value)));
    }

    public static byte[] toString(ValueProtos.Value value) {
        return value.getBytesValue().toByteArray();
    }

    // - utf8 -

    public static ValueProtos.Value text(String value) {
        return ValueProtos.Value.newBuilder().setTextValue(value).build();
    }

    public static ValueProtos.Value utf8(String value) {
        return text(value);
    }

    public static String toUtf8(ValueProtos.Value value) {
        return value.getTextValue();
    }

    // - yson -

    public static ValueProtos.Value yson(byte[] value) {
        return bytes(ByteString.copyFrom(value));
    }

    /**
     * will not copy given array
     */
    public static ValueProtos.Value ysonOwn(byte[] value) {
        return bytes(UnsafeByteOperations.unsafeWrap(value));
    }

    public static byte[] toYson(ValueProtos.Value value) {
        return value.getBytesValue().toByteArray();
    }

    // - json -

    public static ValueProtos.Value json(String value) {
        return text(value);
    }

    public static String toJson(ValueProtos.Value value) {
        return value.getTextValue();
    }

    // - uuid -

    public static ValueProtos.Value uuid(long high, long low) {
        return ValueProtos.Value.newBuilder().setHigh128(high).setLow128(low).build();
    }

    public static ValueProtos.Value uuid(UUID uuid) {
        long msb = uuid.getMostSignificantBits();
        long lsb = uuid.getLeastSignificantBits();

        long timeLow = (msb & 0xffffffff00000000L) >>> 32;
        long timeMid = (msb & 0x00000000ffff0000L) << 16;
        long timeHighAndVersion = (msb & 0x000000000000ffffL) << 48;

        long low = timeLow | timeMid | timeHighAndVersion;
        long high = LittleEndian.bswap(lsb);

        return uuid(high, low);
    }

    public static UUID toUuid(ValueProtos.Value value) {
        final long high = value.getHigh128();
        final long low = value.getLow128();

        long timeLow = (low & 0x00000000ffffffffL) << 32;
        long timeMid = (low & 0x0000ffff00000000L) >>> 16;
        long timeHighAndVersion = (low & 0xffff000000000000L) >>> 48;

        long hiBe = LittleEndian.bswap(high);
        return new UUID(timeLow | timeMid | timeHighAndVersion, hiBe);
    }

    // - interval -

    public static ValueProtos.Value interval(long micros) {
        return ValueProtos.Value.newBuilder().setInt64Value(micros).build();
    }

    public static ValueProtos.Value interval(Duration value) {
        return interval(TimeUnit.NANOSECONDS.toMicros(value.toNanos()));
    }

    public static Duration toInterval(ValueProtos.Value value) {
        return Duration.ofNanos(TimeUnit.MICROSECONDS.toNanos(value.getInt64Value()));
    }

    // - date -

    public static ValueProtos.Value date(long daysSinceEpoch) {
        int daysInt = (int) (daysSinceEpoch & 0xffffffffL);
        return ValueProtos.Value.newBuilder().setUint32Value(daysInt).build();
    }

    public static ValueProtos.Value date(LocalDate value) {
        return date(value.toEpochDay());
    }

    public static ValueProtos.Value date(Instant value) {
        return date(TimeUnit.SECONDS.toDays(value.getEpochSecond()));
    }

    public static LocalDate toDate(long daysSinceEpoch) {
        return LocalDate.ofEpochDay(daysSinceEpoch);
    }

    public static LocalDate toDate(ValueProtos.Value value) {
        return toDate(Integer.toUnsignedLong(value.getUint32Value()));
    }

    // - datetime -

    public static ValueProtos.Value datetime(long secondsSinceEpoch) {
        int secondsInt = (int) (secondsSinceEpoch & 0xffffffffL);
        return ValueProtos.Value.newBuilder().setUint32Value(secondsInt).build();
    }

    public static ValueProtos.Value datetime(Instant value) {
        return datetime(value.getEpochSecond());
    }

    public static LocalDateTime toDatetime(long secondsSinceEpoch) {
        return LocalDateTime.ofEpochSecond(secondsSinceEpoch, 0, ZoneOffset.UTC);
    }

    public static LocalDateTime toDatetime(ValueProtos.Value value) {
        return toDatetime(Integer.toUnsignedLong(value.getUint32Value()));
    }

    // - timestamp -

    public static ValueProtos.Value timestamp(long microsSinceEpoch) {
        return ValueProtos.Value.newBuilder().setUint64Value(microsSinceEpoch).build();
    }

    public static ValueProtos.Value timestamp(Instant value) {
        long micros = TimeUnit.SECONDS.toMicros(value.getEpochSecond()) +
            TimeUnit.NANOSECONDS.toMicros(value.getNano());
        return timestamp(micros);
    }

    public static Instant toTimestamp(long microsSinceEpoch) {
        long seconds = TimeUnit.MICROSECONDS.toSeconds(microsSinceEpoch);
        long micros = microsSinceEpoch - TimeUnit.SECONDS.toMicros(seconds);
        return Instant.ofEpochSecond(seconds, TimeUnit.MICROSECONDS.toNanos(micros));
    }

    public static Instant toTimestamp(ValueProtos.Value value) {
        return toTimestamp(value.getUint64Value());
    }

    // - tzDate -

    public static ValueProtos.Value tzDate(String text) {
        return text(text);
    }

    public static ZonedDateTime toTzDate(String textValue) {
        int commaIdx = textValue.indexOf(',');
        if (commaIdx == -1) {
            throw new IllegalArgumentException("cannot parse TzDate from: \'" + textValue + '\'');
        }

        LocalDate date = LocalDate.parse(textValue.substring(0, commaIdx));
        ZoneId zoneId = ZoneId.of(textValue.substring(commaIdx + 1));
        return LocalDateTime.of(date, LocalTime.MIDNIGHT).atZone(zoneId);
    }

    public static ZonedDateTime toTzDate(ValueProtos.Value value) {
        String textValue = value.getTextValue();
        return toTzDate(textValue);
    }

    // - tzDatetime -

    public static ValueProtos.Value tzDatetime(String text) {
        return text(text);
    }

    public static ZonedDateTime toTzDatetime(String textValue) {
        int commaIdx = textValue.indexOf(',');
        if (commaIdx == -1) {
            throw new IllegalArgumentException("cannot parse TzDatetime from: \'" + textValue + '\'');
        }
        Instant instant = Instant.parse(textValue.substring(0, commaIdx) + 'Z')
            .truncatedTo(ChronoUnit.SECONDS);
        LocalDateTime dateTime = LocalDateTime.ofInstant(instant, ZoneOffset.UTC);
        ZoneId zoneId = ZoneId.of(textValue.substring(commaIdx + 1));
        return dateTime.atZone(zoneId);
    }

    public static ZonedDateTime toTzDatetime(ValueProtos.Value value) {
        String textValue = value.getTextValue();
        return toTzDatetime(textValue);
    }

    // - tzTimestamp -

    public static ZonedDateTime toTzTimestamp(String value) {
        int commaIdx = value.indexOf(',');
        if (commaIdx == -1) {
            throw new IllegalArgumentException("cannot parse TzTimestamp from: \'" + value + '\'');
        }
        Instant instant = Instant.parse(value.substring(0, commaIdx) + 'Z')
            .truncatedTo(ChronoUnit.MICROS);
        LocalDateTime dateTime = LocalDateTime.ofInstant(instant, ZoneOffset.UTC);
        ZoneId zoneId = ZoneId.of(value.substring(commaIdx + 1));
        return dateTime.atZone(zoneId);
    }

    public static ZonedDateTime toTzTimestamp(ValueProtos.Value value) {
        return toTzTimestamp(value.getTextValue());
    }

    // -- decimal --

    public static ValueProtos.Value decimal(long high, long low) {
        return ValueProtos.Value.newBuilder()
            .setHigh128(high)
            .setLow128(low)
            .build();
    }

    public static DecimalValue toDecimal(ValueProtos.Type type, ValueProtos.Value value) {
        ValueProtos.DecimalType decimalType = type.getDecimalType();
        return DecimalValue.of(
            DecimalType.of(decimalType.getPrecision(), decimalType.getScale()),
            value.getHigh128(), value.getLow128());
    }

    // -- dict value --

    public static ValueProtos.Value dict() {
        return EMPTY;
    }

    public static ValueProtos.Value dict(ValueProtos.Value key, ValueProtos.Value value) {
        ValueProtos.Value.Builder builder = ValueProtos.Value.newBuilder();
        builder.addPairsBuilder()
            .setKey(key)
            .setPayload(value);
        return builder.build();
    }

    public static ValueProtos.Value dict(
        ValueProtos.Value key1, ValueProtos.Value value1,
        ValueProtos.Value key2, ValueProtos.Value value2)
    {
        ValueProtos.Value.Builder builder = ValueProtos.Value.newBuilder();
        builder.addPairsBuilder()
            .setKey(key1)
            .setPayload(value1);
        builder.addPairsBuilder()
            .setKey(key2)
            .setPayload(value2);
        return builder.build();
    }

    public static ValueProtos.Value dict(
        ValueProtos.Value key1, ValueProtos.Value value1,
        ValueProtos.Value key2, ValueProtos.Value value2,
        ValueProtos.Value key3, ValueProtos.Value value3)
    {
        ValueProtos.Value.Builder builder = ValueProtos.Value.newBuilder();
        builder.addPairsBuilder()
            .setKey(key1)
            .setPayload(value1);
        builder.addPairsBuilder()
            .setKey(key2)
            .setPayload(value2);
        builder.addPairsBuilder()
            .setKey(key3)
            .setPayload(value3);
        return builder.build();
    }

    public static ValueProtos.Value dict(
        ValueProtos.Value key1, ValueProtos.Value value1,
        ValueProtos.Value key2, ValueProtos.Value value2,
        ValueProtos.Value key3, ValueProtos.Value value3,
        ValueProtos.Value key4, ValueProtos.Value value4)
    {
        ValueProtos.Value.Builder builder = ValueProtos.Value.newBuilder();
        builder.addPairsBuilder()
            .setKey(key1)
            .setPayload(value1);
        builder.addPairsBuilder()
            .setKey(key2)
            .setPayload(value2);
        builder.addPairsBuilder()
            .setKey(key3)
            .setPayload(value3);
        builder.addPairsBuilder()
            .setKey(key4)
            .setPayload(value4);
        return builder.build();
    }

    public static ValueProtos.Value dict(
        ValueProtos.Value key1, ValueProtos.Value value1,
        ValueProtos.Value key2, ValueProtos.Value value2,
        ValueProtos.Value key3, ValueProtos.Value value3,
        ValueProtos.Value key4, ValueProtos.Value value4,
        ValueProtos.Value key5, ValueProtos.Value value5)
    {
        ValueProtos.Value.Builder builder = ValueProtos.Value.newBuilder();
        builder.addPairsBuilder()
            .setKey(key1)
            .setPayload(value1);
        builder.addPairsBuilder()
            .setKey(key2)
            .setPayload(value2);
        builder.addPairsBuilder()
            .setKey(key3)
            .setPayload(value3);
        builder.addPairsBuilder()
            .setKey(key4)
            .setPayload(value4);
        builder.addPairsBuilder()
            .setKey(key5)
            .setPayload(value5);
        return builder.build();
    }

    public static ValueProtos.Value dict(ValueProtos.ValuePair... pairs) {
        if (pairs.length == 0) {
            return EMPTY;
        }

        ValueProtos.Value.Builder builder = ValueProtos.Value.newBuilder();
        for (ValueProtos.ValuePair pair : pairs) {
            builder.addPairs(pair);
        }
        return builder.build();
    }

    // -- list value --

    public static ValueProtos.Value list() {
        return EMPTY;
    }

    public static ValueProtos.Value list(ValueProtos.Value item) {
        ValueProtos.Value.Builder builder = ValueProtos.Value.newBuilder();
        builder.addItems(item);
        return builder.build();
    }

    public static ValueProtos.Value list(ValueProtos.Value item1, ValueProtos.Value item2) {
        ValueProtos.Value.Builder builder = ValueProtos.Value.newBuilder();
        builder.addItems(item1);
        builder.addItems(item2);
        return builder.build();
    }

    public static ValueProtos.Value list(ValueProtos.Value item1, ValueProtos.Value item2, ValueProtos.Value item3) {
        ValueProtos.Value.Builder builder = ValueProtos.Value.newBuilder();
        builder.addItems(item1);
        builder.addItems(item2);
        builder.addItems(item3);
        return builder.build();
    }

    public static ValueProtos.Value list(
        ValueProtos.Value item1,
        ValueProtos.Value item2,
        ValueProtos.Value item3,
        ValueProtos.Value item4)
    {
        ValueProtos.Value.Builder builder = ValueProtos.Value.newBuilder();
        builder.addItems(item1);
        builder.addItems(item2);
        builder.addItems(item3);
        builder.addItems(item4);
        return builder.build();
    }

    public static ValueProtos.Value list(
        ValueProtos.Value item1,
        ValueProtos.Value item2,
        ValueProtos.Value item3,
        ValueProtos.Value item4,
        ValueProtos.Value item5)
    {
        ValueProtos.Value.Builder builder = ValueProtos.Value.newBuilder();
        builder.addItems(item1);
        builder.addItems(item2);
        builder.addItems(item3);
        builder.addItems(item4);
        builder.addItems(item5);
        return builder.build();
    }

    public static ValueProtos.Value list(ValueProtos.Value... items) {
        if (items.length == 0) {
            return EMPTY;
        }

        ValueProtos.Value.Builder builder = ValueProtos.Value.newBuilder();
        for (ValueProtos.Value item : items) {
            builder.addItems(item);
        }
        return builder.build();
    }

    public static ValueProtos.Value list(Iterable<ValueProtos.Value> items) {
        ValueProtos.Value.Builder builder = ValueProtos.Value.newBuilder();
        builder.addAllItems(items);
        return builder.build();
    }

    // -- optional value --

    public static ValueProtos.Value optional() {
        return EMPTY_OPTIONAL;
    }

    public static ValueProtos.Value optional(ValueProtos.Value value) {
        if (value.getValueCase() != ValueProtos.Value.ValueCase.NULL_FLAG_VALUE) {
            return value;
        }
        ValueProtos.Value.Builder builder = ValueProtos.Value.newBuilder();
        builder.setNestedValue(value);
        return builder.build();
    }

    // -- struct value --

    public static ValueProtos.Value struct(ValueProtos.Value member) {
        return list(member);
    }

    public static ValueProtos.Value struct(ValueProtos.Value member1, ValueProtos.Value member2) {
        return list(member1, member2);
    }

    public static ValueProtos.Value struct(
        ValueProtos.Value member1,
        ValueProtos.Value member2,
        ValueProtos.Value member3)
    {
        return list(member1, member2, member3);
    }

    public static ValueProtos.Value struct(
        ValueProtos.Value member1,
        ValueProtos.Value member2,
        ValueProtos.Value member3,
        ValueProtos.Value member4)
    {
        return list(member1, member2, member3, member4);
    }

    public static ValueProtos.Value struct(
        ValueProtos.Value member1,
        ValueProtos.Value member2,
        ValueProtos.Value member3,
        ValueProtos.Value member4,
        ValueProtos.Value member5)
    {
        return list(member1, member2, member3, member4, member5);
    }

    public static ValueProtos.Value struct(ValueProtos.Value firstMember, ValueProtos.Value... members) {
        ValueProtos.Value.Builder builder = ValueProtos.Value.newBuilder();
        builder.addItems(firstMember);
        for (ValueProtos.Value member : members) {
            builder.addItems(member);

        }
        return builder.build();
    }

    // -- tuple value --

    public static ValueProtos.Value tuple(ValueProtos.Value member) {
        return list(member);
    }

    public static ValueProtos.Value tuple(ValueProtos.Value member1, ValueProtos.Value member2) {
        return list(member1, member2);
    }

    public static ValueProtos.Value tuple(
        ValueProtos.Value member1,
        ValueProtos.Value member2,
        ValueProtos.Value member3)
    {
        return list(member1, member2, member3);
    }

    public static ValueProtos.Value tuple(
        ValueProtos.Value member1,
        ValueProtos.Value member2,
        ValueProtos.Value member3,
        ValueProtos.Value member4)
    {
        return list(member1, member2, member3, member4);
    }

    public static ValueProtos.Value tuple(
        ValueProtos.Value member1,
        ValueProtos.Value member2,
        ValueProtos.Value member3,
        ValueProtos.Value member4,
        ValueProtos.Value member5)
    {
        return list(member1, member2, member3, member4, member5);
    }

    public static ValueProtos.Value tuple(ValueProtos.Value... items) {
        return list(items);
    }

    // -- void value --

    public static ValueProtos.Value voidValue() {
        return VOID;
    }

    // -- from proto --

    public static Value fromPb(Type type, ValueProtos.Value value) {
        switch (type.getKind()) {
            case PRIMITIVE:
                return primitiveFromPb((PrimitiveType) type, value);

            case DECIMAL: {
                DecimalType decimalType = (DecimalType) type;
                return DecimalValue.of(decimalType, value.getHigh128(), value.getLow128());
            }

            case DICT: {
                DictType dictType = (DictType) type;

                if (value.getPairsCount() == 0) {
                    return DictValue.of();
                }

                HashMap<Value, Value> items = new HashMap<>(value.getPairsCount());
                for (int i = 0; i < value.getPairsCount(); i++) {
                    ValueProtos.ValuePair pair = value.getPairs(i);
                    items.put(
                        fromPb(dictType.getKeyType(), pair.getKey()),
                        fromPb(dictType.getValueType(), pair.getPayload()));
                }
                return DictValue.fromMapOwn(items);
            }

            case LIST: {
                ListType listType = (ListType) type;

                if (value.getItemsCount() == 0) {
                    return ListValue.of();
                }

                Value[] items = new Value[value.getItemsCount()];
                for (int i = 0; i < value.getItemsCount(); i++) {
                    items[i] = fromPb(listType.getItemType(), value.getItems(i));
                }
                return ListValue.fromArrayOwn(items);
            }

            case OPTIONAL: {
                OptionalType optionalType = (OptionalType) type;

                switch (value.getValueCase()) {
                    case NESTED_VALUE:
                        return OptionalValue.of(fromPb(optionalType.getItemType(), value.getNestedValue()));
                    case NULL_FLAG_VALUE:
                        return OptionalValue.empty();
                    default:
                        return OptionalValue.of(fromPb(optionalType.getItemType(), value));
                }
            }

            case STRUCT: {
                StructType structType = (StructType) type;

                if (value.getItemsCount() == 1) {
                    return StructValue.of(fromPb(structType.getMemberType(0), value.getItems(0)));
                }

                Value[] members = new Value[value.getItemsCount()];
                for (int i = 0; i < value.getItemsCount(); i++) {
                    members[i] = fromPb(structType.getMemberType(i), value.getItems(i));
                }
                return StructValue.ofOwn(members);
            }

            case TUPLE: {
                TupleType tupleType = (TupleType) type;

                if (value.getItemsCount() == 0) {
                    return TupleValue.of();
                }

                Value[] items = new Value[value.getItemsCount()];
                for (int i = 0; i < value.getItemsCount(); i++) {
                    items[i] = fromPb(tupleType.getElementType(i), value.getItems(i));
                }
                return TupleValue.fromArrayOwn(items);
            }

            case VARIANT: {
                VariantType variantType = (VariantType) type;
                Type itemType = variantType.getItemType(value.getVariantIndex());
                ValueProtos.Value itemValue = value.getNestedValue();
                return VariantValue.of(value.getVariantIndex(), fromPb(itemType, itemValue));
            }

            case VOID:
                return VoidValue.of();
        }

        throw new IllegalStateException("unknown type kind: " + type.getKind());
    }

    private static PrimitiveValue primitiveFromPb(PrimitiveType primitiveType, ValueProtos.Value value) {
        switch (primitiveType.getId()) {
            case Bool: return PrimitiveValue.bool(toBool(value));
            case Int8: return PrimitiveValue.int8(toInt8(value));
            case Uint8: return PrimitiveValue.uint8((byte) value.getUint32Value());
            case Int16: return PrimitiveValue.int16(toInt16(value));
            case Uint16: return PrimitiveValue.uint16((short) value.getUint32Value());
            case Int32: return PrimitiveValue.int32(toInt32(value));
            case Uint32: return PrimitiveValue.uint32(value.getUint32Value());
            case Int64: return PrimitiveValue.int64(toInt64(value));
            case Uint64: return PrimitiveValue.uint64(toUint64(value));
            case Float: return PrimitiveValue.float32(toFloat32(value));
            case Double: return PrimitiveValue.float64(toFloat64(value));
            case String: return PrimitiveValue.string(value.getBytesValue());
            case Utf8: return PrimitiveValue.utf8(value.getTextValue());
            case Yson: return PrimitiveValue.yson(value.getBytesValue());
            case Json: return PrimitiveValue.json(value.getTextValue());
            case Uuid: return PrimitiveValue.uuid(value.getHigh128(), value.getLow128());
            case Date: return PrimitiveValue.date(Integer.toUnsignedLong(value.getUint32Value()));
            case Datetime: return PrimitiveValue.datetime(Integer.toUnsignedLong(value.getUint32Value()));
            case Timestamp: return PrimitiveValue.timestamp(value.getUint64Value());
            case Interval: return PrimitiveValue.interval(value.getInt64Value());
            case TzDate: return PrimitiveValue.tzDate(toTzDate(value));
            case TzDatetime: return PrimitiveValue.tzDatetime(toTzDatetime(value));
            case TzTimestamp: return PrimitiveValue.tzTimestamp(toTzTimestamp(value));
        }
        throw new IllegalStateException("unknown PrimitiveType: " + primitiveType.getId());
    }
}
