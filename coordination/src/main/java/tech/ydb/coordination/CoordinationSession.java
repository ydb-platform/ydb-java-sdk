package tech.ydb.coordination;

import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

import tech.ydb.coordination.settings.SemaphoreDescription;
import tech.ydb.core.Status;
import tech.ydb.proto.coordination.SessionRequest;

/**
 * @author Kirill Kurdyukov
 */
public interface CoordinationSession {

    long getSessionId();

    byte[] getProtectionKey();

//    CompletableFuture<Status> start(CoordinationSession.Observer observer, String nodePath);

    /**
     * First message used to start/restore a session
     *
     * @param sessionStart session start of proto body
     */
    @Deprecated
    void sendStartSession(SessionRequest.SessionStart sessionStart);

    /**
     * Used for checking liveness of the connection
     *
     * @param pingPong ping pong of proto body
     */
    @Deprecated
    void sendPingPong(SessionRequest.PingPong pingPong);

    void sendAcquireSemaphore(String semaphoreName, long count, long timeoutMillis, boolean ephemeral);
    void sendAcquireSemaphore(String semaphoreName, long count, long timeoutMillis, boolean ephemeral, long requestId);

    /**
     * Used to acquire a semaphore
     * <p>
     * WARNING: a single session cannot acquire the same semaphore multiple times
     * <p>
     * Later requests override previous operations with the same semaphore,
     * e.g. to reduce acquired count, change timeout or attached data
     * @param semaphoreName Name of the semaphore to acquire
     * @param count Number of tokens to acquire on the semaphore
     * @param timeoutMillis Timeout in milliseconds after which operation will fail
     *                      if it's still waiting in the waiters queue
     * @param ephemeral Ephemeral semaphores are created with the first acquire operation
     *                      and automatically deleted with the last release operation
     * @param data User-defined binary data that may be attached to the operation
     * @param requestId Client-defined request id, echoed in the response
     */
    void sendAcquireSemaphore(String semaphoreName, long count, long timeoutMillis, boolean ephemeral, byte[] data,
                              long requestId);

    void sendReleaseSemaphore(String semaphoreName);

    /**
     * Used to release a semaphore
     * <p>
     * WARNING: a single session cannot release the same semaphore multiple times
     * </p>
     * The release operation will either remove current session from waiters
     * queue or release an already owned semaphore.
     * @param semaphoreName Name of the semaphore to release
     * @param requestId Client-defined request id, echoed in the response
     */
    void sendReleaseSemaphore(String semaphoreName, long requestId);

    void sendDescribeSemaphore(String semaphoreName);

    /**
     * Used to describe semaphores and watch them for changes
     * <p>
     * WARNING: a describe operation will cancel previous watches on the same semaphore
     * </p>
     * @param semaphoreName Name of the semaphore to describe
     * @param includeOwners Response will include owners list if true
     * @param includeWaiters Response will include waiters list if true
     * @param watchData Watch for changes in semaphore data
     * @param watchOwners Watch for changes in semaphore owners (including owners' data)
     */
    void sendDescribeSemaphore(String semaphoreName, boolean includeOwners, boolean includeWaiters, boolean watchData,
                               boolean watchOwners);

    void sendCreateSemaphore(String semaphoreName, long limit);

    /**
     * Used to create a new semaphore
     * @param semaphoreName Name of the semaphore to create
     * @param limit Number of tokens that may be acquired by sessions
     * @param data User-defined data that is attached to the semaphore
     * @param requestId Client-defined request id, echoed in the response
     */
    void sendCreateSemaphore(String semaphoreName, long limit, byte[] data, long requestId);

    void sendUpdateSemaphore(String semaphoreName, byte[] data);

    /**
     * Used to change semaphore data
     * @param semaphoreName Name of the semaphore to update
     * @param data User-defined data that is attached to the semaphore
     * @param requestId Client-defined request id, echoed in the response
     */
    void sendUpdateSemaphore(String semaphoreName, byte[] data, long requestId);

    void sendDeleteSemaphore(String semaphoreName, boolean force);

    /**
     * Used to delete an existing semaphore
     * @param semaphoreName Name of the semaphore to delete
     * @param force Will delete semaphore even if currently acquired by sessions
     * @param requestId Client-defined request id, echoed in the response
     */
    void sendDeleteSemaphore(String semaphoreName, boolean force, long requestId);

    CompletableFuture<Status> getLifetimeFuture();

    void stop();

    interface Observer {

        default void onAcquireSemaphoreResult(boolean acquired, Status status, int requestId) {
        }

        default void onDescribeSemaphoreResult(SemaphoreDescription description, Status status, int requestId) {
        }

        default void onDescribeSemaphoreChanged(boolean dataChanged, boolean ownersChanged, int requestId) {
        }

        default void onDeleteSemaphoreResult(Status status, int requestId) {
        }

        default void onCreateSemaphoreResult(Status status, int requestId) {
        }

        default void onReleaseSemaphoreResult(boolean released, Status status, int requestId) {
        }

        default void onUpdateSemaphoreResult(Status status, int requestId) {
        }
    }
}
