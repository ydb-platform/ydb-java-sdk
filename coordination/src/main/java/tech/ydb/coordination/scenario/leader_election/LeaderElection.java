package tech.ydb.coordination.scenario.leader_election;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tech.ydb.coordination.CoordinationSession;
import tech.ydb.coordination.SemaphoreLease;
import tech.ydb.coordination.description.SemaphoreChangedEvent;
import tech.ydb.coordination.description.SemaphoreDescription.Session;
import tech.ydb.coordination.settings.DescribeSemaphoreMode;
import tech.ydb.coordination.settings.WatchSemaphoreMode;
import tech.ydb.core.UnexpectedResultException;

public class LeaderElection {
    private static final Logger logger = LoggerFactory.getLogger(LeaderElection.class);
    private static final String SEMAPHORE_PREFIX = "leader-election-";
    private final AtomicBoolean isElecting;
    private final CompletableFuture<Session>[] describer;
    private final CompletableFuture<SemaphoreLease>[] acquireFuture;
    private final CompletableFuture<SemaphoreChangedEvent>[] changedEventFuture;

    private LeaderElection(AtomicBoolean isElecting, CompletableFuture<Session>[] describer,
                           CompletableFuture<SemaphoreLease>[] acquireFuture,
                           CompletableFuture<SemaphoreChangedEvent>[] changedEventFuture) {
        this.isElecting = isElecting;
        this.describer = describer;
        this.acquireFuture = acquireFuture;
        this.changedEventFuture = changedEventFuture;
    }

    public static LeaderElection joinElection(CoordinationSession session, String endpoint,
                                              long electionToken) {
        AtomicBoolean isElecting = new AtomicBoolean(true);
        final String semaphoreName = SEMAPHORE_PREFIX + electionToken;

        final CompletableFuture<SemaphoreChangedEvent>[] changedEventFuture = new CompletableFuture[1];

        CompletableFuture<Session>[] describer = new CompletableFuture[1];
        Consumer<SemaphoreChangedEvent>[] changeProcessor = new Consumer[1];
        describer[0] = new CompletableFuture<>();


        Consumer<SemaphoreChangedEvent> describerLocal = changes -> {
            /* Leader was changed */
            describer[0] = new CompletableFuture<>();
            if (isElecting.get()) {
                session.describeAndWatchSemaphore(semaphoreName,
                                DescribeSemaphoreMode.WITH_OWNERS,
                                WatchSemaphoreMode.WATCH_OWNERS)
                        .whenComplete(((semaphoreWatcherResult, throwable) -> {
                                    if (semaphoreWatcherResult != null && semaphoreWatcherResult.isSuccess()
                                            && throwable == null) {
                                        describer[0].complete(
                                                semaphoreWatcherResult
                                                        .getValue()
                                                        .getDescription()
                                                        .getOwnersList()
                                                        .get(0)
                                        );
                                        changedEventFuture[0] = semaphoreWatcherResult.getValue().getChangedFuture();
                                        changedEventFuture[0]
                                                .whenComplete((semaphoreChangedEvent, throwable1) -> {
                                                    if (semaphoreChangedEvent != null && throwable1 == null) {
                                                        changeProcessor[0].accept(semaphoreChangedEvent);
                                                    }
                                                });
                                    } else {
                                        logger.debug("session.describeAndWatchSemaphore.whenComplete() {}, {}",
                                                semaphoreWatcherResult, throwable);
                                        if (throwable != null) {
                                            describer[0].completeExceptionally(throwable);
                                        } else if (semaphoreWatcherResult == null) {
                                            describer[0].completeExceptionally(new NullPointerException(
                                                    "Describe semaphore in LeaderElection was unsuccessful")
                                            );
                                        } else {
                                            describer[0].completeExceptionally(
                                                    new UnexpectedResultException(
                                                            "Describe semaphore in LeaderElection was unsuccessful.",
                                                            semaphoreWatcherResult.getStatus())
                                            );
                                        }
                                        isElecting.set(false);
                                    }
                                })
                        );
            }
        };
        changeProcessor[0] = describerLocal;

        final CompletableFuture<SemaphoreLease>[] acquireFuture = new CompletableFuture[1];
        BiConsumer<? super SemaphoreLease, ? super Throwable>[] biConsumer = new BiConsumer[1];
        BiConsumer<? super SemaphoreLease, ? super Throwable> afterAcquire =
                /* Rejoin the queue on the semaphore */
                ((lease, throwable) -> {
                    if ((lease == null || throwable != null) && isElecting.get()) {
                        acquireFuture[0] =
                                acquire(session, semaphoreName, endpoint.getBytes(StandardCharsets.UTF_8),
                                        biConsumer[0]);
                    } else if (lease != null && !isElecting.get()) {
                        lease.release();
                    }
                });
        biConsumer[0] = afterAcquire;
        acquireFuture[0] =
                acquire(session, semaphoreName, endpoint.getBytes(StandardCharsets.UTF_8), afterAcquire);
        describerLocal.accept(new SemaphoreChangedEvent(false, false, false));

        return new LeaderElection(isElecting, describer, acquireFuture, changedEventFuture);
    }

    private static CompletableFuture<SemaphoreLease>
    acquire(CoordinationSession session, String semaphoreName, byte[] data,
            BiConsumer<? super SemaphoreLease, ? super Throwable> after) {
        return session.acquireEphemeralSemaphore(semaphoreName, data, Duration.ofHours(1)).whenComplete(after);
    }

    public CompletableFuture<Session> forceUpdateLeader() {
        changedEventFuture[0].complete(new SemaphoreChangedEvent(false, false, false));
        return getLeader();
    }

    public CompletableFuture<Session> getLeader() {
        return describer[0];
    }

    public CompletableFuture<Boolean> leaveElection() {
        isElecting.set(false);
        if (acquireFuture[0].isDone()) {
            return acquireFuture[0].join().release();
        }
        /* acquireFuture[0].cancel(true);  <-- future option */
        return CompletableFuture.completedFuture(true);
    }
}
