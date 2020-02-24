package tech.ydb.table.result;

import tech.ydb.table.values.Type;
import tech.ydb.table.values.Value;

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

    Value<?> getGenericValue();

    Type getValueType();

}
