package ru.yandex.ydb.core.rpc;

/**
 * @author Sergey Polovko
 */
public interface RpcTransport extends AutoCloseable {

    @Override
    void close();
}
