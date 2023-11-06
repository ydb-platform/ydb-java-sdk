package tech.ydb.coordination.scenario.service_discovery;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tech.ydb.coordination.CoordinationClient;
import tech.ydb.coordination.CoordinationSession;
import tech.ydb.coordination.description.SemaphoreChangedEvent;
import tech.ydb.coordination.description.SemaphoreDescription;
import tech.ydb.coordination.description.SemaphoreWatcher;
import tech.ydb.coordination.settings.DescribeSemaphoreMode;
import tech.ydb.coordination.settings.WatchSemaphoreMode;
import tech.ydb.core.Result;

public class Subscriber implements AutoCloseable {
    public static final String SEMAPHORE_NAME = "service-discovery-semaphore";
    private static final Logger logger = LoggerFactory.getLogger(Subscriber.class);
    private final CoordinationSession session;
    private final AtomicReference<Runnable> updateWaiter = new AtomicReference<>();
    private SemaphoreDescription description;
    private volatile boolean isStopped = false;

    private Subscriber(CoordinationSession session, Result<SemaphoreWatcher> watcherResult) {
        this.session = session;
        updateDescription(watcherResult, null);
    }

    public static CompletableFuture<Subscriber> newSubscriber(CoordinationClient client, String fullPath) {
        return client.createSession(fullPath)
                .thenCompose(session ->
                        session.describeAndWatchSemaphore(SEMAPHORE_NAME,
                                        DescribeSemaphoreMode.WITH_OWNERS, WatchSemaphoreMode.WATCH_DATA_AND_OWNERS)
                                .thenApply(semaphoreWatcherResult -> new Subscriber(session, semaphoreWatcherResult))
                );
    }

    private void updateDescription(Result<SemaphoreWatcher> result, Throwable th) {
        if (isStopped) {
            return;
        }
        if (th != null) {
            logger.warn("unexpected exception on watch {} in session {}", SEMAPHORE_NAME, session.getId(), th);
            session.describeAndWatchSemaphore(
                    SEMAPHORE_NAME, DescribeSemaphoreMode.WITH_OWNERS, WatchSemaphoreMode.WATCH_DATA_AND_OWNERS
            ).whenComplete(this::updateDescription).join();
            return;
        }
        if (result != null) {
            if (result.isSuccess()) {
                SemaphoreWatcher watch = result.getValue();
                description = watch.getDescription();
                Runnable updater = updateWaiter.get();
                if (updater != null) {
                    updater.run();
                }

                watch.getChangedFuture().whenCompleteAsync(this::handleChangedEvent);
            } else {
                logger.warn("unexpected result {} on watch {} in session {}",
                        result.getStatus(), SEMAPHORE_NAME, session.getId());
                session.describeAndWatchSemaphore(
                        SEMAPHORE_NAME, DescribeSemaphoreMode.WITH_OWNERS, WatchSemaphoreMode.WATCH_DATA_AND_OWNERS
                ).whenComplete(this::updateDescription).join();
            }
        }
    }

    private void handleChangedEvent(SemaphoreChangedEvent ev, Throwable th) {
        if (isStopped) {
            return;
        }

        if (th != null) {
            logger.error("unexpected exception on changed {} in session {}", SEMAPHORE_NAME, session.getId(), th);
        }
        if (ev != null) {
            logger.info("got changed {} with data {}, owners {}, connection {}, redescrive",
                    SEMAPHORE_NAME, ev.isDataChanged(), ev.isOwnersChanged(), ev.isConnectionWasFailed());
        }

        session.describeAndWatchSemaphore(
                SEMAPHORE_NAME, DescribeSemaphoreMode.WITH_OWNERS, WatchSemaphoreMode.WATCH_DATA_AND_OWNERS
        ).whenCompleteAsync(this::updateDescription).join();
    }

    public SemaphoreDescription getDescription() {
        return description;
    }

    public void setUpdateWaiter(Runnable runnable) {
        Runnable old = updateWaiter.getAndSet(runnable);
        if (old != null) {
            old.run();
        }
    }

    @Override
    public void close() throws Exception {
        if (!isStopped) {
            isStopped = true;
            session.close();
        }
    }
}
