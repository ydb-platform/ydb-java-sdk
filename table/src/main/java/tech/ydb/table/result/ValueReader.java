package tech.ydb.table.result;

import tech.ydb.table.values.Type;

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
