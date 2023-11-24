package tech.ydb.coordination.scenario.leader_election;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
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
    private final Consumer<LeaderElection> takeLeadershipObserver;
    private final Consumer<LeaderElection> changeLeaderObserver;
    private final CoordinationSession session;
    private final String name;
    private final byte[] data;
    private CompletableFuture<SemaphoreLease> acquireFuture;
    private CompletableFuture<Optional<Session>> describeFuture;
    private CompletableFuture<SemaphoreChangedEvent> changedEventFuture;
    private final AtomicInteger numberOfDescribeRequestsAtThisMoment = new AtomicInteger(0);

    private LeaderElection(CoordinationSession session, String name, byte[] data,
                           LeadershipPolicy policy,
                           Consumer<LeaderElection> takeLeadershipObserver,
                           Consumer<LeaderElection> changeLeaderObserver) {
        this.session = session;
        this.name = name;
        this.data = data;
        this.takeLeadershipObserver = takeLeadershipObserver;
        this.changeLeaderObserver = changeLeaderObserver;
        initializeAcquireFuture();
        if (policy == LeadershipPolicy.TAKE_LEADERSHIP) {
            proposeLeadershipAsync();
        }
        recursiveDescribe();
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
     *
     * @param client        - Coordination client
     * @param fullPath      - full path to the coordination node
     * @param endpoint      - Leader's identifier. All participants see leader's endpoint
     * @param semaphoreName - All participants try to acquire the same semaphore. This semaphore will be deleted after
     *                      election, hence you shouldn't create semaphore with this name before election.
     * @return LeaderElectionBuilder where you can add event's observer
     */
    public static LeaderElectionBuilder joinElection(CoordinationClient client, String fullPath,
                                                     String endpoint, String semaphoreName) {
        return new LeaderElectionBuilder(client, fullPath, endpoint, semaphoreName);
    }

    private void initializeAcquireFuture() {
        acquireFuture = new CompletableFuture<>();
        acquireFuture.thenRun(() -> takeLeadershipObserver.accept(this));
    }

    private CompletableFuture<SemaphoreChangedEvent> recursiveDescribeDetail() {
        CompletableFuture<Result<SemaphoreWatcher>> descAndWatchFuture = session.describeAndWatchSemaphore(name,
                DescribeSemaphoreMode.WITH_OWNERS,
                WatchSemaphoreMode.WATCH_OWNERS);
        numberOfDescribeRequestsAtThisMoment.incrementAndGet();
        return descAndWatchFuture.handle((semaphoreWatcherResult, th) -> {
            if (numberOfDescribeRequestsAtThisMoment.getAndDecrement() > 1) {
                return new CompletableFuture<SemaphoreChangedEvent>();
            }
            if (th != null) {
                CompletableFuture<Boolean> callback = new CompletableFuture<>();
                if (isSemaphoreExistenceException(th, () -> callback.complete(true))) {
                    callback.join();
                    return recursiveDescribeDetail();
                }
                logger.warn("Exception when trying to describe the semaphore:" +
                        " (semaphoreWatcherResult = {}, throwable = {})", semaphoreWatcherResult, th);
                isElecting.set(false);
            }
            if (semaphoreWatcherResult != null && semaphoreWatcherResult.isSuccess()) {
                final List<Session> leader = semaphoreWatcherResult.getValue().getDescription().getOwnersList();
                describeFuture.complete(leader.isEmpty() ? Optional.empty() : Optional.of(leader.get(0)));
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
        }).thenCompose(Function.identity());
    }

    private void recursiveDescribe() {
        describeFuture = new CompletableFuture<>();
        describeFuture.thenRun(() -> changeLeaderObserver.accept(this));
        changedEventFuture = recursiveDescribeDetail();
        changedEventFuture.whenComplete((semaphoreChangedEvent, th) -> {
            if (isSemaphoreExistenceException(th, this::recursiveDescribe)) {
                return;
            }
            if (semaphoreChangedEvent != null && th == null && isElecting.get()) {
                recursiveDescribe();
                return;
            }
            logger.warn("Exception when trying to check changes at the semaphore:" +
                            " (semaphore changed event: {}, throwable: {}, isElecting: {})", semaphoreChangedEvent, th,
                    isElecting.get());
            isElecting.set(false);
        });
    }

    private void createSemaphore(Runnable callback) {
        session.createSemaphore(name, 1).whenComplete(((status, throwable) -> {
            if (status == null || throwable != null ||
                    (status.getCode() != StatusCode.ALREADY_EXISTS && !status.isSuccess())) {
                logger.warn("Exception when trying to create the semaphore: (status: {}, throwable: {})",
                        status, throwable);
                isElecting.set(false);
            }
            callback.run();
        }));
    }

    private boolean isSemaphoreExistenceException(Throwable th, Runnable callback) {
        if (th instanceof CompletionException) {
            th = th.getCause();
        }
        if (th instanceof UnexpectedResultException) {
            UnexpectedResultException e = (UnexpectedResultException) th;
            if (e.getStatus().getCode() == StatusCode.NOT_FOUND) {
                createSemaphore(callback);
                return true;
            }
        }
        return false;
    }

    private void recursiveAcquire() {
        session.acquireSemaphore(name, 1, data, Duration.ofHours(1)).whenComplete(
                (res, throwable) -> {
                    if (!isElecting.get()) {
                        if (res != null && res.isSuccess()) {
                            res.getValue().close();
                        }
                        return;
                    }
                    if (throwable != null) {
                        recursiveAcquire();
                        return;
                    }
                    if (res.getStatus().getCode() == StatusCode.NOT_FOUND) {
                        createSemaphore(this::recursiveAcquire);
                        return;
                    }
                    if (!res.isSuccess()) {
                        recursiveAcquire();
                        return;
                    }

                    acquireFuture.complete(res.getValue());
                });
    }

    public void proposeLeadershipAsync() {
        recursiveAcquire();
    }

    /**
     * Don't wait until the node notifies session about change the leader and require information about current leader
     *
     * @return Completable future of leader's endpoint.
     */
    public synchronized CompletableFuture<Optional<String>> forceUpdateLeaderAsync() {
        changedEventFuture.complete(new SemaphoreChangedEvent(false, false, false));
        return getLeaderAsync();
    }

    /**
     * {@link LeaderElection#forceUpdateLeaderAsync()}
     */
    public Optional<String> forceUpdateLeader() {
        return forceUpdateLeaderAsync().join();
    }

    /**
     * When your participant know the leader, you can see its endpoint.
     * @return Completable future of leader's endpoint if leader is present.
     */
    private CompletableFuture<Optional<String>> getLeaderAsync() {
        return describeFuture.thenApply(optional -> optional.map(session ->
                new String(session.getData(), StandardCharsets.UTF_8)));
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
        private LeadershipPolicy policy = LeadershipPolicy.ONLY_WATCH_LEADER;

        LeaderElectionBuilder(CoordinationClient client, String fullPath, String endpoint,
                              String semaphoreName) {
            this.client = client;
            this.fullPath = fullPath;
            this.endpoint = endpoint;
            this.semaphoreName = semaphoreName;
        }

        /**
         * Add observer for taking leadership. It will be called when your participant becomes a leader
         *
         * @param takeLeadershipObserver - callback for taking leadership observing
         * @return LeaderElectionBuilder
         */
        public LeaderElectionBuilder withTakeLeadershipObserver(Consumer<LeaderElection> takeLeadershipObserver) {
            this.takeLeadershipObserver = takeLeadershipObserver;
            return this;
        }

        /**
         * Add observer for changing leader. It will be called when participant joins the election,
         * when participant is notified about leader change
         * when participant calls forceUpdateLeader
         *
         * @param leaderChangeObserver - callback for leader change observing
         * @return LeaderElectionBuilder
         */
        public LeaderElectionBuilder withChangeLeaderObserver(Consumer<LeaderElection> leaderChangeObserver) {
            this.leaderChangeObserver = leaderChangeObserver;
            return this;
        }

        public LeaderElectionBuilder withLeadershipPolicy(LeadershipPolicy policy) {
            this.policy = policy;
            return this;
        }

        /**
         * Build Leader Election participant in asynchronous way
         *
         * @return Completable future with Leader Election
         */
        public CompletableFuture<LeaderElection> buildAsync() {
            return client.createSession(fullPath)
                    .thenApply(session ->
                            new LeaderElection(
                                    session,
                                    semaphoreName,
                                    endpoint.getBytes(StandardCharsets.UTF_8),
                                    policy,
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

    public enum LeadershipPolicy {
        TAKE_LEADERSHIP,
        ONLY_WATCH_LEADER
    }
}
