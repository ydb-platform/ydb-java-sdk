package tech.ydb.table.values.proto;


import java.util.Arrays;

import com.google.protobuf.NullValue;

import tech.ydb.proto.ValueProtos;
import tech.ydb.proto.ValueProtos.Type.PrimitiveTypeId;
import tech.ydb.table.values.DecimalType;
import tech.ydb.table.values.DictType;
import tech.ydb.table.values.ListType;
import tech.ydb.table.values.NullType;
import tech.ydb.table.values.OptionalType;
import tech.ydb.table.values.PgType;
import tech.ydb.table.values.PrimitiveType;
import tech.ydb.table.values.StructType;
import tech.ydb.table.values.TupleType;
import tech.ydb.table.values.Type;
import tech.ydb.table.values.VariantType;
import tech.ydb.table.values.VoidType;

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

    private static final ValueProtos.Type NULL = ValueProtos.Type.newBuilder()
        .setNullType(NullValue.NULL_VALUE)
        .build();

    private static final ValueProtos.Type BOOL = primitiveType(PrimitiveTypeId.BOOL);
    private static final ValueProtos.Type INT_8 = primitiveType(PrimitiveTypeId.INT8);
    private static final ValueProtos.Type UINT_8 = primitiveType(PrimitiveTypeId.UINT8);
    private static final ValueProtos.Type INT_16 = primitiveType(PrimitiveTypeId.INT16);
    private static final ValueProtos.Type UINT_16 = primitiveType(PrimitiveTypeId.UINT16);
    private static final ValueProtos.Type INT_32 = primitiveType(PrimitiveTypeId.INT32);
    private static final ValueProtos.Type UINT_32 = primitiveType(PrimitiveTypeId.UINT32);
    private static final ValueProtos.Type INT_64 = primitiveType(PrimitiveTypeId.INT64);
    private static final ValueProtos.Type UINT_64 = primitiveType(PrimitiveTypeId.UINT64);
    private static final ValueProtos.Type FLOAT = primitiveType(PrimitiveTypeId.FLOAT);
    private static final ValueProtos.Type DOUBLE = primitiveType(PrimitiveTypeId.DOUBLE);
    private static final ValueProtos.Type BYTES = primitiveType(PrimitiveTypeId.STRING);
    private static final ValueProtos.Type TEXT = primitiveType(PrimitiveTypeId.UTF8);
    private static final ValueProtos.Type YSON = primitiveType(PrimitiveTypeId.YSON);
    private static final ValueProtos.Type JSON = primitiveType(PrimitiveTypeId.JSON);
    private static final ValueProtos.Type UUID = primitiveType(PrimitiveTypeId.UUID);
    private static final ValueProtos.Type DATE = primitiveType(PrimitiveTypeId.DATE);
    private static final ValueProtos.Type DATETIME = primitiveType(PrimitiveTypeId.DATETIME);
    private static final ValueProtos.Type TIMESTAMP = primitiveType(PrimitiveTypeId.TIMESTAMP);
    private static final ValueProtos.Type INTERVAL = primitiveType(PrimitiveTypeId.INTERVAL);
    private static final ValueProtos.Type TZ_DATE = primitiveType(PrimitiveTypeId.TZ_DATE);
    private static final ValueProtos.Type TZ_DATETIME = primitiveType(PrimitiveTypeId.TZ_DATETIME);
    private static final ValueProtos.Type TZ_TIMESTAMP = primitiveType(PrimitiveTypeId.TZ_TIMESTAMP);
    private static final ValueProtos.Type JSON_DOCUMENT = primitiveType(PrimitiveTypeId.JSON_DOCUMENT);
    private static final ValueProtos.Type DYNUMBER = primitiveType(PrimitiveTypeId.DYNUMBER);
    private static final ValueProtos.Type DATE32 = primitiveType(PrimitiveTypeId.DATE32);
    private static final ValueProtos.Type DATETIME64 = primitiveType(PrimitiveTypeId.DATETIME64);
    private static final ValueProtos.Type TIMESTAMP64 = primitiveType(PrimitiveTypeId.TIMESTAMP64);
    private static final ValueProtos.Type INTERVAL64 = primitiveType(PrimitiveTypeId.INTERVAL64);

    private ProtoType() { }

    private static ValueProtos.Type primitiveType(PrimitiveTypeId id) {
        ValueProtos.Type.Builder builder = ValueProtos.Type.newBuilder();
        builder.setTypeId(id);
        return builder.build();
    }

    public static ValueProtos.Type getBool() {
        return BOOL;
    }

    public static ValueProtos.Type getInt8() {
        return INT_8;
    }

    public static ValueProtos.Type getUint8() {
        return UINT_8;
    }

    public static ValueProtos.Type getInt16() {
        return INT_16;
    }

    public static ValueProtos.Type getUint16() {
        return UINT_16;
    }

    public static ValueProtos.Type getInt32() {
        return INT_32;
    }

    public static ValueProtos.Type getUint32() {
        return UINT_32;
    }

    public static ValueProtos.Type getInt64() {
        return INT_64;
    }

    public static ValueProtos.Type getUint64() {
        return UINT_64;
    }

    public static ValueProtos.Type getFloat() {
        return FLOAT;
    }

    public static ValueProtos.Type getDouble() {
        return DOUBLE;
    }

    public static ValueProtos.Type getBytes() {
        return BYTES;
    }

    public static ValueProtos.Type getText() {
        return TEXT;
    }

    public static ValueProtos.Type getYson() {
        return YSON;
    }

    public static ValueProtos.Type getJson() {
        return JSON;
    }

    public static ValueProtos.Type getUuid() {
        return UUID;
    }

    public static ValueProtos.Type getDate() {
        return DATE;
    }

    public static ValueProtos.Type getDatetime() {
        return DATETIME;
    }

    public static ValueProtos.Type getTimestamp() {
        return TIMESTAMP;
    }

    public static ValueProtos.Type getInterval() {
        return INTERVAL;
    }

    public static ValueProtos.Type getTzDate() {
        return TZ_DATE;
    }

    public static ValueProtos.Type getTzDatetime() {
        return TZ_DATETIME;
    }

    public static ValueProtos.Type getTzTimestamp() {
        return TZ_TIMESTAMP;
    }

    public static ValueProtos.Type getJsonDocument() {
        return JSON_DOCUMENT;
    }

    public static ValueProtos.Type getDyNumber() {
        return DYNUMBER;
    }

    public static ValueProtos.Type getDate32() {
        return DATE32;
    }

    public static ValueProtos.Type getDatetime64() {
        return DATETIME64;
    }

    public static ValueProtos.Type getTimestamp64() {
        return TIMESTAMP64;
    }

    public static ValueProtos.Type getInterval64() {
        return INTERVAL64;
    }

    public static ValueProtos.Type getDecimal(int precision, int scale) {
        ValueProtos.Type.Builder builder = ValueProtos.Type.newBuilder();
        builder.getDecimalTypeBuilder()
            .setPrecision(precision)
            .setScale(scale);
        return builder.build();
    }

    public static ValueProtos.Type getDict(ValueProtos.Type keyType, ValueProtos.Type valueType) {
        ValueProtos.Type.Builder builder = ValueProtos.Type.newBuilder();
        builder.getDictTypeBuilder()
            .setKey(keyType)
            .setPayload(valueType);
        return builder.build();
    }

    public static ValueProtos.Type getList(ValueProtos.Type itemType) {
        ValueProtos.Type.Builder builder = ValueProtos.Type.newBuilder();
        builder.getListTypeBuilder().setItem(itemType);
        return builder.build();
    }

    public static ValueProtos.Type getOptional(ValueProtos.Type itemType) {
        ValueProtos.Type.Builder builder = ValueProtos.Type.newBuilder();
        builder.getOptionalTypeBuilder().setItem(itemType);
        return builder.build();
    }

    public static ValueProtos.Type getStruct(String memberName, ValueProtos.Type memberType) {
        ValueProtos.Type.Builder builder = ValueProtos.Type.newBuilder();
        ValueProtos.StructType.Builder structType = builder.getStructTypeBuilder();
        structType.addMembersBuilder()
            .setName(memberName)
            .setType(memberType);
        return builder.build();
    }

    public static ValueProtos.Type getStruct(
        String member1Name, ValueProtos.Type member1Type,
        String member2Name, ValueProtos.Type member2Type) {
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

    public static ValueProtos.Type getStruct(
        String member1Name, ValueProtos.Type member1Type,
        String member2Name, ValueProtos.Type member2Type,
        String member3Name, ValueProtos.Type member3Type) {
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

    @SuppressWarnings("checkstyle:ParameterNumber")
    public static ValueProtos.Type getStruct(
        String member1Name, ValueProtos.Type member1Type,
        String member2Name, ValueProtos.Type member2Type,
        String member3Name, ValueProtos.Type member3Type,
        String member4Name, ValueProtos.Type member4Type) {
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

    @SuppressWarnings("checkstyle:ParameterNumber")
    public static ValueProtos.Type getStruct(
        String member1Name, ValueProtos.Type member1Type,
        String member2Name, ValueProtos.Type member2Type,
        String member3Name, ValueProtos.Type member3Type,
        String member4Name, ValueProtos.Type member4Type,
        String member5Name, ValueProtos.Type member5Type) {
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

    public static ValueProtos.Type getStruct(ValueProtos.StructMember firstMember,
            ValueProtos.StructMember... members) {
        ValueProtos.Type.Builder builder = ValueProtos.Type.newBuilder();
        ValueProtos.StructType.Builder structType = builder.getStructTypeBuilder();
        structType.addMembers(firstMember);
        for (ValueProtos.StructMember member : members) {
            structType.addMembers(member);
        }
        return builder.build();
    }

    public static ValueProtos.Type getTuple() {
        return EMPTY_TUPLE;
    }

    public static ValueProtos.Type getTuple(ValueProtos.Type... elementTypes) {
        ValueProtos.Type.Builder builder = ValueProtos.Type.newBuilder();
        ValueProtos.TupleType.Builder tupleType = builder.getTupleTypeBuilder();
        for (ValueProtos.Type elementType : elementTypes) {
            tupleType.addElements(elementType);
        }
        return builder.build();
    }

    public static ValueProtos.Type getVariant(ValueProtos.StructType structType) {
        ValueProtos.Type.Builder builder = ValueProtos.Type.newBuilder();
        builder.getVariantTypeBuilder().setStructItems(structType);
        return builder.build();
    }

    public static ValueProtos.Type getVariant(ValueProtos.TupleType tupleType) {
        ValueProtos.Type.Builder builder = ValueProtos.Type.newBuilder();
        builder.getVariantTypeBuilder().setTupleItems(tupleType);
        return builder.build();
    }

    public static ValueProtos.Type getVariant(ValueProtos.Type... elementTypes) {
        ValueProtos.Type.Builder builder = ValueProtos.Type.newBuilder();
        builder.getVariantTypeBuilder().setTupleItems(
            ValueProtos.TupleType.newBuilder()
            .addAllElements(Arrays.asList(elementTypes))
            .build());
        return builder.build();
    }

    public static ValueProtos.Type getVoid() {
        return VOID;
    }

    public static ValueProtos.Type getNull() {
        return NULL;
    }

    public static ValueProtos.Type getPgType(int oid, int typlen, int typmod) {
        return ValueProtos.Type.newBuilder()
                .setPgType(ValueProtos.PgType.newBuilder()
                        .setOid(oid)
                        .setTyplen(typlen)
                        .setTypmod(typmod)
                        .build()
                ).build();
    }

    public static Type fromPb(ValueProtos.Type type) {
        switch (type.getTypeCase()) {
            case TYPE_ID:
                return primitiveTypeFromPb(type);

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

                return StructType.ofOwn(names, types);
            }

            case TUPLE_TYPE: {
                ValueProtos.TupleType tupleType = type.getTupleType();
                Type[] elements = new Type[tupleType.getElementsCount()];

                for (int i = 0; i < tupleType.getElementsCount(); i++) {
                    elements[i] = fromPb(tupleType.getElements(i));
                }

                return TupleType.ofOwn(elements);
            }

            case VARIANT_TYPE: {
                ValueProtos.VariantType variantType = type.getVariantType();
                if (variantType.hasStructItems()) {
                    ValueProtos.StructType structItems = variantType.getStructItems();
                    Type[] items = new Type[structItems.getMembersCount()];
                    for (int i = 0; i < structItems.getMembersCount(); i++) {
                        items[i] = fromPb(structItems.getMembers(i).getType());
                    }
                    return VariantType.ofOwn(items);
                } else if (variantType.hasTupleItems()) {
                    ValueProtos.TupleType tupleItems = variantType.getTupleItems();
                    Type[] items = new Type[tupleItems.getElementsCount()];
                    for (int i = 0; i < tupleItems.getElementsCount(); i++) {
                        items[i] = fromPb(tupleItems.getElements(i));
                    }
                    return VariantType.ofOwn(items);
                }
                throw new IllegalStateException("empty variant type");
            }

            case VOID_TYPE:
                return VoidType.of();

            case NULL_TYPE:
                return NullType.of();

            case PG_TYPE:
                ValueProtos.PgType pgType = type.getPgType();
                return PgType.of(pgType.getOid(), pgType.getTyplen(), pgType.getTypmod());

            default:
                throw new IllegalStateException("unknown type: " + type.getTypeCase());
        }
    }


    private static PrimitiveType primitiveTypeFromPb(ValueProtos.Type dataType) {
        switch (dataType.getTypeId()) {
            case BOOL: return PrimitiveType.Bool;
            case INT8: return PrimitiveType.Int8;
            case UINT8: return PrimitiveType.Uint8;
            case INT16: return PrimitiveType.Int16;
            case UINT16: return PrimitiveType.Uint16;
            case INT32: return PrimitiveType.Int32;
            case UINT32: return PrimitiveType.Uint32;
            case INT64: return PrimitiveType.Int64;
            case UINT64: return PrimitiveType.Uint64;
            case FLOAT: return PrimitiveType.Float;
            case DOUBLE: return PrimitiveType.Double;
            case STRING: return PrimitiveType.Bytes;
            case UTF8: return PrimitiveType.Text;
            case YSON: return PrimitiveType.Yson;
            case JSON: return PrimitiveType.Json;
            case UUID: return PrimitiveType.Uuid;
            case DATE: return PrimitiveType.Date;
            case DATETIME: return PrimitiveType.Datetime;
            case TIMESTAMP: return PrimitiveType.Timestamp;
            case INTERVAL: return PrimitiveType.Interval;
            case TZ_DATE: return PrimitiveType.TzDate;
            case TZ_DATETIME: return PrimitiveType.TzDatetime;
            case TZ_TIMESTAMP: return PrimitiveType.TzTimestamp;
            case JSON_DOCUMENT: return PrimitiveType.JsonDocument;
            case DYNUMBER: return PrimitiveType.DyNumber;
            default:
                throw new IllegalStateException("unknown PrimitiveType: " + dataType.getTypeId());
        }
    }

    public static String toString(ValueProtos.Type type) {
        // TODO: implement it
        return type.toString();
    }
}
