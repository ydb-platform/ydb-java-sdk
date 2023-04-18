package tech.ydb.coordination;

import java.util.concurrent.CompletableFuture;

import tech.ydb.coordination.session.CoordinationSession;
import tech.ydb.coordination.settings.CoordinationNodeSettings;
import tech.ydb.coordination.settings.DescribeCoordinationNodeSettings;
import tech.ydb.coordination.settings.DropCoordinationNodeSettings;
import tech.ydb.core.Status;

/**
 * @author Kirill Kurdyukov
 */
public interface CoordinationClient {

    /**
     * Bidirectional stream used to establish a session with a coordination node
     * <p>
     * Relevant APIs for managing semaphores, distributed locking, creating or
     * restoring a previously established session are described using nested
     * messages in SessionRequest and SessionResponse. Session is established
     * with a specific coordination node (previously created using CreateNode
     * below) and semaphores are local to that coordination node.
     */
    CoordinationSession createSession(String path);

    /**
     * Creates a new coordination node
     */
    CompletableFuture<Status> createNode(
            String path,
            CoordinationNodeSettings coordinationNodeSettings
    );

    /**
     * Modifies settings of a coordination node
     */
    CompletableFuture<Status> alterNode(
            String path,
            CoordinationNodeSettings coordinationNodeSettings
    );

    /**
     * Drops a coordination node
     */
    CompletableFuture<Status> dropNode(
            String path,
            DropCoordinationNodeSettings dropCoordinationNodeSettings
    );

    /**
     * Describes a coordination node
     */
    CompletableFuture<Status> describeNode(
            String path,
            DescribeCoordinationNodeSettings describeCoordinationNodeSettings
    );
}
