package tech.ydb.coordination.recipes.election;

import java.io.Closeable;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tech.ydb.common.retry.RetryPolicy;
import tech.ydb.coordination.CoordinationClient;
import tech.ydb.coordination.CoordinationSession;
import tech.ydb.coordination.description.SemaphoreDescription;
import tech.ydb.coordination.recipes.locks.LockInternals;
import tech.ydb.coordination.recipes.util.Listenable;
import tech.ydb.coordination.recipes.util.ListenableContainer;
import tech.ydb.coordination.recipes.util.RetryableTask;
import tech.ydb.coordination.recipes.util.SessionListenableProvider;
import tech.ydb.coordination.recipes.util.SemaphoreObserver;
import tech.ydb.coordination.settings.DescribeSemaphoreMode;
import tech.ydb.coordination.settings.WatchSemaphoreMode;
import tech.ydb.core.Status;
import tech.ydb.core.StatusCode;

/**
 * A distributed leader election implementation using coordination services.
 * This class provides a mechanism for multiple instances to compete for leadership
 * of a named resource, with exactly one instance becoming the leader at any time.
 *
 * <p>The election process uses a semaphore-based approach where:
 * <ul>
 *   <li>The leader holds the semaphore lock</li>
 *   <li>Other participants wait in a queue</li>
 *   <li>Leadership can be voluntarily released or lost due to session issues</li>
 * </ul>
 *
 * <p>Thread safety: This class is thread-safe. All public methods can be called
 * from multiple threads concurrently.
 */
public class LeaderElection implements Closeable, SessionListenableProvider {
    private static final Logger logger = LoggerFactory.getLogger(LeaderElection.class);
    private static final ThreadFactory threadFactory = new ThreadFactoryBuilder()
            .setNameFormat("ydb-leader-election-%d")
            .setDaemon(true)
            .build();
    private static final long MAX_LEASE = 1L;

    private final LeaderElectionListener leaderElectionListener;
    private final String coordinationNodePath;
    private final String electionName;
    private final byte[] data;
    private final RetryPolicy retryPolicy;

    private final ScheduledExecutorService scheduledExecutor;
    private final ExecutorService blockingExecutor;
    private final CoordinationSession coordinationSession;
    private final ListenableContainer<CoordinationSession.State> sessionListenable;
    private final LockInternals lock;
    private final SemaphoreObserver semaphoreObserver;

    private final CountDownLatch startingLatch = new CountDownLatch(1);
    private AtomicReference<State> state = new AtomicReference<>(State.INITIAL);
    private AtomicReference<Future<Status>> initializingTask = new AtomicReference<>(null);
    private Future<Void> electionTask = null;
    private volatile boolean autoRequeue = false;
    private volatile boolean isLeader = false;

    private enum State {
        INITIAL,
        STARTING,
        STARTED,
        FAILED,
        CLOSED
    }

    /**
     * Creates a new LeaderElection instance with default settings.
     *
     * @param client the coordination client to use
     * @param coordinationNodePath path to the coordination node
     * @param electionName name of the election (must be unique per coordination node)
     * @param data optional data to associate with the leader (visible to all participants)
     * @param leaderElectionListener callback for leadership events
     */
    public LeaderElection(
            CoordinationClient client,
            String coordinationNodePath,
            String electionName,
            byte[] data,
            LeaderElectionListener leaderElectionListener
    ) {
        this(
                client,
                coordinationNodePath,
                electionName,
                data,
                leaderElectionListener,
                LeaderElectionSettings.newBuilder()
                        .build()
        );
    }

    /**
     * Creates a new LeaderElection instance with custom settings.
     *
     * @param client the coordination client to use
     * @param coordinationNodePath path to the coordination node
     * @param electionName name of the election (must be unique per coordination node)
     * @param data optional data to associate with the leader (visible to all participants)
     * @param leaderElectionListener callback for leadership events
     * @param settings configuration settings for the election process
     * @throws NullPointerException if any required parameter is null
     */
    public LeaderElection(
            CoordinationClient client,
            String coordinationNodePath,
            String electionName,
            byte[] data,
            LeaderElectionListener leaderElectionListener,
            LeaderElectionSettings settings
    ) {
        Preconditions.checkNotNull(client, "CoordinationClient cannot be null");
        Preconditions.checkNotNull(coordinationNodePath, "Coordination node path cannot be null");
        Preconditions.checkNotNull(electionName, "Election name cannot be null");
        Preconditions.checkNotNull(leaderElectionListener, "LeaderElectionListener cannot be null");
        Preconditions.checkNotNull(settings, "LeaderElectionSettings cannot be null");

        this.coordinationNodePath = coordinationNodePath;
        this.electionName = electionName;
        this.data = data;
        this.leaderElectionListener = leaderElectionListener;
        this.scheduledExecutor = settings.getScheduledExecutor();
        this.blockingExecutor = Executors.newSingleThreadExecutor(threadFactory);
        this.retryPolicy = settings.getRetryPolicy();

        this.coordinationSession = client.createSession(coordinationNodePath);
        this.sessionListenable = new ListenableContainer<>();
        coordinationSession.addStateListener(sessionState -> {
            if (!state.get().equals(State.CLOSED) && (sessionState == CoordinationSession.State.LOST ||
                    sessionState == CoordinationSession.State.CLOSED)) {
                logger.error("Coordination session unexpectedly changed to {} state, marking election as FAILED",
                        sessionState);
                stopInternal(State.FAILED);
            }
            sessionListenable.notifyListeners(sessionState);
        });
        this.lock = new LockInternals(
                coordinationSession,
                electionName,
                MAX_LEASE
        );
        this.semaphoreObserver = new SemaphoreObserver(
                coordinationSession,
                electionName,
                WatchSemaphoreMode.WATCH_OWNERS,
                DescribeSemaphoreMode.WITH_OWNERS_AND_WAITERS,
                settings.getRetryPolicy(),
                settings.getScheduledExecutor()
        );
    }

    /**
     * Starts the leader election process.
     *
     * @throws IllegalStateException if the election is already started or closed
     */
    public void start() {
        Preconditions.checkState(
                state.compareAndSet(State.INITIAL, State.STARTING),
                "Leader election may be started only once"
        );

        CompletableFuture<Status> connectionTask = executeWithRetry(coordinationSession::connect);
        CompletableFuture<Status> semaphoreCreateTask = executeWithRetry(
                () -> coordinationSession.createSemaphore(electionName, MAX_LEASE)
                        .thenCompose(status -> {
                            if (status.getCode() == StatusCode.ALREADY_EXISTS) {
                                return CompletableFuture.completedFuture(Status.SUCCESS);
                            }
                            return CompletableFuture.completedFuture(status);
                        })
        );

        CompletableFuture<Status> initializingRetriedTask = connectionTask
                .thenCompose(connectionStatus -> {
                    connectionStatus.expectSuccess("Unable to establish session");
                    return semaphoreCreateTask;
                })
                .thenApply(semaphoreStatus -> {
                    if (semaphoreStatus.isSuccess()) {
                        state.set(State.STARTED);
                        semaphoreObserver.start();
                        startingLatch.countDown();
                    }
                    semaphoreStatus.expectSuccess("Unable to create semaphore");
                    return semaphoreStatus;
                }).exceptionally(ex -> {
                    logger.error("Leader election initializing task failed", ex);
                    stopInternal(State.FAILED);
                    return Status.of(StatusCode.CLIENT_INTERNAL_ERROR);
                });

        initializingTask.set(initializingRetriedTask);

        if (autoRequeue) {
            enqueueElection();
        }
    }

    private CompletableFuture<Status> executeWithRetry(Supplier<CompletableFuture<Status>> taskSupplier) {
        return new RetryableTask("leaderElectionInitialize", taskSupplier, scheduledExecutor, retryPolicy).execute();
    }

    /**
     * Enables automatic requeueing when leadership is lost.
     * If called before start election will be started immediately.
     */
    public void autoRequeue() {
        autoRequeue = true;
    }

    /**
     * Checks if this instance is currently the leader.
     *
     * @return true if this instance is the leader, false otherwise
     */
    public boolean isLeader() {
        return isLeader;
    }

    /**
     * Re-queue an attempt for leadership. If this instance is already queued, nothing
     * happens and false is returned. If the instance was not queued, it is re-queued and true
     * is returned.
     *
     * @return true if reenqueue was successful
     * @throws IllegalStateException if the election is not in STARTED or STARTING state
     */
    public boolean requeue() {
        State localState = state.get();
        Preconditions.checkState(
                localState == State.STARTED || localState == State.STARTING,
                "Unexpected state: " + localState.name()
        );

        return enqueueElection();
    }

    /**
     * Interrupts the current leadership attempt if one is in progress.
     *
     * @return true if leadership was interrupted, false if no attempt was in progress
     */
    public synchronized boolean interruptLeadership() {
        Future<?> localTask = electionTask;
        if (localTask != null) {
            localTask.cancel(true);
            electionTask = null;
            return true;
        }
        return false;
    }

    private synchronized boolean enqueueElection() {
        State localState = state.get();
        if (!isQueued() && (localState == State.STARTED || localState == State.STARTING)) {
            electionTask = blockingExecutor.submit(new Callable<Void>() {
                @Override
                public Void call() throws Exception {
                    try {
                        doWork();
                    } finally {
                        finishTask();
                    }
                    return null;
                }
            });
            return true;
        }

        return false;
    }

    /**
     * Main work loop for leadership acquisition and maintenance.
     *
     * @throws Exception if the leadership attempt fails
     */
    private void doWork() throws Exception {
        isLeader = false;

        try {
            waitStartedStateOrFail();
            lock.tryAcquire(
                    null,
                    true,
                    data
            );
            isLeader = true;
            try {
                leaderElectionListener.takeLeadership();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw e;
            } catch (Throwable e) {
                logger.error("Unexpected error in takeLeadership", e);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw e;
        } finally {
            if (isLeader) {
                isLeader = false;
                boolean wasInterrupted = Thread.interrupted();
                try {
                    lock.release();
                } catch (Exception e) {
                    logger.error("Lock release exception for: " + coordinationNodePath);
                } finally {
                    if (wasInterrupted) {
                        Thread.currentThread().interrupt();
                    }
                }
            }
        }
    }

    private void waitStartedStateOrFail() throws InterruptedException {
        State localState = state.get();
        if (localState == State.STARTING) {
            startingLatch.await();
            localState = state.get();
        }

        if (localState != State.STARTED) {
            throw new IllegalStateException("Unexpected state: " + localState.name());
        }
    }

    private synchronized void finishTask() {
        electionTask = null;
        State localState = state.get();
        if (autoRequeue && localState != State.CLOSED && localState != State.FAILED) {
            enqueueElection();
        }
    }

    private boolean isQueued() {
        return electionTask != null;
    }

    /**
     * Gets all participants in the election.
     * Note: Due to observer limitations, waiters may be visible only eventually (after lease changes).
     *
     * @return list of election participants (owners and visible waiters)
     */
    public List<ElectionParticipant> getParticipants() {
        SemaphoreDescription semaphoreDescription = semaphoreObserver.getCachedData();
        if (semaphoreDescription == null) {
            return Collections.emptyList();
        }

        return Stream.concat(
                semaphoreDescription.getOwnersList().stream()
                        .map(session -> mapParticipant(session, true)),
                semaphoreDescription.getWaitersList().stream()
                        .map(session -> mapParticipant(session, false))
        ).collect(Collectors.toList());
    }

    /**
     * Gets the current leader if one exists.
     *
     * @return Optional containing the current leader, or empty if no leader exists
     */
    public Optional<ElectionParticipant> getCurrentLeader() {
        SemaphoreDescription semaphoreDescription = semaphoreObserver.getCachedData();
        if (semaphoreDescription == null) {
            return Optional.empty();
        }

        return semaphoreDescription.getOwnersList().stream().findFirst()
                .map(session -> mapParticipant(session, true));
    }

    private static ElectionParticipant mapParticipant(SemaphoreDescription.Session session, boolean owner) {
        return new ElectionParticipant(
                session.getId(),
                session.getData(),
                owner
        );
    }

    @Override
    public Listenable<CoordinationSession.State> getSessionListenable() {
        return sessionListenable;
    }

    /**
     * Closes the leader election and releases all resources.
     * After closing, the instance cannot be reused.
     */
    @Override
    public synchronized void close() {
        stopInternal(State.CLOSED);
    }

    /**
     * Internal method to stop the election with the specified termination state.
     *
     * @param terminationState the state to transition to (FAILED or CLOSED)
     * @return true if the state was changed, false if already terminated
     */
    private synchronized boolean stopInternal(State terminationState) {
        State localState = state.get();
        if (localState == State.FAILED || localState == State.CLOSED) {
            logger.warn("Already stopped leader election {} with status: {}", electionName, localState);
            return false;
        }
        logger.debug("Transitioning leader election {} from {} to {}", electionName, localState, terminationState);

        // change state
        state.set(terminationState);

        // unblock starting latch if not yet
        startingLatch.countDown();

        // stop tasks
        Future<Status> localInitializingTask = initializingTask.get();
        if (localInitializingTask != null) {
            localInitializingTask.cancel(true);
            initializingTask.set(null);
        }
        Future<Void> localTask = electionTask;
        if (localTask != null) {
            localTask.cancel(true);
            electionTask = null;
        }

        // Clean up resources
        try {
            semaphoreObserver.close();
        } catch (Exception e) {
            logger.warn("Error closing semaphore observer for {}: {}", electionName, e.getMessage());
        }

        try {
            blockingExecutor.shutdown();
        } catch (Exception e) {
            logger.warn("Error shutting down executor for {}: {}", electionName, e.getMessage());
        }
        return true;
    }
}
