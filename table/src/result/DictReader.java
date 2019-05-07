package ru.yandex.ydb.table.result;

/**
 * @author Sergey Polovko
 */
public interface DictReader {

    int getDictItemsCount();

    ValueReader getDictKey(int index);

    ValueReader getDictValue(int index);
}
