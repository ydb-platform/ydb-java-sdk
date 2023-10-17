package tech.ydb.coordination;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import tech.ydb.coordination.settings.DescribeSemaphoreChanged;
import tech.ydb.coordination.settings.SemaphoreDescription;
import tech.ydb.core.Result;
import tech.ydb.core.Status;

public interface CoordinationSessionNew extends AutoCloseable {
    /**
     * {@link CoordinationSessionNew#createSemaphore(String, long, byte[])}
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
     * {@link CoordinationSessionNew#acquireSemaphore(String, long, boolean, Duration, byte[])}
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

    CompletableFuture<Result<SemaphoreDescription>> describeSemaphore(String semaphoreName, DescribeMode describeMode,
                                                                      WatchMode watchMode,
                                                                      Consumer<DescribeSemaphoreChanged> updateWatcher);

    CompletableFuture<Result<SemaphoreDescription>> describeSemaphore(String semaphoreName, DescribeMode mode);

    CompletableFuture<Status> deleteSemaphore(String semaphoreName, boolean force);

    boolean removeWatcher(String semaphoreName);

    enum DescribeMode {
        /**
         * Describe only semaphore's data (name, user-defined data and others)
         */
        DATA_ONLY(false, false),
        /**
         * Include owners list to describe result
         */
        WITH_OWNERS(true, false),
        /**
         * Include waiters list to describe result
         */
        WITH_WAITERS(false, true),
        /**
         * Include waiters and owners lists to describe result
         */
        WITH_OWNERS_AND_WAITERS(true, true);

        private final boolean includeOwners;
        private final boolean includeWaiters;

        DescribeMode(boolean includeOwners, boolean includeWaiters) {
            this.includeOwners = includeOwners;
            this.includeWaiters = includeWaiters;
        }

        public boolean includeOwners() {
            return includeOwners;
        }

        public boolean includeWaiters() {
            return includeWaiters;
        }
    }

    enum WatchMode {
        /**
         * Watch for changes in semaphore data
         */
        WATCH_DATA(true, false),
        /**
         * Watch for changes in semaphore owners
         */
        WATCH_OWNERS(false, true),
        /**
         * Watch for changes in semaphore data or owners
         */
        WATCH_DATA_AND_OWNERS(true, true);

        private final boolean watchData;
        private final boolean watchOwners;

        WatchMode(boolean watchData, boolean watchOwners) {
            this.watchData = watchData;
            this.watchOwners = watchOwners;
        }

        public boolean watchData() {
            return watchData;
        }

        public boolean watchOwners() {
            return watchOwners;
        }
    }

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