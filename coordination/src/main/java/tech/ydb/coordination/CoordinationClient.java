package tech.ydb.coordination;

import java.util.concurrent.CompletableFuture;

import javax.annotation.WillNotClose;

import tech.ydb.coordination.impl.CoordinationClientImpl;
import tech.ydb.coordination.impl.CoordinationGrpc;
import tech.ydb.coordination.settings.CoordinationNodeSettings;
import tech.ydb.coordination.settings.CoordinationSessionSettings;
import tech.ydb.coordination.settings.DescribeCoordinationNodeSettings;
import tech.ydb.coordination.settings.DropCoordinationNodeSettings;
import tech.ydb.core.Status;
import tech.ydb.core.grpc.GrpcTransport;

/**
 * @author Kirill Kurdyukov
 * @author Alexandr Gorshein
 */
public interface CoordinationClient {

    static CoordinationClient newClient(@WillNotClose GrpcTransport transport) {
        return new CoordinationClientImpl(CoordinationGrpc.useTransport(transport));
    }

    /**
     * Database path
     * Using for creating coordination node path
     *
     * @return path to database
     */
    String getDatabase();

    /**
     * Creates a new coordination session.
     * The coordination session establishes bidirectional grpc stream with a specific coordination node and uses this
     * stream for exchanging messages with the coordination service.
     *
     * @param path full path to coordination node
     * @param settings coordination session settings
     * @return future with instance of coordination session
     */
    CompletableFuture<CoordinationSession> createSession(String path, CoordinationSessionSettings settings);

    /**
     * Creates a new coordination node.
     *
     * @param path full path to coordination node
     * @param coordinationNodeSettings coordination node settings
     * @return status of request
     */
    CompletableFuture<Status> createNode(String path, CoordinationNodeSettings coordinationNodeSettings);

    /**
     * Modifies settings of a coordination node
     *
     * @param path full path to coordination node
     * @param coordinationNodeSettings coordination node settings
     * @return status of request
     */
    CompletableFuture<Status> alterNode(String path, CoordinationNodeSettings coordinationNodeSettings);

    /**
     * Drops a coordination node
     *
     * @param path full path to coordination node
     * @param dropNodeSettings drop coordination node settings
     * @return request of status
     */
    CompletableFuture<Status> dropNode(String path, DropCoordinationNodeSettings dropNodeSettings);

    /**
     * Describes a coordination node
     *
     * @param path full path to coordination node
     * @param describeNodeSettings describe coordination node settings
     * @return request of status
     */
    CompletableFuture<Status> describeNode(String path, DescribeCoordinationNodeSettings describeNodeSettings);



    // --------------- default methods ------------------------------

    /**
     * Creates a new coordination session with default settings.
     * The coordination session establishes bidirectional grpc stream with a specific coordination node and uses this
     * stream for exchanging messages with the coordination service.
     *
     * @param path full path to coordination node
     * @return future with instance of coordination session
     */
    default CompletableFuture<CoordinationSession> createSession(String path) {
        return createSession(path, CoordinationSessionSettings.newBuilder().build());
    }

    /**
     * Creates a new coordination node.
     *
     * @param path full path to coordination node
     * @return status of request
     */
    default CompletableFuture<Status> createNode(String path) {
        return createNode(path, CoordinationNodeSettings.newBuilder().build());
    }

    /**
     * Drops a coordination node
     *
     * @param path full path to coordination node
     * @return request of status
     */
    default CompletableFuture<Status> dropNode(String path) {
        return dropNode(path, DropCoordinationNodeSettings.newBuilder().build());
    }

    /**
     * Describes a coordination node
     *
     * @param path full path to coordination node
     * @return request of status
     */
    default CompletableFuture<Status> describeNode(String path) {
        return describeNode(path, DescribeCoordinationNodeSettings.newBuilder().build());
    }
}
