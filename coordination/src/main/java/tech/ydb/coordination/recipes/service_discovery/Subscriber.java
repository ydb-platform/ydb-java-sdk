package tech.ydb.coordination.recipes.service_discovery;

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
    private final Long sessionID;
    private final AtomicReference<Runnable> updateWaiter = new AtomicReference<>();
    private SemaphoreDescription description;
    private volatile boolean isStopped = false;

    private Subscriber(CoordinationSession session, Long id, Result<SemaphoreWatcher> watcherResult) {
        this.session = session;
        this.sessionID = id;
        updateDescription(watcherResult, null);
    }

    /**
     * Create a new subscriber for service discovery
     * @param client - Coordination client
     * @param fullPath - full path to the coordination node
     * @return Completable future with Subscriber
     */
    public static CompletableFuture<Subscriber> newSubscriberAsync(CoordinationClient client, String fullPath) {
        CoordinationSession newSession = client.createSession(fullPath);
        return newSession.connect().thenCompose(status ->
                        newSession.watchSemaphore(SEMAPHORE_NAME,
                                        DescribeSemaphoreMode.WITH_OWNERS, WatchSemaphoreMode.WATCH_DATA_AND_OWNERS)
                                .thenApply(result -> new Subscriber(newSession, newSession.getId(), result))
                );
    }

    /**
     * {@link Subscriber#newSubscriberAsync(CoordinationClient, String)}
     */
    public static Subscriber newSubscriber(CoordinationClient client, String fullPath) {
        return newSubscriberAsync(client, fullPath).join();
    }

    private void updateDescription(Result<SemaphoreWatcher> result, Throwable th) {
        if (isStopped) {
            return;
        }
        if (th != null) {
            logger.warn("unexpected exception on watch {} in session {}", SEMAPHORE_NAME, sessionID, th);
            session.watchSemaphore(
                    SEMAPHORE_NAME, DescribeSemaphoreMode.WITH_OWNERS, WatchSemaphoreMode.WATCH_DATA_AND_OWNERS
            ).whenComplete(this::updateDescription);
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
                        result.getStatus(), SEMAPHORE_NAME, sessionID);
                session.watchSemaphore(
                        SEMAPHORE_NAME, DescribeSemaphoreMode.WITH_OWNERS, WatchSemaphoreMode.WATCH_DATA_AND_OWNERS
                ).whenComplete(this::updateDescription);
            }
        }
    }

    private void handleChangedEvent(Result<SemaphoreChangedEvent> ev, Throwable th) {
        if (isStopped) {
            return;
        }

        if (th != null) {
            logger.error("unexpected exception on changed {} in session {}", SEMAPHORE_NAME, sessionID, th);
        }
        if (ev != null) {
            if (ev.isSuccess()) {
                logger.info("got changed {} with data {}, owners {}, redescrive",
                        SEMAPHORE_NAME, ev.getValue().isDataChanged(), ev.getValue().isOwnersChanged());
            } else {
                logger.info("got changed {} with statue {}", SEMAPHORE_NAME, ev.getStatus());
            }
        }

        session.watchSemaphore(
                SEMAPHORE_NAME, DescribeSemaphoreMode.WITH_OWNERS, WatchSemaphoreMode.WATCH_DATA_AND_OWNERS
        ).whenComplete(this::updateDescription);
    }

    /**
     * Get the last received (maybe out-of-date) information about Workers
     * @return description of semaphore where you can see all Workers
     */
    public SemaphoreDescription getDescription() {
        return description;
    }

    /**
     * Replace or set new runnable
     * @param runnable - will be executed when Subscriber receives information about changes on semaphore
     */
    public void setUpdateWaiter(Runnable runnable) {
        Runnable old = updateWaiter.getAndSet(runnable);
        if (old != null) {
            old.run();
        }
    }

    /**
     * Stop to observe Workers and close session
     */
    @Override
    public void close() {
        if (!isStopped) {
            isStopped = true;
            session.close();
        }
    }
}
