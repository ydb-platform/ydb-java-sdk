package tech.ydb.core.rpc;

/**
 * @author Sergey Polovko
 */
public interface Rpc extends AutoCloseable {

    OperationTray getOperationTray();

    @Override
    void close();
}
