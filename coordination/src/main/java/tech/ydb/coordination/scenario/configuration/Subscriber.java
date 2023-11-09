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
    private final AtomicBoolean isWorking;

    private Subscriber(AtomicBoolean isWorking) {
        this.isWorking = isWorking;
    }

    public static CompletableFuture<Subscriber> newSubscriberAsync(CoordinationClient client, String path,
             String semaphoreName, Consumer<byte[]> observer) {
        return client.createSession(path)
                .thenApply(session -> {
                    final AtomicBoolean isWorking = new AtomicBoolean(true);
                    session.createSemaphore(semaphoreName, 1)
                            .whenComplete((status, th1) ->
                                    recursiveDescribe(session, semaphoreName, isWorking, observer));
                    return new Subscriber(isWorking);
                });
    }

    public static Subscriber newSubscriber(CoordinationClient client, String path,
                                                              String semaphoreName, Consumer<byte[]> observer) {
        return newSubscriberAsync(client, path, semaphoreName, observer).join();
    }

    private static void recursiveDescribe(CoordinationSession session, String name, AtomicBoolean isWorking,
                                          Consumer<byte[]> observer) {
        if (isWorking.get()) {
            session.describeAndWatchSemaphore(name,
                            DescribeSemaphoreMode.DATA_ONLY, WatchSemaphoreMode.WATCH_DATA)
                    .whenComplete((result, thDescribe) -> {
                        if (thDescribe == null && result != null && result.isSuccess()) {
                            observer.accept(result.getValue().getDescription().getData());
                            result.getValue().getChangedFuture().whenComplete((changedEvent, th) -> {
                                if (th == null) {
                                    recursiveDescribe(session, name, isWorking, observer);
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

    @Override
    public void close() {
        isWorking.set(false);
    }
}
