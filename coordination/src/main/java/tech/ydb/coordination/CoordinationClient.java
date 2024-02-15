package tech.ydb.coordination;

import java.util.concurrent.CompletableFuture;

import javax.annotation.WillNotClose;

import tech.ydb.coordination.description.NodeConfig;
import tech.ydb.coordination.impl.CoordinationClientImpl;
import tech.ydb.coordination.settings.CoordinationNodeSettings;
import tech.ydb.coordination.settings.CoordinationSessionSettings;
import tech.ydb.coordination.settings.DescribeCoordinationNodeSettings;
import tech.ydb.coordination.settings.DropCoordinationNodeSettings;
import tech.ydb.core.Result;
import tech.ydb.core.Status;
import tech.ydb.core.grpc.GrpcTransport;

/**
 * @author Kirill Kurdyukov
 * @author Alexandr Gorshein
 */
public interface CoordinationClient {

    static CoordinationClient newClient(@WillNotClose GrpcTransport transport) {
        return new CoordinationClientImpl(transport);
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
     * @return new instance of coordination session
     */
    CoordinationSession createSession(String path, CoordinationSessionSettings settings);

    /**
     * Creates a new coordination node.
     *
     * @param path full path to coordination node
     * @param settings coordination node settings
     * @return future with status of operation
     */
    CompletableFuture<Status> createNode(String path, CoordinationNodeSettings settings);

    /**
     * Modifies settings of a coordination node
     *
     * @param path full path to coordination node
     * @param settings coordination node settings
     * @return future with status of operation
     */
    CompletableFuture<Status> alterNode(String path, CoordinationNodeSettings settings);

    /**
     * Drops a coordination node
     *
     * @param path full path to coordination node
     * @param settings drop coordination node settings
     * @return future with status of operation
     */
    CompletableFuture<Status> dropNode(String path, DropCoordinationNodeSettings settings);

    /**
     * Describes a coordination node
     *
     * @param path full path to coordination node
     * @param settings describe coordination node settings
     * @return future with node configuration
     */
    CompletableFuture<Result<NodeConfig>> describeNode(String path, DescribeCoordinationNodeSettings settings);



    // --------------- default methods ------------------------------

    /**
     * Creates a new coordination session with default settings.
     * The coordination session establishes bidirectional grpc stream with a specific coordination node and uses this
     * stream for exchanging messages with the coordination service.
     *
     * @param path full path to coordination node
     * @return new instance of coordination session
     */
    default CoordinationSession createSession(String path) {
        return createSession(path, CoordinationSessionSettings.newBuilder().build());
    }

    /**
     * Creates a new coordination node.
     *
     * @param path full path to coordination node
     * @return future with status of operation
     */
    default CompletableFuture<Status> createNode(String path) {
        return createNode(path, CoordinationNodeSettings.newBuilder().build());
    }

    /**
     * Drops a coordination node
     *
     * @param path full path to coordination node
     * @return future with status of operation
     */
    default CompletableFuture<Status> dropNode(String path) {
        return dropNode(path, DropCoordinationNodeSettings.newBuilder().build());
    }

    /**
     * Describes a coordination node
     *
     * @param path full path to coordination node
     * @return future with result of operation
     */
    default CompletableFuture<Result<NodeConfig>> describeNode(String path) {
        return describeNode(path, DescribeCoordinationNodeSettings.newBuilder().build());
    }
}
