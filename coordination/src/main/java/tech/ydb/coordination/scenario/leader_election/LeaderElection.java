package tech.ydb.coordination.scenario.leader_election;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Queue;
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
    private CompletableFuture<SemaphoreLease> acquireFuture;
    private CompletableFuture<Session> describeFuture;
    private final Queue<Runnable> afterAcquireFuture;
    private CompletableFuture<SemaphoreChangedEvent> changedEventFuture;
    private final CoordinationSession session;
    private final String name;
    private final byte[] data;

    private LeaderElection(CoordinationSession session, String name, byte[] data) {
        this.session = session;
        this.name = name;
        this.data = data;
        afterAcquireFuture = new ArrayDeque<>();
        initializeAcquireFuture();
        recursiveAcquire();
        recursiveDescribe();
    }

    private void initializeAcquireFuture() {
        acquireFuture = new CompletableFuture<>();
        for (final Runnable r : afterAcquireFuture) {
            acquireFuture.thenRun(r);
        }
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

    public static CompletableFuture<LeaderElection> joinElectionAsync(CoordinationClient client, String fullPath,
                                                                 String endpoint, String semaphoreName) {
        return client.createSession(fullPath)
                .thenApply(session ->
                        new LeaderElection(session, semaphoreName, endpoint.getBytes(StandardCharsets.UTF_8)));
    }

    public static LeaderElection joinElection(CoordinationClient client, String fullPath, String endpoint,
                                                                 String semaphoreName) {
        return joinElectionAsync(client, fullPath, endpoint, semaphoreName).join();
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
                                close();
                                describeFuture.completeExceptionally(e);
                                throw e;
                            }
                        })
                );
    }

    private void recursiveDescribe() {
        describeFuture = new CompletableFuture<>();
        changedEventFuture = recursiveDescribeDetail(session, name);
        changedEventFuture.whenComplete((semaphoreChangedEvent, th) -> {
            if (semaphoreChangedEvent != null && th == null && isElecting.get()) {
                recursiveDescribe();
            }
        });
    }

    private void recursiveAcquire() {
        session.acquireEphemeralSemaphore(name, data, Duration.ofHours(1)).whenComplete(
                (lease, throwable) -> {
                    if ((lease == null || throwable != null) && isElecting.get()) {
                        recursiveAcquire();
                    } else if (lease != null) {
                        if (!isElecting.get()) {
                            lease.release();
                        } else {
                            acquireFuture.complete(lease);
                        }
                    }
                });
    }

    public synchronized CompletableFuture<String> forceUpdateLeaderAsync() {
        changedEventFuture.complete(new SemaphoreChangedEvent(false, false, false));
        return getLeaderAsync();
    }

    public String forceUpdateLeader() {
        return forceUpdateLeaderAsync().join();
    }

    public CompletableFuture<String> getLeaderAsync() {
        return describeFuture.thenApply(session -> new String(session.getData(), StandardCharsets.UTF_8));
    }

    public String getLeader() {
        return getLeaderAsync().join();
    }

    public synchronized void interruptLeadership() {
        if (isLeader()) {
            acquireFuture.join().release();
            initializeAcquireFuture();
            recursiveAcquire();
        }
    }

    public synchronized void whenTakeLead(final Runnable r) {
        afterAcquireFuture.add(r);
        if (acquireFuture != null && !acquireFuture.isDone()) {
            acquireFuture.thenRun(r);
        }
    }

    public boolean isLeader() {
        return acquireFuture.getNow(null) != null;
    }

    @Override
    public void close() {
        if (isElecting.compareAndSet(true, false)) {
            if (acquireFuture.isDone()) {
                final CompletableFuture<Boolean> releaseFuture = acquireFuture.join().release();
                releaseFuture.thenRun(session::close);
                return;
            }
            session.close();
        }
    }
}
