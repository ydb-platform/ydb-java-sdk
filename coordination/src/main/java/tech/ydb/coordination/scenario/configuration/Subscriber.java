package tech.ydb.coordination.scenario.configuration;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tech.ydb.coordination.CoordinationClient;
import tech.ydb.coordination.CoordinationSession;
import tech.ydb.coordination.settings.DescribeSemaphoreMode;
import tech.ydb.coordination.settings.WatchSemaphoreMode;

public class Subscriber implements AutoCloseable {
    private static final Logger logger = LoggerFactory.getLogger(Subscriber.class);
    private final CoordinationSession session;
    private final AtomicBoolean isWorking;
    private Consumer<byte[]> observer;

    private Subscriber(CoordinationSession session, String semaphoreName, Consumer<byte[]> observer) {
        this.session = session;
        this.isWorking = new AtomicBoolean(true);
        this.observer = observer;
        session.createSemaphore(semaphoreName, 1)
                .whenComplete((status, th1) ->
                        recursiveDescribe(semaphoreName));
    }

    /**
     * Subscriber for the Configuration service which observe semaphore data changes from Publishers.
     * @param client - Coordination client
     * @param fullPath - full path to the coordination node
     * @param semaphoreName - Configuration scenario semaphore
     * @param observer - consumer which will be executed every time Subscriber receives info about data changes
     * @return Completable future with Subscriber
     */
    public static CompletableFuture<Subscriber> newSubscriberAsync(CoordinationClient client, String fullPath,
                                                                   String semaphoreName, Consumer<byte[]> observer) {
        return client.createSession(fullPath).thenApply(session -> new Subscriber(session, semaphoreName, observer));
    }

    /**
     * {@link Subscriber#newSubscriberAsync(CoordinationClient, String, String, Consumer)}
     */
    public static Subscriber newSubscriber(CoordinationClient client, String path,
                                           String semaphoreName, Consumer<byte[]> observer) {
        return newSubscriberAsync(client, path, semaphoreName, observer).join();
    }

    private void recursiveDescribe(String name) {
        if (isWorking.get()) {
            session.describeAndWatchSemaphore(name,
                            DescribeSemaphoreMode.DATA_ONLY, WatchSemaphoreMode.WATCH_DATA)
                    .whenComplete((result, thDescribe) -> {
                        if (thDescribe == null && result != null && result.isSuccess()) {
                            observer.accept(result.getValue().getDescription().getData());
                            result.getValue().getChangedFuture().whenComplete((changedEvent, th) -> {
                                if (th == null) {
                                    recursiveDescribe(name);
                                }
                            });
                            return;
                        }
                        isWorking.set(false);
                        logger.debug("Exception in describeAndWatchSemaphore request ({}, {}).",
                                result, thDescribe);
                    });
        }
    }

    /**
     * Reset observer
     * @param newObserver - new observer, which will process new data from Publishers
     */
    public void resetObserver(Consumer<byte[]> newObserver) {
        this.observer = newObserver;
    }

    /**
     * Close Subscriber with closing session
     */
    @Override
    public void close() {
        isWorking.set(false);
        session.close();
    }
}
