package tech.ydb.coordination;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;

import tech.ydb.coordination.description.SemaphoreDescription;
import tech.ydb.coordination.description.SemaphoreWatcher;
import tech.ydb.coordination.settings.DescribeSemaphoreMode;
import tech.ydb.coordination.settings.WatchSemaphoreMode;
import tech.ydb.core.Result;
import tech.ydb.core.Status;

public interface CoordinationSession extends AutoCloseable {

    /**
     * Identifier of session. This value never changes even if the session restarts the grpc stream several times
     *
     * @return session id
     */
    long getId();

    @Override
    void close();

    boolean isClosed();

    /**
     * Create a new semaphore. This operation doesn't change internal state of the coordination session
     * so one session may be used for creating different semaphores
     *
     * @param name   Name of the semaphore to create
     * @param limit  Number of tokens that may be acquired by sessions
     * @param data   User-defined data that will be attached to the semaphore
     * @return Future with status of operation.
     * If there already was a semaphore with such a name, you get
     * {@code ALREADY_EXISTS} status.
     */
    CompletableFuture<Status> createSemaphore(String name, long limit, byte[] data);

    /**
     * Update data attached to the semaphore. This operation doesn't change internal state of the coordination session
     * so one session may be used for updating different semaphores
     *
     * @param name   Name of the semaphore to update
     * @param data   User-defined data that will be attached to the semaphore
     * @return future with status of operation
     */
    CompletableFuture<Status> updateSemaphore(String name, byte[] data);

    /**
     * Remove a semaphore. This operation doesn't change internal state of the coordination session
     * so one session may be used for removing different semaphores
     *
     * @param name  Name of the semaphore to remove
     * @param force Will delete semaphore even if it's currently acquired by sessions
     * @return Future with status of operation.
     */
    CompletableFuture<Status> deleteSemaphore(String name, boolean force);

    /**
     * Acquire an semaphore.
     * <p>WARNING: a single session can acquire only one semaphore in one moment<p>
     * Later requests override previous operations with the same semaphore,
     * e.g. to reduce acquired count, change timeout or attached data
     *
     * @param name    Name of the semaphore to acquire
     * @param count   Number of tokens to acquire on the semaphore
     * @param timeout Duration after which operation will fail if it's still waiting in the waiters queue
     * @param data    User-defined binary data that may be attached to the operation
     * @return If there is a semaphore with {@code name}, future will return a semaphore lease object.
     * If there is no such a semaphore, future will complete exceptionally
     * with {@link tech.ydb.core.UnexpectedResultException}.
     */
    CompletableFuture<Result<SemaphoreLease>> acquireSemaphore(String name, long count, byte[] data, Duration timeout);

    /**
     * Acquire an ephemeral semaphore.
     * Ephemeral semaphores are created with the first acquire operation and automatically deleted with
     * the last release operation.
     * <p>WARNING: a single session can acquire only one semaphore in one moment<p>
     * Later requests override previous operations with the same semaphore,
     * e.g. to reduce acquired count, change timeout or attached data
     *
     * @param name      Name of the semaphore to acquire
     * @param exclusive Flag of exclusive acquiring
     * @param timeout   Duration after which operation will fail if it's still waiting in the waiters queue
     * @param data      User-defined binary data that may be attached to the operation
     * @return future with a semaphore lease object
     */
    CompletableFuture<Result<SemaphoreLease>> acquireEphemeralSemaphore(String name, boolean exclusive, byte[] data,
            Duration timeout);

    CompletableFuture<Result<SemaphoreDescription>> describeSemaphore(String name, DescribeSemaphoreMode mode);

    CompletableFuture<Result<SemaphoreWatcher>> describeAndWatchSemaphore(String name,
            DescribeSemaphoreMode describeMode, WatchSemaphoreMode watchMode);

    // ----------------------------- default methods -------------------------------

    /**
     * Create a new semaphore. This operation doesn't change internal state of the coordination session
     * so one session may be used for creating different semaphores
     *
     * @param name   Name of the semaphore to create
     * @param limit  Number of tokens that may be acquired by sessions
     * @return future with status of operation
     */
    default CompletableFuture<Status> createSemaphore(String name, long limit) {
        return createSemaphore(name, limit, null);
    }


    /**
     * Acquire an semaphore.
     * <p>WARNING: a single session can acquire only one semaphore in one moment<p>
     * Later requests override previous operations with the same semaphore,
     * e.g. to reduce acquired count, change timeout or attached data
     *
     * @param name    Name of the semaphore to acquire
     * @param count   Number of tokens to acquire on the semaphore
     * @param timeout Duration after which operation will fail if it's still waiting in the waiters queue
     * @return future with a semaphore lease object
     */
    default CompletableFuture<Result<SemaphoreLease>> acquireSemaphore(String name, long count, Duration timeout) {
        return acquireSemaphore(name, count, null, timeout);
    }

    /**
     * Acquire an ephemeral semaphore.
     * Ephemeral semaphores are created with the first acquire operation and automatically deleted with
     * the last release operation.
     * <p>WARNING: a single session can acquire only one semaphore in one moment<p>
     * Later requests override previous operations with the same semaphore,
     * e.g. to reduce acquired count, change timeout or attached data
     *
     * @param name    Name of the semaphore to acquire
     * @param exclusive Flag of exclusive acquiring
     * @param timeout Duration after which operation will fail if it's still waiting in the waiters queue
     * @return future with a semaphore lease object
     */
    default CompletableFuture<Result<SemaphoreLease>> acquireEphemeralSemaphore(String name, boolean exclusive,
            Duration timeout) {
        return acquireEphemeralSemaphore(name, exclusive, null, timeout);
    }
}
