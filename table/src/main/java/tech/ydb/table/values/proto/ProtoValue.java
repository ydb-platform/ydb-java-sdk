package tech.ydb.table.values.proto;

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

import tech.ydb.ValueProtos;
import tech.ydb.table.utils.LittleEndian;
import tech.ydb.table.values.DecimalType;
import tech.ydb.table.values.DecimalValue;
import tech.ydb.table.values.DictType;
import tech.ydb.table.values.ListType;
import tech.ydb.table.values.OptionalType;
import tech.ydb.table.values.PrimitiveType;
import tech.ydb.table.values.PrimitiveValue;
import tech.ydb.table.values.StructType;
import tech.ydb.table.values.TupleType;
import tech.ydb.table.values.TupleValue;
import tech.ydb.table.values.Type;
import tech.ydb.table.values.Value;
import tech.ydb.table.values.VariantType;
import tech.ydb.table.values.VoidValue;


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

    private ProtoValue() { }

    // -- integers --

    public static ValueProtos.Value fromBool(boolean value) {
        return value ? TRUE : FALSE;
    }

    public static ValueProtos.Value fromInt8(byte value) {
        return ValueProtos.Value.newBuilder().setInt32Value(value).build();
    }

    public static ValueProtos.Value fromUint8(int value) {
        return ValueProtos.Value.newBuilder().setUint32Value(value & 0xFF).build();
    }

    public static ValueProtos.Value fromInt16(short value) {
        return ValueProtos.Value.newBuilder().setInt32Value(value).build();
    }

    public static ValueProtos.Value fromUint16(int value) {
        return ValueProtos.Value.newBuilder().setUint32Value(value & 0xFFFF).build();
    }

    public static ValueProtos.Value fromInt32(int value) {
        return ValueProtos.Value.newBuilder().setInt32Value(value).build();
    }

    public static ValueProtos.Value fromUint32(long value) {
        return ValueProtos.Value.newBuilder().setUint32Value((int) (value & 0xFFFFFFFFL)).build();
    }

    public static ValueProtos.Value fromInt64(long value) {
        return ValueProtos.Value.newBuilder().setInt64Value(value).build();
    }

    public static ValueProtos.Value fromUint64(long value) {
        return ValueProtos.Value.newBuilder().setUint64Value(value).build();
    }

    // - float32, float64 -

    public static ValueProtos.Value fromFloat(float value) {
        return ValueProtos.Value.newBuilder().setFloatValue(value).build();
    }

    public static ValueProtos.Value fromDouble(double value) {
        return ValueProtos.Value.newBuilder().setDoubleValue(value).build();
    }

    // - string -

    public static ValueProtos.Value fromBytes(ByteString value) {
        return ValueProtos.Value.newBuilder().setBytesValue(value).build();
    }

    public static ValueProtos.Value fromBytes(byte[] value) {
        return ProtoValue.fromBytes(ByteString.copyFrom(value));
    }

    /*
     * will not copy given array
     */
    public static ValueProtos.Value fromBytesOwn(byte[] value) {
        return ProtoValue.fromBytes(UnsafeByteOperations.unsafeWrap(value));
    }

    public static ValueProtos.Value fromStringAsBytes(String value, Charset charset) {
        return ProtoValue.fromBytes(UnsafeByteOperations.unsafeWrap(charset.encode(value)));
    }

    public static byte[] toBytes(ValueProtos.Value value) {
        return value.getBytesValue().toByteArray();
    }

    public static String toBytesAsString(ValueProtos.Value value, Charset charset) {
        return value.getBytesValue().toString(charset);
    }

    // - utf8 -

    public static ValueProtos.Value fromText(String value) {
        return ValueProtos.Value.newBuilder().setTextValue(value).build();
    }

    public static String toText(ValueProtos.Value value) {
        return value.getTextValue();
    }

    // - yson -

    public static ValueProtos.Value fromYson(byte[] value) {
        return ProtoValue.fromBytes(ByteString.copyFrom(value));
    }

    /*
     * will not copy given array
     */
    public static ValueProtos.Value fromOwnYson(byte[] value) {
        return ProtoValue.fromBytes(UnsafeByteOperations.unsafeWrap(value));
    }

    public static byte[] toYson(ValueProtos.Value value) {
        return value.getBytesValue().toByteArray();
    }

    // - json -

    public static ValueProtos.Value fromJson(String value) {
        return fromText(value);
    }

    public static String toJson(ValueProtos.Value value) {
        return value.getTextValue();
    }

    public static ValueProtos.Value fromJsonDocument(String value) {
        return fromText(value);
    }

    public static String toJsonDocument(ValueProtos.Value value) {
        return value.getTextValue();
    }

    // - uuid -

    public static ValueProtos.Value fromUuid(long high, long low) {
        return ValueProtos.Value.newBuilder().setHigh128(high).setLow128(low).build();
    }

    public static ValueProtos.Value fromUuid(UUID uuid) {
        long msb = uuid.getMostSignificantBits();
        long lsb = uuid.getLeastSignificantBits();

        long timeLow = (msb & 0xffffffff00000000L) >>> 32;
        long timeMid = (msb & 0x00000000ffff0000L) << 16;
        long timeHighAndVersion = (msb & 0x000000000000ffffL) << 48;

        long low = timeLow | timeMid | timeHighAndVersion;
        long high = LittleEndian.bswap(lsb);

        return ProtoValue.fromUuid(high, low);
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

    public static ValueProtos.Value fromInterval(long micros) {
        return ValueProtos.Value.newBuilder().setInt64Value(micros).build();
    }

    public static ValueProtos.Value fromInterval(Duration value) {
        return ProtoValue.fromInterval(TimeUnit.NANOSECONDS.toMicros(value.toNanos()));
    }

    public static Duration toInterval(ValueProtos.Value value) {
        return Duration.ofNanos(TimeUnit.MICROSECONDS.toNanos(value.getInt64Value()));
    }

    // - date -

    public static ValueProtos.Value fromDate(long daysSinceEpoch) {
        int daysInt = (int) (daysSinceEpoch & 0xffffffffL);
        return ValueProtos.Value.newBuilder().setUint32Value(daysInt).build();
    }

    public static ValueProtos.Value fromDate(LocalDate value) {
        return ProtoValue.fromDate(value.toEpochDay());
    }

    public static ValueProtos.Value fromDate(Instant value) {
        return ProtoValue.fromDate(TimeUnit.SECONDS.toDays(value.getEpochSecond()));
    }

    public static LocalDate toDate(long daysSinceEpoch) {
        return LocalDate.ofEpochDay(daysSinceEpoch);
    }

    public static LocalDate toDate(ValueProtos.Value value) {
        return toDate(Integer.toUnsignedLong(value.getUint32Value()));
    }

    // - datetime -

    public static ValueProtos.Value fromDatetime(long secondsSinceEpoch) {
        int secondsInt = (int) (secondsSinceEpoch & 0xffffffffL);
        return ValueProtos.Value.newBuilder().setUint32Value(secondsInt).build();
    }

    public static ValueProtos.Value fromDatetime(Instant value) {
        return ProtoValue.fromDatetime(value.getEpochSecond());
    }

    public static ValueProtos.Value fromDatetime(LocalDateTime value) {
        return ProtoValue.fromDatetime(value.toEpochSecond(ZoneOffset.UTC));
    }

    public static LocalDateTime toDatetime(long secondsSinceEpoch) {
        return LocalDateTime.ofEpochSecond(secondsSinceEpoch, 0, ZoneOffset.UTC);
    }

    public static LocalDateTime toDatetime(ValueProtos.Value value) {
        return toDatetime(Integer.toUnsignedLong(value.getUint32Value()));
    }

    // - timestamp -

    public static ValueProtos.Value fromTimestamp(long microsSinceEpoch) {
        return ValueProtos.Value.newBuilder().setUint64Value(microsSinceEpoch).build();
    }

    public static ValueProtos.Value fromTimestamp(Instant value) {
        long micros = TimeUnit.SECONDS.toMicros(value.getEpochSecond()) +
            TimeUnit.NANOSECONDS.toMicros(value.getNano());
        return ProtoValue.fromTimestamp(micros);
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

    public static ValueProtos.Value fromTzDate(String text) {
        return fromText(text);
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

    public static ValueProtos.Value fromTzDatetime(String text) {
        return fromText(text);
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

    public static ValueProtos.Value fromDecimal(long high, long low) {
        return ValueProtos.Value.newBuilder()
            .setHigh128(high)
            .setLow128(low)
            .build();
    }

    public static DecimalValue toDecimal(ValueProtos.Type type, ValueProtos.Value value) {
        ValueProtos.DecimalType dt = type.getDecimalType();
        DecimalType decimalType = DecimalType.of(dt.getPrecision(), dt.getScale());
        return decimalType.newValue(value.getHigh128(), value.getLow128());
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
        ValueProtos.Value key2, ValueProtos.Value value2) {
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
        ValueProtos.Value key3, ValueProtos.Value value3) {
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

    @SuppressWarnings("checkstyle:ParameterNumber")
    public static ValueProtos.Value dict(
        ValueProtos.Value key1, ValueProtos.Value value1,
        ValueProtos.Value key2, ValueProtos.Value value2,
        ValueProtos.Value key3, ValueProtos.Value value3,
        ValueProtos.Value key4, ValueProtos.Value value4) {
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

    @SuppressWarnings("checkstyle:ParameterNumber")
    public static ValueProtos.Value dict(
        ValueProtos.Value key1, ValueProtos.Value value1,
        ValueProtos.Value key2, ValueProtos.Value value2,
        ValueProtos.Value key3, ValueProtos.Value value3,
        ValueProtos.Value key4, ValueProtos.Value value4,
        ValueProtos.Value key5, ValueProtos.Value value5) {
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
        ValueProtos.Value item4) {
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
        ValueProtos.Value item5) {
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
        ValueProtos.Value member3) {
        return list(member1, member2, member3);
    }

    public static ValueProtos.Value struct(
        ValueProtos.Value member1,
        ValueProtos.Value member2,
        ValueProtos.Value member3,
        ValueProtos.Value member4) {
        return list(member1, member2, member3, member4);
    }

    public static ValueProtos.Value struct(
        ValueProtos.Value member1,
        ValueProtos.Value member2,
        ValueProtos.Value member3,
        ValueProtos.Value member4,
        ValueProtos.Value member5) {
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
        ValueProtos.Value member3) {
        return list(member1, member2, member3);
    }

    public static ValueProtos.Value tuple(
        ValueProtos.Value member1,
        ValueProtos.Value member2,
        ValueProtos.Value member3,
        ValueProtos.Value member4) {
        return list(member1, member2, member3, member4);
    }

    public static ValueProtos.Value tuple(
        ValueProtos.Value member1,
        ValueProtos.Value member2,
        ValueProtos.Value member3,
        ValueProtos.Value member4,
        ValueProtos.Value member5) {
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

    public static Value<?> fromPb(Type type, ValueProtos.Value value) {
        switch (type.getKind()) {
            case PRIMITIVE:
                return primitiveFromPb((PrimitiveType) type, value);

            case DECIMAL: {
                DecimalType decimalType = (DecimalType) type;
                return decimalType.newValue(value.getHigh128(), value.getLow128());
            }

            case DICT: {
                DictType dictType = (DictType) type;

                if (value.getPairsCount() == 0) {
                    return dictType.emptyValue();
                }

                HashMap<Value<?>, Value<?>> items = new HashMap<>(value.getPairsCount());
                for (int i = 0; i < value.getPairsCount(); i++) {
                    ValueProtos.ValuePair pair = value.getPairs(i);
                    items.put(
                        fromPb(dictType.getKeyType(), pair.getKey()),
                        fromPb(dictType.getValueType(), pair.getPayload()));
                }
                return dictType.newValueOwn(items);
            }

            case LIST: {
                ListType listType = (ListType) type;

                if (value.getItemsCount() == 0) {
                    return listType.emptyValue();
                }

                Value<?>[] items = new Value<?>[value.getItemsCount()];
                for (int i = 0; i < value.getItemsCount(); i++) {
                    items[i] = fromPb(listType.getItemType(), value.getItems(i));
                }
                return listType.newValueOwn(items);
            }

            case OPTIONAL: {
                OptionalType optionalType = (OptionalType) type;

                switch (value.getValueCase()) {
                    case NULL_FLAG_VALUE:
                        return optionalType.emptyValue();
                    case NESTED_VALUE: {
                        Value<?> itemValue = fromPb(optionalType.getItemType(), value.getNestedValue());
                        return optionalType.newValue(itemValue);
                    }
                    default: {
                        Value<?> itemValue = fromPb(optionalType.getItemType(), value);
                        return optionalType.newValue(itemValue);
                    }
                }
            }

            case STRUCT: {
                StructType structType = (StructType) type;

                if (value.getItemsCount() == 1) {
                    Value<?> memberValue = fromPb(structType.getMemberType(0), value.getItems(0));
                    return structType.newValueUnsafe(memberValue);
                }

                Value<?>[] members = new Value<?>[value.getItemsCount()];
                for (int i = 0; i < value.getItemsCount(); i++) {
                    members[i] = fromPb(structType.getMemberType(i), value.getItems(i));
                }
                return structType.newValueUnsafe(members);
            }

            case TUPLE: {
                TupleType tupleType = (TupleType) type;

                if (value.getItemsCount() == 0) {
                    return TupleValue.empty();
                }

                Value<?>[] items = new Value<?>[value.getItemsCount()];
                for (int i = 0; i < value.getItemsCount(); i++) {
                    items[i] = fromPb(tupleType.getElementType(i), value.getItems(i));
                }
                return tupleType.newValueOwn(items);
            }

            case VARIANT: {
                VariantType variantType = (VariantType) type;
                Type itemType = variantType.getItemType(value.getVariantIndex());
                ValueProtos.Value itemValue = value.getNestedValue();
                return variantType.newValue(fromPb(itemType, itemValue), value.getVariantIndex());
            }

            case VOID:
                return VoidValue.of();

            default:
                throw new IllegalStateException("unknown type kind: " + type.getKind());
        }
    }

    private static PrimitiveValue primitiveFromPb(PrimitiveType primitiveType, ValueProtos.Value value) {
        switch (primitiveType) {
            case Bool: return PrimitiveValue.newBool(value.getBoolValue());
            case Int8: return PrimitiveValue.newInt8((byte) value.getInt32Value());
            case Uint8: return PrimitiveValue.newUint8(value.getUint32Value());
            case Int16: return PrimitiveValue.newInt16((short) value.getInt32Value());
            case Uint16: return PrimitiveValue.newUint16(value.getUint32Value());
            case Int32: return PrimitiveValue.newInt32(value.getInt32Value());
            case Uint32: return PrimitiveValue.newUint32(value.getUint32Value());
            case Int64: return PrimitiveValue.newInt64(value.getInt64Value());
            case Uint64: return PrimitiveValue.newUint64(value.getUint64Value());
            case Float: return PrimitiveValue.newFloat(value.getFloatValue());
            case Double: return PrimitiveValue.newDouble(value.getDoubleValue());
            case Bytes: return PrimitiveValue.newBytes(value.getBytesValue());
            case Text: return PrimitiveValue.newText(value.getTextValue());
            case Yson: return PrimitiveValue.newYson(value.getBytesValue());
            case Json: return PrimitiveValue.newJson(value.getTextValue());
            case JsonDocument: return PrimitiveValue.newJsonDocument(value.getTextValue());
            case Uuid: return PrimitiveValue.newUuid(value.getHigh128(), value.getLow128());
            case Date: return PrimitiveValue.newDate(Integer.toUnsignedLong(value.getUint32Value()));
            case Datetime: return PrimitiveValue.newDatetime(Integer.toUnsignedLong(value.getUint32Value()));
            case Timestamp: return PrimitiveValue.newTimestamp(value.getUint64Value());
            case Interval: return PrimitiveValue.newInterval(value.getInt64Value());
            case TzDate: return PrimitiveValue.newTzDate(toTzDate(value));
            case TzDatetime: return PrimitiveValue.newTzDatetime(toTzDatetime(value));
            case TzTimestamp: return PrimitiveValue.newTzTimestamp(toTzTimestamp(value));
            default:
                throw new IllegalStateException("unknown PrimitiveType: " + primitiveType);
        }
    }

    public static ValueProtos.TypedValue toTypedValue(Value<?> p) {
        return ValueProtos.TypedValue.newBuilder()
            .setType(p.getType().toPb())
            .setValue(p.toPb())
            .build();
    }
}
