package tech.ydb.coordination.scenario.leader_election;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.annotation.Nonnull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tech.ydb.coordination.CoordinationClient;
import tech.ydb.coordination.CoordinationSession;
import tech.ydb.coordination.SemaphoreLease;
import tech.ydb.coordination.description.SemaphoreChangedEvent;
import tech.ydb.coordination.description.SemaphoreDescription.Session;
import tech.ydb.coordination.description.SemaphoreWatcher;
import tech.ydb.coordination.settings.DescribeSemaphoreMode;
import tech.ydb.coordination.settings.WatchSemaphoreMode;
import tech.ydb.core.Result;
import tech.ydb.core.StatusCode;
import tech.ydb.core.UnexpectedResultException;

public class LeaderElection implements AutoCloseable {
    private static final Logger logger = LoggerFactory.getLogger(LeaderElection.class);
    private final AtomicBoolean isElecting = new AtomicBoolean(true);
    private final CompletableFuture<SemaphoreLease> acquireFuture = new CompletableFuture<>();
    private CompletableFuture<Session> describeFuture;
    private CompletableFuture<SemaphoreChangedEvent> changedEventFuture;
    private final CoordinationSession session;

    private LeaderElection(CoordinationSession session, String name, byte[] data) {
        this.session = session;
        recursiveAcquire(session, name, data);
        recursiveDescribe(session, name);
    }

    @Nonnull
    private static RuntimeException getSemaphoreWatcherException(Result<SemaphoreWatcher> semaphoreWatcherResult) {
        final RuntimeException e;
        if (semaphoreWatcherResult == null) {
            e = new NullPointerException(
                    "Describe semaphore in LeaderElection was unsuccessful");
        } else {
            e = new UnexpectedResultException(
                    "Describe semaphore in LeaderElection was unsuccessful.",
                    semaphoreWatcherResult.getStatus());
        }
        return e;
    }

    public static CompletableFuture<LeaderElection> joinElection(CoordinationClient client,
                                                                 String fullPath,
                                                                 String endpoint,
                                                                 String semaphoreName) {
        return client.createSession(fullPath)
                .thenApply(session ->
                        new LeaderElection(session, semaphoreName, endpoint.getBytes(StandardCharsets.UTF_8)));
    }

    private CompletableFuture<SemaphoreChangedEvent> recursiveDescribeDetail(CoordinationSession session, String name) {
        return session.describeAndWatchSemaphore(name,
                        DescribeSemaphoreMode.WITH_OWNERS,
                        WatchSemaphoreMode.WATCH_OWNERS)
                .thenCompose((semaphoreWatcherResult -> {
                            if (semaphoreWatcherResult != null && semaphoreWatcherResult.isSuccess()) {
                                describeFuture.complete(
                                        semaphoreWatcherResult
                                                .getValue()
                                                .getDescription()
                                                .getOwnersList()
                                                .get(0)
                                );
                                return semaphoreWatcherResult.getValue().getChangedFuture();
                            } else if (semaphoreWatcherResult != null &&
                                    semaphoreWatcherResult.getStatus().getCode() == StatusCode.NOT_FOUND) {
                                return CompletableFuture.completedFuture(new SemaphoreChangedEvent(false, false,
                                        false));
                            } else {
                                logger.debug("session.describeAndWatchSemaphore.whenComplete() {}",
                                        semaphoreWatcherResult);
                                final RuntimeException e = getSemaphoreWatcherException(semaphoreWatcherResult);
                                leaveElection();
                                describeFuture.completeExceptionally(e);
                                throw e;
                            }
                        })
                );
    }

    private void recursiveDescribe(CoordinationSession session, String name) {
        describeFuture = new CompletableFuture<>();
        changedEventFuture = recursiveDescribeDetail(session, name);
        changedEventFuture.whenComplete((semaphoreChangedEvent, th) -> {
            if (semaphoreChangedEvent != null && th == null && isElecting.get()) {
                recursiveDescribe(session, name);
            }
        });
    }

    private void recursiveAcquire(CoordinationSession session, String semaphoreName, byte[] data) {
        session.acquireEphemeralSemaphore(semaphoreName, data, Duration.ofHours(1)).whenComplete(
                (lease, throwable) -> {
                    if ((lease == null || throwable != null) && isElecting.get()) {
                        recursiveAcquire(session, semaphoreName, data);
                    } else if (lease != null) {
                        if (!isElecting.get()) {
                            lease.release();
                        } else {
                            acquireFuture.complete(lease);
                        }
                    }
                });
    }

    public CompletableFuture<Session> forceUpdateLeader() {
        changedEventFuture.complete(new SemaphoreChangedEvent(false, false, false));
        return getLeader();
    }

    public CompletableFuture<Session> getLeader() {
        return describeFuture;
    }

    public boolean isLeader() {
        return acquireFuture.getNow(null) != null;
    }

    public CompletableFuture<Boolean> leaveElection() {
        if (isElecting.compareAndSet(true, false)) {
            if (acquireFuture.isDone()) {
                final CompletableFuture<Boolean> releaseFuture = acquireFuture.join().release();
                releaseFuture.thenRun(session::close);
                return releaseFuture;
            }
            session.close();
        }
        return CompletableFuture.completedFuture(true);
    }

    @Override
    public void close() throws Exception {
        leaveElection();
    }
}
