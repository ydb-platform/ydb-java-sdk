package tech.ydb.table.result;

/**
 * @author Sergey Polovko
 */
public interface TupleReader {

    int getTupleElementsCount();

    ValueReader getTupleElement(int index);
}
