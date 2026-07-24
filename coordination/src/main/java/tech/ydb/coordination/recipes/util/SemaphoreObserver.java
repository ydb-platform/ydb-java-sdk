package tech.ydb.coordination.recipes.util;

import java.io.Closeable;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tech.ydb.common.retry.RetryPolicy;
import tech.ydb.coordination.CoordinationSession;
import tech.ydb.coordination.description.SemaphoreDescription;
import tech.ydb.coordination.description.SemaphoreWatcher;
import tech.ydb.coordination.settings.DescribeSemaphoreMode;
import tech.ydb.coordination.settings.WatchSemaphoreMode;
import tech.ydb.core.Result;
import tech.ydb.core.Status;

/**
 * Observes changes in a distributed semaphore state and notifies listeners.
 * Handles automatic reconnection and retries on failures.
 */
public class SemaphoreObserver implements Closeable {
    private static final Logger logger = LoggerFactory.getLogger(SemaphoreObserver.class);

    private final CoordinationSession session;
    private final String semaphoreName;
    private final WatchSemaphoreMode watchSemaphoreMode;
    private final DescribeSemaphoreMode describeSemaphoreMode;
    private final RetryPolicy retryPolicy;
    private final ScheduledExecutorService scheduledExecutor;
    private final ListenableContainer<SemaphoreDescription> watchDataListenable = new ListenableContainer<>();
    private final ListenableContainer<CoordinationSession.State> sessionStateListenable = new ListenableContainer<>();

    private AtomicReference<State> state = new AtomicReference<>(State.CREATED);
    private AtomicReference<SemaphoreDescription> cachedData = new AtomicReference<>(null);
    private Future<Void> watchTask;
    private final AtomicReference<CompletableFuture<Status>> forceDescribeTask = new AtomicReference<>();

    /**
     * Observer state
     */
    public enum State {
        CREATED,
        STARTED,
        CLOSED
    }

    /**
     * Creates a new semaphore observer instance.
     *
     * @param session coordination session to use
     * @param semaphoreName name of the semaphore to observe
     * @param watchSemaphoreMode watch mode configuration
     * @param describeSemaphoreMode describe mode configuration
     * @param retryPolicy retry policy for failed operations
     * @param scheduledExecutor executor for scheduling retries
     */
    public SemaphoreObserver(
            CoordinationSession session,
            String semaphoreName,
            WatchSemaphoreMode watchSemaphoreMode,
            DescribeSemaphoreMode describeSemaphoreMode,
            RetryPolicy retryPolicy,
            ScheduledExecutorService scheduledExecutor
    ) {
        this.session = Objects.requireNonNull(session, "session cannot be null");
        this.semaphoreName = Objects.requireNonNull(semaphoreName, "semaphoreName cannot be null");
        this.watchSemaphoreMode = Objects.requireNonNull(watchSemaphoreMode, "watchSemaphoreMode cannot be null");
        this.describeSemaphoreMode = Objects.requireNonNull(
                describeSemaphoreMode, "describeSemaphoreMode cannot be null"
        );
        this.retryPolicy = Objects.requireNonNull(retryPolicy, "retryPolicy cannot be null");
        this.scheduledExecutor = Objects.requireNonNull(scheduledExecutor, "scheduledExecutor cannot be null");

        this.session.addStateListener(state -> {
            logger.debug("Session state changed to {} for semaphore {}", state, semaphoreName);
            if (state == CoordinationSession.State.LOST || state == CoordinationSession.State.CLOSED) {
                logger.warn("Session lost or closed, closing observer for semaphore {}", semaphoreName);
                close();
            }

            if (state == CoordinationSession.State.RECONNECTED) {
                logger.info("Session reconnected, forcing describe for semaphore {}", semaphoreName);
                enqueueForceDescribe();
            }

            sessionStateListenable.notifyListeners(state);
        });
    }

    /**
     * Starts observing the semaphore state.
     * Can only be called once.
     */
    public void start() {
        if (!state.compareAndSet(State.CREATED, State.STARTED)) {
            logger.warn("Attempt to start already started observer for semaphore {}", semaphoreName);
            return;
        }

        logger.info("Starting semaphore observer for: {}", semaphoreName);
        enqueueForceDescribe();
        enqueueWatch();
    }

    private void enqueueForceDescribe() {
        CompletableFuture<Status> existingTask = forceDescribeTask.get();
        if (existingTask != null && !existingTask.isDone()) {
            logger.debug("Force describe task already exists for semaphore {}", semaphoreName);
            return;
        }

        Supplier<CompletableFuture<Status>> taskSupplier = () ->
                session.describeSemaphore(semaphoreName, describeSemaphoreMode)
                        .thenApply(result -> {
                            if (result.isSuccess()) {
                                logger.debug("Successfully described semaphore {}", semaphoreName);
                                saveDescription(result.getValue());
                            } else {
                                logger.warn("Failed to describe semaphore {}: {}", semaphoreName, result.getStatus());
                            }
                            return result.getStatus();
                        });

        CompletableFuture<Status> newTask = new RetryableTask(
                "semaphoreObserverForceDescribe-" + semaphoreName,
                taskSupplier,
                scheduledExecutor,
                retryPolicy
        ).execute();

        if (!forceDescribeTask.compareAndSet(existingTask, newTask)) {
            newTask.cancel(true);
            logger.debug("Another thread updated force describe task first for semaphore {}", semaphoreName);
        }
    }

    private synchronized boolean enqueueWatch() {
        if (watchTask != null && state.get() == State.STARTED) {
            logger.warn("Watch task already exists for semaphore {}", semaphoreName);
            return false;
        }

        logger.debug("Enqueuing new watch task for semaphore {}", semaphoreName);
        CompletableFuture<Status> watchRetriedTask = new RetryableTask(
                "semaphoreObserverWatchTask-" + semaphoreName,
                this::watchSemaphore,
                scheduledExecutor,
                retryPolicy
        ).execute();

        this.watchTask = watchRetriedTask.thenCompose(status -> {
            if (!status.isSuccess()) {
                logger.error("Failed to watch semaphore: {} with status: {}", semaphoreName, status);
            }
            finishWatch();
            return null;
        });

        return true;
    }

    private synchronized void finishWatch() {
        logger.debug("Finishing watch task for semaphore {}", semaphoreName);
        watchTask = null;
        if (state.get() == State.STARTED) {
            logger.debug("Restarting watch for semaphore {}", semaphoreName);
            enqueueWatch();
        }
    }

    private CompletableFuture<Status> watchSemaphore() {
        logger.debug("Starting watch operation for semaphore {}", semaphoreName);
        return session.watchSemaphore(
                semaphoreName,
                describeSemaphoreMode,
                watchSemaphoreMode
        ).thenCompose(result -> {
            Status status = result.getStatus();
            if (!status.isSuccess()) {
                logger.warn("Watch operation failed for semaphore {}: {}", semaphoreName, status);
                return CompletableFuture.completedFuture(status);
            }

            SemaphoreWatcher watcher = result.getValue();
            saveDescription(watcher.getDescription());
            logger.debug("Successfully started watching semaphore {}", semaphoreName);
            return watcher.getChangedFuture().thenApply(Result::getStatus);
        });
    }

    private void saveDescription(SemaphoreDescription description) {
        SemaphoreDescription prev = cachedData.getAndSet(description);
        logger.info("Semaphore state changed: {} -> {}",
                formatSemaphoreDescription(prev),
                formatSemaphoreDescription(description));
        watchDataListenable.notifyListeners(description);
    }

    private static String formatSemaphoreDescription(SemaphoreDescription desc) {
        if (desc == null) {
            return "null";
        }
        return String.format("SemaphoreDescription{name='%s', count=%d, limit=%d, owners=%d, waiters=%d}",
                desc.getName(), desc.getCount(), desc.getLimit(),
                desc.getOwnersList().size(), desc.getWaitersList().size());
    }

    /**
     * Returns listenable for semaphore state changes.
     */
    public Listenable<SemaphoreDescription> getWatchDataListenable() {
        return watchDataListenable;
    }

    /**
     * Gets the last observed semaphore state.
     * @return last cached semaphore description or null if not available
     */
    public @Nullable SemaphoreDescription getCachedData() {
        return cachedData.get();
    }

    /**
     * Closes the observer and releases all resources.
     */
    @Override
    public void close() {
        state.set(State.CLOSED);
        stopTasks();
    }

    private synchronized void stopTasks() {
        Future<Void> localWatchTask = watchTask;
        CompletableFuture<Status> describeTask = forceDescribeTask.getAndSet(null);

        if (describeTask != null) {
            logger.debug("Cancelling force describe task for semaphore {}", semaphoreName);
            describeTask.cancel(true);
        }

        if (localWatchTask != null) {
            logger.debug("Cancelling watch task for semaphore {}", semaphoreName);
            localWatchTask.cancel(true);
            watchTask = null;
        }
    }
}
