package tech.ydb.core.rpc;

/**
 * @author Sergey Polovko
 */
public interface RpcTransport extends AutoCloseable {

    OperationTray getOperationTray();

    @Override
    void close();
}
