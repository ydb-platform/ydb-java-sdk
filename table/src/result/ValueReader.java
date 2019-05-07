package ru.yandex.ydb.table.result;

import ru.yandex.ydb.table.types.Type;

/**
 * @author Sergey Polovko
 */
public interface ValueReader extends
    PrimitiveReader,
    OptionalReader,
    TupleReader,
    ListReader,
    DictReader,
    StructReader,
    VariantReader
{

    void toString(StringBuilder sb);

    Type getValueType();

}
