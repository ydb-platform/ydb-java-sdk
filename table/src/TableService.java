package ru.yandex.ydb.table;

/**
 * @author Sergey Polovko
 */
public interface TableService extends AutoCloseable {

    TableClient newTableClient();

    SchemeClient newSchemeClient();

    @Override
    void close();
}
