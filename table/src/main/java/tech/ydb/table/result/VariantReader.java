package tech.ydb.table.result;

/**
 * @author Sergey Polovko
 */
public interface VariantReader {

    int getVariantTypeIndex();

    ValueReader getVariantItem();
}
