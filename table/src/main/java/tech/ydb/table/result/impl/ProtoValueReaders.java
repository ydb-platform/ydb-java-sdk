package tech.ydb.table.result.impl;

import tech.ydb.ValueProtos;
import tech.ydb.table.result.ResultSetReader;
import tech.ydb.table.result.ValueReader;


/**
 * @author Sergey Polovko
 */
public class ProtoValueReaders {
    private ProtoValueReaders() {}

    public static ResultSetReader forResultSet(ValueProtos.ResultSet resultSet) {
        return new ProtoResultSetReader(resultSet);
    }

    public static ValueReader forType(ValueProtos.Type type) {
        return forTypeImpl(type);
    }

    static AbstractValueReader forTypeImpl(ValueProtos.Type type) {
        switch (type.getTypeCase()) {
            case TYPE_ID:
            case DECIMAL_TYPE:
                return new ProtoPrimitiveValueReader(type);
            case OPTIONAL_TYPE:
                return optionalReader(type);
            case TUPLE_TYPE:
                return tupleReader(type);
            case DICT_TYPE:
                return dictReader(type);
            case LIST_TYPE:
                return listReader(type);
            case STRUCT_TYPE:
                return structReader(type);
            case VARIANT_TYPE:
                return variantReader(type);
            case VOID_TYPE:
                return ProtoVoidValueReader.INSTANCE;
            default:
                throw new IllegalStateException("unsupported type: " + type);
        }
    }

    private static AbstractValueReader variantReader(ValueProtos.Type type) {
        final ValueProtos.VariantType variantType = type.getVariantType();
        if (variantType.hasStructItems()) {
            ValueProtos.StructType structItems = variantType.getStructItems();
            AbstractValueReader[] itemReaders = new AbstractValueReader[structItems.getMembersCount()];
            for (int i = 0; i < structItems.getMembersCount(); i++) {
                itemReaders[i] = forTypeImpl(structItems.getMembers(i).getType());
            }
            return new ProtoVariantValueReader(type, itemReaders);
        }
        if (variantType.hasTupleItems()) {
            ValueProtos.TupleType tupleItems = variantType.getTupleItems();
            AbstractValueReader[] itemReaders = new AbstractValueReader[tupleItems.getElementsCount()];
            for (int i = 0; i < tupleItems.getElementsCount(); i++) {
                itemReaders[i] = forTypeImpl(tupleItems.getElements(i));
            }
            return new ProtoVariantValueReader(type, itemReaders);
        }
        throw new IllegalStateException("empty variant type");
    }

    private static AbstractValueReader structReader(ValueProtos.Type type) {
        final ValueProtos.StructType structType = type.getStructType();
        final int membersCount = structType.getMembersCount();

        AbstractValueReader[] memberReaders = new AbstractValueReader[membersCount];
        for (int i = 0; i < membersCount; i++) {
            memberReaders[i] = forTypeImpl(structType.getMembers(i).getType());
        }
        return new ProtoStructValueReader(type, memberReaders);
    }

    private static AbstractValueReader listReader(ValueProtos.Type type) {
        ValueProtos.ListType listType = type.getListType();
        return new ProtoListValueReader(type, forTypeImpl(listType.getItem()));
    }

    private static AbstractValueReader dictReader(ValueProtos.Type type) {
        ValueProtos.DictType dictType = type.getDictType();
        return new ProtoDictValueReader(type,
            forTypeImpl(dictType.getKey()),
            forTypeImpl(dictType.getPayload()));
    }

    private static AbstractValueReader optionalReader(ValueProtos.Type type) {
        ValueProtos.Type itemType = type.getOptionalType().getItem();
        switch (itemType.getTypeCase()) {
            case TYPE_ID:
            case DECIMAL_TYPE:
                return new ProtoPrimitiveValueReader.Optional(type);
            default:
                return new ProtoOptionalValueReader(type, forTypeImpl(itemType));
        }
    }

    private static AbstractValueReader tupleReader(ValueProtos.Type type) {
        final ValueProtos.TupleType tupleType = type.getTupleType();
        final int elementsCount = tupleType.getElementsCount();

        AbstractValueReader[] elementReaders = new AbstractValueReader[elementsCount];
        for (int i = 0; i < elementsCount; i++) {
            elementReaders[i] = forTypeImpl(tupleType.getElements(i));
        }
        return new ProtoTupleValueReader(type, elementReaders);
    }
}
