package tech.ydb.coordination;

import java.util.concurrent.CompletableFuture;

import tech.ydb.core.Status;

/**
 * @author Kirill Kurdyukov
 */
public interface CoordinationSession {


    long getSessionId();

    CompletableFuture<Status> start(Observer observer);

    /**
     * First message used to start/restore a session
     *
     * @param sessionStart session start of proto body
     */
    void sendStartSession(SessionRequest.SessionStart sessionStart);

    /**
     * Used for checking liveness of the connection
     *
     * @param pingPong ping pong of proto body
     */
    void sendPingPong(SessionRequest.PingPong pingPong);

    /**
     * Used to acquire a semaphore
     * <p>
     * WARNING: a single session cannot acquire the same semaphore multiple times
     * <p>
     * Later requests override previous operations with the same semaphore,
     * e.g. to reduce acquired count, change timeout or attached data.
     *
     * @param acquireSemaphore acquire semaphore of proto body
     */
    void sendAcquireSemaphore(SessionRequest.AcquireSemaphore acquireSemaphore);

    /**
     * Used to release a semaphore
     * <p>
     * WARNING: a single session cannot release the same semaphore multiple times
     * <p>
     * The release operation will either remove current session from waiters
     * queue or release an already owned semaphore.
     *
     * @param releaseSemaphore release semaphore of proto body
     */
    void sendReleaseSemaphore(SessionRequest.ReleaseSemaphore releaseSemaphore);

    /**
     * Used to describe semaphores and watch them for changes
     * <p>
     * WARNING: a describe operation will cancel previous watches on the same semaphore
     *
     * @param describeSemaphore describe semaphore of proto body
     */
    void sendDescribeSemaphore(SessionRequest.DescribeSemaphore describeSemaphore);

    /**
     * Used to create a new semaphore
     *
     * @param createSemaphore create semaphore of proto body
     */
    void sendCreateSemaphore(SessionRequest.CreateSemaphore createSemaphore);

    /**
     * Used to change semaphore data
     *
     * @param updateSemaphore update semaphore of proto body
     */
    void sendUpdateSemaphore(SessionRequest.UpdateSemaphore updateSemaphore);

    /**
     * Used to delete an existing semaphore
     *
     * @param deleteSemaphore delete semaphore of proto body
     */
    void sendDeleteSemaphore(SessionRequest.DeleteSemaphore deleteSemaphore);

    void stop();

    interface Observer {

        default void onAcquireSemaphoreResult(boolean acquired, Status status) {
        }

        default void onAcquireSemaphorePending() {
        }

        default void onDescribeSemaphoreResult(SemaphoreDescription semaphoreDescription, Status status) {
        }

        default void onDescribeSemaphoreChanged(boolean dataChanged, boolean ownersChanged) {
        }

        default void onDeleteSemaphoreResult(Status status) {
        }

        default void onCreateSemaphoreResult(Status status) {
        }

        default void onFailure(Status status) {
        }

        default void onReleaseSemaphoreResult(boolean released, Status status) {
        }

        default void onUpdateSemaphoreResult(long reqId, Status status) {
        }

        default void onSessionStarted() {
        }

        default void onPong(long pingValue) {
        }
    }

}
