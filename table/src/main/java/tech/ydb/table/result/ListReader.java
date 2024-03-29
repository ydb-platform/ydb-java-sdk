package tech.ydb.table.result;

/**
 * @author Sergey Polovko
 */
public interface ListReader {

    int getListItemsCount();

    ValueReader getListItem(int index);
}
