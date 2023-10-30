package tech.ydb.coordination.scenario.service_discovery;

import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import tech.ydb.coordination.CoordinationSession;
import tech.ydb.coordination.description.SemaphoreChangedEvent;
import tech.ydb.coordination.description.SemaphoreDescription;
import tech.ydb.coordination.settings.DescribeSemaphoreMode;
import tech.ydb.coordination.settings.WatchSemaphoreMode;
import tech.ydb.core.Result;
import tech.ydb.core.Status;
import tech.ydb.core.StatusCode;
import tech.ydb.core.UnexpectedResultException;

public class Subscriber {
    public static final String SEMAPHORE_NAME = "service-discovery-semaphore";
    private final Consumer<SemaphoreChangedEvent>[] subscribeFunctor = new Consumer[1];
    private final BiConsumer<? super Result<SemaphoreDescription>, ? super Throwable>[] dataSetter = new BiConsumer[1];
    private final CompletableFuture<SemaphoreDescription>[] description = new CompletableFuture[1];

    private Subscriber(CoordinationSession session) {
        this.description[0] = new CompletableFuture<>();
        this.dataSetter[0] = (semaphoreDescriptionResult, throwable) -> {
            if (semaphoreDescriptionResult == null || !semaphoreDescriptionResult.isSuccess() || throwable != null) {
                throw new UnexpectedResultException("Subscriber get exception when trying to describe semaphore.",
                        semaphoreDescriptionResult != null ? semaphoreDescriptionResult.getStatus() : Status.of(
                                StatusCode.UNUSED_STATUS));
            }
            description[0].complete(semaphoreDescriptionResult.getValue());
        };
        this.subscribeFunctor[0] = changes -> {
            this.description[0] = new CompletableFuture<>();
            session.describeSemaphore(SEMAPHORE_NAME,
                            DescribeSemaphoreMode.WITH_OWNERS,
                            WatchSemaphoreMode.WATCH_DATA_AND_OWNERS,
                            subscribeFunctor[0])
                    .whenComplete(this.dataSetter[0]);
        };
    }

    public static CompletableFuture<Subscriber> newSubscriber(CoordinationSession session) {
        final Subscriber subscriber = new Subscriber(session);
        return session.describeSemaphore(
                SEMAPHORE_NAME,
                DescribeSemaphoreMode.WITH_OWNERS,
                WatchSemaphoreMode.WATCH_DATA_AND_OWNERS,
                subscriber.subscribeFunctor[0]
        ).whenComplete(subscriber.dataSetter[0]).thenApply(ignored -> subscriber);
    }

    public CompletableFuture<SemaphoreDescription> getDescription() {
        return description[0];
    }

    public void stop() {
        dataSetter[0] = (ignoredResult, ignoredTh) -> {
        };
        subscribeFunctor[0] = ignored -> {
        };
    }
}
