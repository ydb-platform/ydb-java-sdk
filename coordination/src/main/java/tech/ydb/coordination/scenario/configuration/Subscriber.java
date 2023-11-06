package tech.ydb.coordination.scenario.configuration;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tech.ydb.coordination.CoordinationClient;
import tech.ydb.coordination.description.SemaphoreChangedEvent;
import tech.ydb.coordination.settings.DescribeSemaphoreMode;
import tech.ydb.coordination.settings.WatchSemaphoreMode;

public class Subscriber implements AutoCloseable {
    static final String SEMAPHORE_PREFIX = "configuration-";
    private static final Logger logger = LoggerFactory.getLogger(Subscriber.class);
    private final AtomicBoolean isWorking;

    private Subscriber(AtomicBoolean isWorking) {
        this.isWorking = isWorking;
    }

    public static CompletableFuture<Subscriber> newSubscriber(CoordinationClient client, String path,
                                                              long token, Consumer<byte[]> observer) {
        return client.createSession(path)
                .thenApply(session -> {
                    final String name = SEMAPHORE_PREFIX + token;
                    final AtomicBoolean isWorking = new AtomicBoolean(true);
                    BiConsumer<? super SemaphoreChangedEvent, ? super Throwable>[] onChanges = new BiConsumer[1];
                    onChanges[0] = (changes, changesTh) -> {
                        if (isWorking.get()) {
                            session.describeAndWatchSemaphore(name,
                                            DescribeSemaphoreMode.DATA_ONLY, WatchSemaphoreMode.WATCH_DATA)
                                    .whenComplete((result, throwable) -> {
                                        if (throwable == null && result != null && result.isSuccess()) {
                                            observer.accept(result.getValue().getDescription().getData());
                                            result.getValue().getChangedFuture().whenComplete(onChanges[0]);
                                            return;
                                        }
                                        isWorking.set(false);
                                        logger.debug("Exception in describeAndWatchSemaphore request ({}, {}).",
                                                result, throwable);
                                    });
                        }
                    };
                    session.createSemaphore(name, 1)
                            .whenComplete(
                                    (status, th1) -> onChanges[0].accept(new SemaphoreChangedEvent(false, false, false),
                                            null)
                            );
                    return new Subscriber(isWorking);
                });
    }

    @Override
    public void close() {
        isWorking.set(false);
    }
}
