package tech.ydb.core.rpc;

import javax.annotation.Nullable;

/**
 * @author Sergey Polovko
 */
public interface Rpc extends AutoCloseable {

    String getDatabase();

    /**
     * Returns endpoint (host:port) for corresponding node id.
     * Returns null if there is no such node id.
     * @param nodeId number identity of the node
     * @return endpoint associated with the node
     */
    @Nullable
    String getEndpointByNodeId(int nodeId);

    OperationTray getOperationTray();

    @Override
    void close();
}
