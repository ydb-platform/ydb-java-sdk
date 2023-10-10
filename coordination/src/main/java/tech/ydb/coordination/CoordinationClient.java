package tech.ydb.coordination;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;

import javax.annotation.WillNotClose;

import tech.ydb.coordination.impl.CoordinationClientImpl;
import tech.ydb.coordination.rpc.grpc.GrpcCoordinationRpc;
import tech.ydb.coordination.settings.CoordinationNodeSettings;
import tech.ydb.coordination.settings.DescribeCoordinationNodeSettings;
import tech.ydb.coordination.settings.DropCoordinationNodeSettings;
import tech.ydb.core.Status;
import tech.ydb.core.grpc.GrpcTransport;

/**
 * @author Kirill Kurdyukov
 */
public interface CoordinationClient {

    static CoordinationClient newClient(@WillNotClose GrpcTransport transport) {
        return new CoordinationClientImpl(GrpcCoordinationRpc.useTransport(transport));
    }

    /**
     * Bidirectional stream used to establish a session with a coordination node
     * <p>
     * Relevant APIs for managing semaphores, distributed locking, creating or
     * restoring a previously established session are described using nested
     * messages in SessionRequest and SessionResponse. Session is established
     * with a specific coordination node (previously created using CreateNode
     * below) and semaphores are local to that coordination node.
     *
     * @return coordination node session
     */
    CompletableFuture<CoordinationSessionNew> createSession(String nodePath, Duration timeout);

    /**
     * Creates a new coordination node.
     *
     * @param path full path to coordination node
     * @param coordinationNodeSettings coordination node settings
     * @return status of request
     */
    CompletableFuture<Status> createNode(
            String path,
            CoordinationNodeSettings coordinationNodeSettings
    );

    /**
     * Modifies settings of a coordination node
     *
     * @param path full path to coordination node
     * @param coordinationNodeSettings coordination node settings
     * @return status of request
     */
    CompletableFuture<Status> alterNode(
            String path,
            CoordinationNodeSettings coordinationNodeSettings
    );

    /**
     * Drops a coordination node
     *
     * @param path full path to coordination node
     * @param dropCoordinationNodeSettings drop coordination node settings
     * @return request of status
     */
    CompletableFuture<Status> dropNode(
            String path,
            DropCoordinationNodeSettings dropCoordinationNodeSettings
    );

    /**
     * Describes a coordination node
     *
     * @param path full path to coordination node
     * @param describeCoordinationNodeSettings describe coordination node settings
     * @return request of status
     */
    CompletableFuture<Status> describeNode(
            String path,
            DescribeCoordinationNodeSettings describeCoordinationNodeSettings
    );

    /**
     * Database path
     * Using for creating coordination node path
     *
     * @return path to database
     */
    String getDatabase();
}
