package ru.yandex.ydb.table.types.proto;


import java.util.Arrays;

import com.google.protobuf.NullValue;

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
import ru.yandex.ydb.table.types.VoidType;


/**
 * @author Sergey Polovko
 */
public class ProtoType {

    private static final ValueProtos.Type EMPTY_TUPLE = ValueProtos.Type.newBuilder()
            .setTupleType(ValueProtos.TupleType.newBuilder().build())
            .build();

    private static final ValueProtos.Type VOID = ValueProtos.Type.newBuilder()
        .setVoidType(NullValue.NULL_VALUE)
        .build();

    private static final ValueProtos.Type BOOL = dataType(ValueProtos.Type.PrimitiveTypeId.BOOL);
    private static final ValueProtos.Type INT_8 = dataType(ValueProtos.Type.PrimitiveTypeId.INT8);
    private static final ValueProtos.Type UINT_8 = dataType(ValueProtos.Type.PrimitiveTypeId.UINT8);
    private static final ValueProtos.Type INT_16 = dataType(ValueProtos.Type.PrimitiveTypeId.INT16);
    private static final ValueProtos.Type UINT_16 = dataType(ValueProtos.Type.PrimitiveTypeId.UINT16);
    private static final ValueProtos.Type INT_32 = dataType(ValueProtos.Type.PrimitiveTypeId.INT32);
    private static final ValueProtos.Type UINT_32 = dataType(ValueProtos.Type.PrimitiveTypeId.UINT32);
    private static final ValueProtos.Type INT_64 = dataType(ValueProtos.Type.PrimitiveTypeId.INT64);
    private static final ValueProtos.Type UINT_64 = dataType(ValueProtos.Type.PrimitiveTypeId.UINT64);
    private static final ValueProtos.Type FLOAT_32 = dataType(ValueProtos.Type.PrimitiveTypeId.FLOAT);
    private static final ValueProtos.Type FLOAT_64 = dataType(ValueProtos.Type.PrimitiveTypeId.DOUBLE);
    private static final ValueProtos.Type STRING = dataType(ValueProtos.Type.PrimitiveTypeId.STRING);
    private static final ValueProtos.Type UTF_8 = dataType(ValueProtos.Type.PrimitiveTypeId.UTF8);
    private static final ValueProtos.Type YSON = dataType(ValueProtos.Type.PrimitiveTypeId.YSON);
    private static final ValueProtos.Type JSON = dataType(ValueProtos.Type.PrimitiveTypeId.JSON);
    private static final ValueProtos.Type UUID = dataType(ValueProtos.Type.PrimitiveTypeId.UUID);
    private static final ValueProtos.Type DATE = dataType(ValueProtos.Type.PrimitiveTypeId.DATE);
    private static final ValueProtos.Type DATETIME = dataType(ValueProtos.Type.PrimitiveTypeId.DATETIME);
    private static final ValueProtos.Type TIMESTAMP = dataType(ValueProtos.Type.PrimitiveTypeId.TIMESTAMP);
    private static final ValueProtos.Type INTERVAL = dataType(ValueProtos.Type.PrimitiveTypeId.INTERVAL);
    private static final ValueProtos.Type TZ_DATE = dataType(ValueProtos.Type.PrimitiveTypeId.TZ_DATE);
    private static final ValueProtos.Type TZ_DATETIME = dataType(ValueProtos.Type.PrimitiveTypeId.TZ_DATETIME);
    private static final ValueProtos.Type TZ_TIMESTAMP = dataType(ValueProtos.Type.PrimitiveTypeId.TZ_TIMESTAMP);

    private static ValueProtos.Type dataType(ValueProtos.Type.PrimitiveTypeId id) {
        ValueProtos.Type.Builder builder = ValueProtos.Type.newBuilder();
        builder.setTypeId(id);
        return builder.build();
    }

    public static ValueProtos.Type bool() { return BOOL; }
    public static ValueProtos.Type int8() { return INT_8; }
    public static ValueProtos.Type uint8() { return UINT_8; }
    public static ValueProtos.Type int16() { return INT_16; }
    public static ValueProtos.Type uint16() { return UINT_16; }
    public static ValueProtos.Type int32() { return INT_32; }
    public static ValueProtos.Type uint32() { return UINT_32; }
    public static ValueProtos.Type int64() { return INT_64; }
    public static ValueProtos.Type uint64() { return UINT_64; }
    public static ValueProtos.Type float32() { return FLOAT_32; }
    public static ValueProtos.Type float64() { return FLOAT_64; }
    public static ValueProtos.Type string() { return STRING; }
    public static ValueProtos.Type utf8() { return UTF_8; }
    public static ValueProtos.Type yson() { return YSON; }
    public static ValueProtos.Type json() { return JSON; }
    public static ValueProtos.Type uuid() { return UUID; }
    public static ValueProtos.Type date() { return DATE; }
    public static ValueProtos.Type datetime() { return DATETIME; }
    public static ValueProtos.Type timestamp() { return TIMESTAMP; }
    public static ValueProtos.Type interval() { return INTERVAL; }
    public static ValueProtos.Type tzDate() { return TZ_DATE; }
    public static ValueProtos.Type tzDatetime() { return TZ_DATETIME; }
    public static ValueProtos.Type tzTimestamp() { return TZ_TIMESTAMP; }

    public static ValueProtos.Type decimal(int precision, int scale) {
        ValueProtos.Type.Builder builder = ValueProtos.Type.newBuilder();
        builder.getDecimalTypeBuilder()
            .setPrecision(precision)
            .setScale(scale);
        return builder.build();
    }

    public static ValueProtos.Type dict(ValueProtos.Type keyType, ValueProtos.Type valueType) {
        ValueProtos.Type.Builder builder = ValueProtos.Type.newBuilder();
        builder.getDictTypeBuilder()
            .setKey(keyType)
            .setPayload(valueType);
        return builder.build();
    }

    public static ValueProtos.Type list(ValueProtos.Type itemType) {
        ValueProtos.Type.Builder builder = ValueProtos.Type.newBuilder();
        builder.getListTypeBuilder().setItem(itemType);
        return builder.build();
    }

    public static ValueProtos.Type optional(ValueProtos.Type itemType) {
        ValueProtos.Type.Builder builder = ValueProtos.Type.newBuilder();
        builder.getOptionalTypeBuilder().setItem(itemType);
        return builder.build();
    }

    public static ValueProtos.Type struct(String memberName, ValueProtos.Type memberType) {
        ValueProtos.Type.Builder builder = ValueProtos.Type.newBuilder();
        ValueProtos.StructType.Builder structType = builder.getStructTypeBuilder();
        structType.addMembersBuilder()
            .setName(memberName)
            .setType(memberType);
        return builder.build();
    }

    public static ValueProtos.Type struct(
        String member1Name, ValueProtos.Type member1Type,
        String member2Name, ValueProtos.Type member2Type)
    {
        ValueProtos.Type.Builder builder = ValueProtos.Type.newBuilder();
        ValueProtos.StructType.Builder structType = builder.getStructTypeBuilder();
        structType.addMembersBuilder()
            .setName(member1Name)
            .setType(member1Type);
        structType.addMembersBuilder()
            .setName(member2Name)
            .setType(member2Type);
        return builder.build();
    }

    public static ValueProtos.Type struct(
        String member1Name, ValueProtos.Type member1Type,
        String member2Name, ValueProtos.Type member2Type,
        String member3Name, ValueProtos.Type member3Type)
    {
        ValueProtos.Type.Builder builder = ValueProtos.Type.newBuilder();
        ValueProtos.StructType.Builder structType = builder.getStructTypeBuilder();
        structType.addMembersBuilder()
            .setName(member1Name)
            .setType(member1Type);
        structType.addMembersBuilder()
            .setName(member2Name)
            .setType(member2Type);
        structType.addMembersBuilder()
            .setName(member3Name)
            .setType(member3Type);
        return builder.build();
    }

    public static ValueProtos.Type struct(
        String member1Name, ValueProtos.Type member1Type,
        String member2Name, ValueProtos.Type member2Type,
        String member3Name, ValueProtos.Type member3Type,
        String member4Name, ValueProtos.Type member4Type)
    {
        ValueProtos.Type.Builder builder = ValueProtos.Type.newBuilder();
        ValueProtos.StructType.Builder structType = builder.getStructTypeBuilder();
        structType.addMembersBuilder()
            .setName(member1Name)
            .setType(member1Type);
        structType.addMembersBuilder()
            .setName(member2Name)
            .setType(member2Type);
        structType.addMembersBuilder()
            .setName(member3Name)
            .setType(member3Type);
        structType.addMembersBuilder()
            .setName(member4Name)
            .setType(member4Type);
        return builder.build();
    }

    public static ValueProtos.Type struct(
        String member1Name, ValueProtos.Type member1Type,
        String member2Name, ValueProtos.Type member2Type,
        String member3Name, ValueProtos.Type member3Type,
        String member4Name, ValueProtos.Type member4Type,
        String member5Name, ValueProtos.Type member5Type)
    {
        ValueProtos.Type.Builder builder = ValueProtos.Type.newBuilder();
        ValueProtos.StructType.Builder structType = builder.getStructTypeBuilder();
        structType.addMembersBuilder()
            .setName(member1Name)
            .setType(member1Type);
        structType.addMembersBuilder()
            .setName(member2Name)
            .setType(member2Type);
        structType.addMembersBuilder()
            .setName(member3Name)
            .setType(member3Type);
        structType.addMembersBuilder()
            .setName(member4Name)
            .setType(member4Type);
        structType.addMembersBuilder()
            .setName(member5Name)
            .setType(member5Type);
        return builder.build();
    }

    public static ValueProtos.Type struct(ValueProtos.StructMember firstMember, ValueProtos.StructMember... members) {
        ValueProtos.Type.Builder builder = ValueProtos.Type.newBuilder();
        ValueProtos.StructType.Builder structType = builder.getStructTypeBuilder();
        structType.addMembers(firstMember);
        for (ValueProtos.StructMember member : members) {
            structType.addMembers(member);
        }
        return builder.build();
    }

    public static ValueProtos.Type tuple() {
        return EMPTY_TUPLE;
    }

    public static ValueProtos.Type tuple(ValueProtos.Type... elementTypes) {
        ValueProtos.Type.Builder builder = ValueProtos.Type.newBuilder();
        ValueProtos.TupleType.Builder tupleType = builder.getTupleTypeBuilder();
        for (ValueProtos.Type elementType : elementTypes) {
            tupleType.addElements(elementType);
        }
        return builder.build();
    }

    public static ValueProtos.Type variant(ValueProtos.StructType structType) {
        ValueProtos.Type.Builder builder = ValueProtos.Type.newBuilder();
        builder.getVariantTypeBuilder().setStructItems(structType);
        return builder.build();
    }

    public static ValueProtos.Type variant(ValueProtos.TupleType tupleType) {
        ValueProtos.Type.Builder builder = ValueProtos.Type.newBuilder();
        builder.getVariantTypeBuilder().setTupleItems(tupleType);
        return builder.build();
    }

    public static ValueProtos.Type variant(ValueProtos.Type... elementTypes) {
        ValueProtos.Type.Builder builder = ValueProtos.Type.newBuilder();
        builder.getVariantTypeBuilder().setTupleItems(
            ValueProtos.TupleType.newBuilder()
            .addAllElements(Arrays.asList(elementTypes))
            .build());
        return builder.build();
    }

    public static ValueProtos.Type voidType() {
        return VOID;
    }

    public static Type fromPb(ValueProtos.Type type) {
        switch (type.getTypeCase()) {
            case TYPE_ID:
                return dataTypeFromPb(type);

            case DECIMAL_TYPE: {
                ValueProtos.DecimalType decimalType = type.getDecimalType();
                return DecimalType.of(decimalType.getPrecision(), decimalType.getScale());
            }

            case DICT_TYPE: {
                ValueProtos.DictType dictType = type.getDictType();
                return DictType.of(fromPb(dictType.getKey()), fromPb(dictType.getPayload()));
            }

            case LIST_TYPE: {
                ValueProtos.ListType listType = type.getListType();
                return ListType.of(fromPb(listType.getItem()));
            }

            case OPTIONAL_TYPE: {
                ValueProtos.OptionalType optionalType = type.getOptionalType();
                return OptionalType.of(fromPb(optionalType.getItem()));
            }

            case STRUCT_TYPE: {
                ValueProtos.StructType structType = type.getStructType();
                String[] names = new String[structType.getMembersCount()];
                Type[] types = new Type[structType.getMembersCount()];

                for (int i = 0; i < structType.getMembersCount(); i++) {
                    ValueProtos.StructMember member = structType.getMembers(i);
                    names[i] = member.getName();
                    types[i] = fromPb(member.getType());
                }

                return StructType.ofOwning(names, types);
            }

            case TUPLE_TYPE: {
                ValueProtos.TupleType tupleType = type.getTupleType();
                Type[] elements = new Type[tupleType.getElementsCount()];

                for (int i = 0; i < tupleType.getElementsCount(); i++) {
                    elements[i] = fromPb(tupleType.getElements(i));
                }

                return TupleType.ofOwning(elements);
            }

            case VARIANT_TYPE: {
                ValueProtos.VariantType variantType = type.getVariantType();
                if (variantType.hasStructItems()) {
                    ValueProtos.StructType structItems = variantType.getStructItems();
                    Type[] items = new Type[structItems.getMembersCount()];
                    for (int i = 0; i < structItems.getMembersCount(); i++) {
                        items[i] = fromPb(structItems.getMembers(i).getType());
                    }
                    return VariantType.ofOwning(items);
                } else if (variantType.hasTupleItems()) {
                    ValueProtos.TupleType tupleItems = variantType.getTupleItems();
                    Type[] items = new Type[tupleItems.getElementsCount()];
                    for (int i = 0; i < tupleItems.getElementsCount(); i++) {
                        items[i] = fromPb(tupleItems.getElements(i));
                    }
                    return VariantType.ofOwning(items);
                }
                throw new IllegalStateException("empty variant type");
            }

            case VOID_TYPE:
                return VoidType.of();
        }

        throw new IllegalStateException("unknown type: " + type.getTypeCase());
    }


    public static ValueProtos.Type toPb(Type type) {
        switch (type.getKind()) {
            case PRIMITIVE:
                return dataTypeToPb((PrimitiveType) type);

            case DECIMAL: {
                DecimalType decimalType = (DecimalType) type;
                return decimal(decimalType.getPrecision(), decimalType.getScale());
            }

            case DICT: {
                DictType dictType = (DictType) type;
                ValueProtos.Type keyType = toPb(dictType.getKeyType());
                ValueProtos.Type valueType = toPb(dictType.getValueType());
                return dict(keyType, valueType);
            }

            case LIST: {
                ListType listType = (ListType) type;
                return list(toPb(listType.getItemType()));
            }

            case OPTIONAL: {
                OptionalType optionalType = (OptionalType) type;
                return optional(toPb(optionalType.getItemType()));
            }

            case STRUCT: {
                StructType structType = (StructType) type;
                ValueProtos.Type.Builder builder = ValueProtos.Type.newBuilder();
                ValueProtos.StructType.Builder pbStructTypeBuilder = builder.getStructTypeBuilder();
                for (int i = 0; i < structType.getMembersCount(); i++) {
                    pbStructTypeBuilder.addMembersBuilder()
                        .setName(structType.getMemberName(i))
                        .setType(toPb(structType.getMemberType(i)));
                }
                return builder.build();
            }

            case TUPLE: {
                TupleType tupleType = (TupleType) type;
                if (tupleType.getElementsCount() == 0) {
                    return tuple();
                }

                ValueProtos.Type.Builder builder = ValueProtos.Type.newBuilder();
                ValueProtos.TupleType.Builder pbTupleType = builder.getTupleTypeBuilder();
                for (int i = 0; i < tupleType.getElementsCount(); i++) {
                    pbTupleType.addElements(toPb(tupleType.getElementType(i)));
                }
                return builder.build();
            }

            case VARIANT: {
                VariantType variantType = (VariantType) type;
                ValueProtos.TupleType.Builder pbTupleBuilder = ValueProtos.TupleType.newBuilder();
                for (int i = 0; i < variantType.getItemsCount(); i++) {
                    pbTupleBuilder.addElements(toPb(variantType.getItemType(i)));
                }
                return variant(pbTupleBuilder.build());
            }

            case VOID:
                return VOID;
        }

        throw new IllegalStateException("unknown type kind: " + type.getKind());
    }

    private static PrimitiveType dataTypeFromPb(ValueProtos.Type dataType) {
        switch (dataType.getTypeId()) {
            case BOOL: return PrimitiveType.bool();
            case INT8: return PrimitiveType.int8();
            case UINT8: return PrimitiveType.uint8();
            case INT16: return PrimitiveType.int16();
            case UINT16: return PrimitiveType.uint16();
            case INT32: return PrimitiveType.int32();
            case UINT32: return PrimitiveType.uint32();
            case INT64: return PrimitiveType.int64();
            case UINT64: return PrimitiveType.uint64();
            case FLOAT: return PrimitiveType.float32();
            case DOUBLE: return PrimitiveType.float64();
            case STRING: return PrimitiveType.string();
            case UTF8: return PrimitiveType.utf8();
            case YSON: return PrimitiveType.yson();
            case JSON: return PrimitiveType.json();
            case UUID: return PrimitiveType.uuid();
            case DATE: return PrimitiveType.date();
            case DATETIME: return PrimitiveType.datetime();
            case TIMESTAMP: return PrimitiveType.timestamp();
            case INTERVAL: return PrimitiveType.interval();
            case TZ_DATE: return PrimitiveType.tzDate();
            case TZ_DATETIME: return PrimitiveType.tzDatetime();
            case TZ_TIMESTAMP: return PrimitiveType.tzTimestamp();
        }

        throw new IllegalStateException("unknown PrimitiveType: " + dataType.getTypeId());
    }

    @SuppressWarnings("Duplicates")
    private static ValueProtos.Type dataTypeToPb(PrimitiveType primitiveType) {
        switch (primitiveType.getId()) {
            case Bool: return BOOL;
            case Int8: return INT_8;
            case Uint8: return UINT_8;
            case Int16: return INT_16;
            case Uint16: return UINT_16;
            case Int32: return INT_32;
            case Uint32: return UINT_32;
            case Int64: return INT_64;
            case Uint64: return UINT_64;
            case Float: return FLOAT_32;
            case Double: return FLOAT_64;
            case String: return STRING;
            case Utf8: return UTF_8;
            case Yson: return YSON;
            case Json: return JSON;
            case Uuid: return UUID;
            case Date: return DATE;
            case Datetime: return DATETIME;
            case Timestamp: return TIMESTAMP;
            case Interval: return INTERVAL;
            case TzDate: return TZ_DATE;
            case TzDatetime: return TZ_DATETIME;
            case TzTimestamp: return TZ_TIMESTAMP;
        }
        throw new IllegalStateException("unknown PrimitiveType: " + primitiveType.getId());
    }

    public static String toString(ValueProtos.Type type) {
        // TODO: implement it
        return type.toString();
    }
}
