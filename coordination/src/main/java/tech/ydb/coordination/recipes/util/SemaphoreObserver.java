package tech.ydb.coordination.recipes.util;

import java.io.Closeable;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicReference;

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

public class SemaphoreObserver implements Closeable {
    private static final Logger logger = LoggerFactory.getLogger(SemaphoreObserver.class);

    private final CoordinationSession session;
    private final String semaphoreName;
    private final WatchSemaphoreMode watchSemaphoreMode;
    private final DescribeSemaphoreMode describeSemaphoreMode;
    private final RetryPolicy retryPolicy;
    private final ListenableContainer<SemaphoreDescription> watchDataListenable = new ListenableContainer<>();
    private final ListenableContainer<CoordinationSession.State> sessionStateListenable = new ListenableContainer<>();

    private AtomicReference<State> state = new AtomicReference<>(State.CREATED);
    private AtomicReference<SemaphoreDescription> cachedData = new AtomicReference<>(null);
    private Future<Void> watchTask;

    public SemaphoreObserver(
            CoordinationSession session,
            String semaphoreName,
            WatchSemaphoreMode watchSemaphoreMode,
            DescribeSemaphoreMode describeSemaphoreMode,
            RetryPolicy retryPolicy
    ) {
        this.session = Objects.requireNonNull(session, "session cannot be null");
        this.semaphoreName = Objects.requireNonNull(semaphoreName, "semaphoreName cannot be null");
        this.watchSemaphoreMode = watchSemaphoreMode;
        this.describeSemaphoreMode = describeSemaphoreMode;
        this.retryPolicy = retryPolicy;

        this.session.addStateListener(state -> {
            if (!state.isActive()) {
                close();
            }
            // TODO: clear data after reconnect?
            sessionStateListenable.notifyListeners(state);
        });
    }

    public enum State {
        CREATED,
        STARTED,
        CLOSED
    }

    public void start() {
        if (state.compareAndSet(State.CREATED, State.STARTED)) {
            // TODO: first describe
            enqueueWatch();
        }
    }

    private synchronized boolean enqueueWatch() {
        if (watchTask != null && state.get() == State.STARTED) {
            return false;
        }

        watchTask = watchSemaphore().thenCompose(status -> {
            if (!status.isSuccess()) {
                // TODO: backoff via retryPolicy
                logger.error("Failed to watch semaphore: {} with status: {}", semaphoreName, status);
            }

            finish();
            return null;
        });
        return true;
    }

    private synchronized void finish() {
        watchTask = null;
        if (state.get() == State.STARTED) {
            enqueueWatch();
        }
    }

    private CompletableFuture<Status> watchSemaphore() {
        return session.watchSemaphore(
                semaphoreName,
                describeSemaphoreMode,
                watchSemaphoreMode
        ).thenCompose(result -> {
            Status status = result.getStatus();
            if (!status.isSuccess()) {
                return CompletableFuture.completedFuture(status);
            }
            SemaphoreWatcher watcher = result.getValue();
            saveWatchState(watcher.getDescription());
            return watcher.getChangedFuture().thenApply(Result::getStatus);
        });
    }

    private void saveWatchState(SemaphoreDescription description) {
        logger.info("Changed semaphore state from {} to {}", cachedData.get(), description);
        cachedData.set(description);
        watchDataListenable.notifyListeners(description);
    }

    private synchronized void stopTaskInternal() {
        Future<Void> localWatchTask = watchTask;
        if (localWatchTask != null) {
            localWatchTask.cancel(true);
            watchTask = null;
        }
    }

    public Listenable<SemaphoreDescription> getWatchDataListenable() {
        return watchDataListenable;
    }

    public @Nullable SemaphoreDescription getCachedData() {
        return cachedData.get();
    }

    @Override
    public void close() {
        state.set(State.CLOSED);
        stopTaskInternal();
    }
}
