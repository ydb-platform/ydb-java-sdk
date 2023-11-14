package tech.ydb.coordination.scenario.leader_election;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayDeque;
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

    /**
     * Join an election.
     * When you only start an election, you should use this method as well as when you join an already
     * existing election.
     * @param client - Coordination client
     * @param fullPath - full path to the coordination node
     * @param endpoint - Leader's identifier. All participants see leader's endpoint
     * @param semaphoreName - All participants try to acquire the same semaphore. This semaphore will be deleted after
     *                      election, hence you shouldn't create semaphore with this name before election.
     * @return Completable future with leader election participant class.
     */
    public static CompletableFuture<LeaderElection> joinElectionAsync(CoordinationClient client, String fullPath,
                                                                 String endpoint, String semaphoreName) {
        return client.createSession(fullPath)
                .thenApply(session ->
                        new LeaderElection(session, semaphoreName, endpoint.getBytes(StandardCharsets.UTF_8)));
    }

    /**
     * {@link LeaderElection#joinElectionAsync(CoordinationClient, String, String, String)}
     */
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

    /**
     * Don't wait until the node notifies session about change the leader and require information about current leader
     * @return Completable future of leader's endpoint.
     */
    public synchronized CompletableFuture<String> forceUpdateLeaderAsync() {
        changedEventFuture.complete(new SemaphoreChangedEvent(false, false, false));
        return getLeaderAsync();
    }

    /**
     * {@link LeaderElection#forceUpdateLeaderAsync()}
     */
    public String forceUpdateLeader() {
        return forceUpdateLeaderAsync().join();
    }

    /**
     * When your participant know the leader, you can see its endpoint
     * @return Completable future of leader's endpoint.
     */
    public CompletableFuture<String> getLeaderAsync() {
        return describeFuture.thenApply(session -> new String(session.getData(), StandardCharsets.UTF_8));
    }

    /**
     * {@link LeaderElection#getLeaderAsync()}
     */
    public String getLeader() {
        return getLeaderAsync().join();
    }

    /**
     * Pass on your leadership in the election, but this session is still participating in the election.
     * So it could be the leader again.
     */
    public synchronized void interruptLeadership() {
        if (isLeader()) {
            acquireFuture.join().release();
            initializeAcquireFuture();
            recursiveAcquire();
        }
    }

    /**
     * Attention: using this method multiple times save all previous runnable arguments,
     * and all of them will be executed.
     * @param runnable - after acquiring leadership, execute this functional interface
     */
    public synchronized void whenTakeLead(final Runnable runnable) {
        afterAcquireFuture.add(runnable);
        if (acquireFuture != null && !acquireFuture.isDone()) {
            acquireFuture.thenRun(runnable);
        }
    }

    /**
     * @return true, if you are a leader at this moment otherwise false
     */
    public boolean isLeader() {
        return acquireFuture.getNow(null) != null;
    }

    /**
     * Leave election with closing all resources
     */
    @Override
    public void close() {
        if (isElecting.compareAndSet(true, false)) {
            if (acquireFuture.isDone()) {
                acquireFuture.join().release().join();
            }
            session.close();
        }
    }
}
