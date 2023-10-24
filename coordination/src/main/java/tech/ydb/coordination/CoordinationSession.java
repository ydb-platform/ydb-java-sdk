package tech.ydb.coordination;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import tech.ydb.coordination.description.SemaphoreChangedEvent;
import tech.ydb.coordination.description.SemaphoreDescription;
import tech.ydb.coordination.settings.DescribeSemaphoreMode;
import tech.ydb.coordination.settings.WatchSemaphoreMode;
import tech.ydb.core.Result;
import tech.ydb.core.Status;

public interface CoordinationSession extends AutoCloseable {

    @Override
    void close();

    /**
     * {@link CoordinationSession#createSemaphore(String, long, byte[])}
     */
    default CompletableFuture<Status> createSemaphore(String semaphoreName, long limit) {
        return createSemaphore(semaphoreName, limit, null);
    }

    /**
     * Used to create a new semaphore
     *
     * @param semaphoreName Name of the semaphore to create
     * @param limit         Number of tokens that may be acquired by sessions
     * @param data          User-defined data that is attached to the semaphore
     */
    CompletableFuture<Status> createSemaphore(String semaphoreName, long limit, byte[] data);


    default CompletableFuture<Result<CoordinationSemaphore>> acquireSemaphore(
            String semaphoreName, long count, boolean ephemeral, Duration timeout) {
        return acquireSemaphore(semaphoreName, count, ephemeral, timeout, null);
    }

    /**
     * {@link CoordinationSession#acquireSemaphore(String, long, boolean, Duration, byte[])}
     */
    default CompletableFuture<Result<CoordinationSemaphore>> acquireSemaphore(
            String semaphoreName, long count, Duration timeout) {
        return acquireSemaphore(semaphoreName, count, false, timeout, null);
    }

    /**
     * Used to acquire an ephemeral semaphore.
     * Ephemeral semaphores are created with the first acquire operation and automatically deleted with
     * the last release operation.
     * <p>
     * WARNING: a single session cannot acquire the same semaphore multiple times
     * <p>
     * Later requests override previous operations with the same semaphore,
     * e.g. to reduce acquired count, change timeout or attached data
     *
     * @param semaphoreName Name of the semaphore to acquire
     * @param count         Number of tokens to acquire on the semaphore
     * @param timeout       Duration after which operation will fail
     *                      if it's still waiting in the waiters queue
     * @param data          User-defined binary data that may be attached to the operation
     */
    CompletableFuture<Result<CoordinationSemaphore>> acquireSemaphore(String semaphoreName, long count,
                                                                      boolean ephemeral,
                                                                      Duration timeout, byte[] data);

    CompletableFuture<Status> updateSemaphore(String semaphoreName, byte[] data);

    CompletableFuture<Result<SemaphoreDescription>> describeSemaphore(String name, DescribeSemaphoreMode mode);

    CompletableFuture<Result<SemaphoreDescription>> describeSemaphore(String name,
            DescribeSemaphoreMode describeMode, WatchSemaphoreMode watchMode, Consumer<SemaphoreChangedEvent> watcher);

    CompletableFuture<Status> deleteSemaphore(String semaphoreName, boolean force);

    long getId();

    interface CoordinationSemaphore {
        /**
         * Used to release a semaphore
         * <p>
         * WARNING: a single session cannot release the same semaphore multiple times
         * </p>
         * The release operation will either remove current session from waiters
         * queue or release an already owned semaphore.
         */
        CompletableFuture<Result<Boolean>> release();
    }
}
