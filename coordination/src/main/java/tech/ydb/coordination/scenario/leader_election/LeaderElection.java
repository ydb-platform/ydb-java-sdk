package tech.ydb.coordination.scenario.leader_election;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Function;

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
    private final Consumer<LeaderElection> takeLeadershipObserver;
    private final Consumer<LeaderElection> changeLeaderObserver;
    private CompletableFuture<SemaphoreChangedEvent> changedEventFuture;
    private final CoordinationSession session;
    private final String name;
    private final byte[] data;

    private LeaderElection(CoordinationSession session, String name, byte[] data,
                           Consumer<LeaderElection> takeLeadershipObserver,
                           Consumer<LeaderElection> changeLeaderObserver) {
        this.session = session;
        this.name = name;
        this.data = data;
        this.takeLeadershipObserver = takeLeadershipObserver;
        this.changeLeaderObserver = changeLeaderObserver;
        initializeAcquireFuture();
        recursiveAcquire();
        recursiveDescribe();
    }

    private void initializeAcquireFuture() {
        acquireFuture = new CompletableFuture<>();
        acquireFuture.thenRun(() -> takeLeadershipObserver.accept(this));
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
     * @return LeaderElectionBuilder where you can add event's observer
     */
    public static LeaderElectionBuilder joinElection(CoordinationClient client, String fullPath,
                                                                 String endpoint, String semaphoreName) {
        return new LeaderElectionBuilder(client, fullPath, endpoint, semaphoreName);
    }

    private CompletableFuture<SemaphoreChangedEvent> recursiveDescribeDetail() {
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
        describeFuture.thenRun(() -> changeLeaderObserver.accept(this));
        changedEventFuture = recursiveDescribeDetail();
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
    public synchronized CompletableFuture<Void> interruptLeadershipAsync() {
        if (isLeader()) {
            return acquireFuture.join().release().thenRun(() -> {
                initializeAcquireFuture();
                recursiveAcquire();
            });
        }
        return CompletableFuture.completedFuture(null);
    }

    /**
     * {@link LeaderElection#interruptLeadershipAsync()}
     */
    public synchronized void interruptLeadership() {
        interruptLeadershipAsync().join();
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

    public static class LeaderElectionBuilder {
        private final CoordinationClient client;
        private final String fullPath;
        private final String endpoint;
        private final String semaphoreName;
        private Consumer<LeaderElection> takeLeadershipObserver;
        private Consumer<LeaderElection> leaderChangeObserver;

        LeaderElectionBuilder(CoordinationClient client, String fullPath, String endpoint,
                                     String semaphoreName) {
            this.client = client;
            this.fullPath = fullPath;
            this.endpoint = endpoint;
            this.semaphoreName = semaphoreName;
        }

        /**
         * Add observer for taking leadership. It will be called when your participant becomes a leader
         * @param takeLeadershipObserver - callback for taking leadership observing
         * @return LeaderElectionBuilder
         */
        public LeaderElectionBuilder withTakeLeadershipObserver(Consumer<LeaderElection> takeLeadershipObserver) {
            this.takeLeadershipObserver = takeLeadershipObserver;
            return this;
        }

        /**
         * Add observer for changing leader. It will be called when participant joins the election,
         *                                                     when participant is notified about leader change
         *                                                     when participant calls forceUpdateLeader
         * @param leaderChangeObserver - callback for leader change observing
         * @return LeaderElectionBuilder
         */
        public LeaderElectionBuilder withChangeLeaderObserver(Consumer<LeaderElection> leaderChangeObserver) {
            this.leaderChangeObserver = leaderChangeObserver;
            return this;
        }

        /**
         * Build Leader Election participant in asynchronous way
         * @return Completable future with Leader Election
         */
        public CompletableFuture<LeaderElection> buildAsync() {
            return client.createSession(fullPath)
                    .thenApply(session ->
                            new LeaderElection(
                                    session,
                                    semaphoreName,
                                    endpoint.getBytes(StandardCharsets.UTF_8),
                                    takeLeadershipObserver == null ? Function.identity()::apply :
                                            takeLeadershipObserver,
                                    leaderChangeObserver == null ? Function.identity()::apply : leaderChangeObserver
                            )
                    );
        }

        /**
         * {@link LeaderElectionBuilder#buildAsync()}
         */
        public LeaderElection build() {
            return buildAsync().join();
        }

    }
}
