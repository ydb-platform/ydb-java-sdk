package ru.yandex.ydb.core.rpc;

/**
 * @author Sergey Polovko
 */
public interface Rpc extends AutoCloseable {

    @Override
    void close();
}
