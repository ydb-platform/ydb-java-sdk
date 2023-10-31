package tech.ydb.coordination.scenario.service_discovery;

import java.util.concurrent.atomic.AtomicReference;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tech.ydb.coordination.CoordinationSession;
import tech.ydb.coordination.description.SemaphoreChangedEvent;
import tech.ydb.coordination.description.SemaphoreDescription;
import tech.ydb.coordination.description.SemaphoreWatcher;
import tech.ydb.coordination.settings.DescribeSemaphoreMode;
import tech.ydb.coordination.settings.WatchSemaphoreMode;
import tech.ydb.core.Result;

public class Subscriber {
    private static final Logger logger = LoggerFactory.getLogger(Subscriber.class);

    public static final String SEMAPHORE_NAME = "service-discovery-semaphore";

    private final CoordinationSession session;
    private final AtomicReference<SemaphoreDescription> description = new AtomicReference<>();
    private final AtomicReference<Runnable> updateWaiter = new AtomicReference<>();

    private volatile boolean isStopped = false;

    private Subscriber(CoordinationSession session) {
        this.session = session;
        this.session.describeAndWatchSemaphore(
                SEMAPHORE_NAME, DescribeSemaphoreMode.WITH_OWNERS, WatchSemaphoreMode.WATCH_DATA_AND_OWNERS
        ).whenComplete(this::updateDescription).join();
    }

    private void updateDescription(Result<SemaphoreWatcher> result, Throwable th) {
        if (isStopped) {
            return;
        }

        if (th != null) {
            logger.error("unexpected exception on watch {} in session {}", SEMAPHORE_NAME, session.getId(), th);
            session.describeAndWatchSemaphore(
                    SEMAPHORE_NAME, DescribeSemaphoreMode.WITH_OWNERS, WatchSemaphoreMode.WATCH_DATA_AND_OWNERS
            ).whenComplete(this::updateDescription).join();
            return;
        }

        if (result != null) {
            if (result.isSuccess()) {
                SemaphoreWatcher watch = result.getValue();
                description.set(watch.getDescription());
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

    public static Subscriber newSubscriber(CoordinationSession session) {
        return new Subscriber(session);
    }

    public SemaphoreDescription getDescription() {
        return description.get();
    }

    public void stop() {
        isStopped = true;
    }

    public void setUndateWaiter(Runnable runnable) {
        Runnable old = updateWaiter.getAndSet(runnable);
        if (old != null) {
            old.run();
        }
    }
}
