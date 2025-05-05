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
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.common.base.Preconditions;
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

// TODO: документцаия /  логгирование / рекомендации по коду
public class LeaderElection implements Closeable, SessionListenableProvider {
    private static final Logger logger = LoggerFactory.getLogger(LeaderElection.class);
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

    public LeaderElection(
            CoordinationClient client,
            String coordinationNodePath,
            String electionName,
            byte[] data,
            LeaderElectionListener leaderElectionListener,
            LeaderElectionSettings settings
    ) {
        this.coordinationNodePath = coordinationNodePath;
        this.electionName = electionName;
        this.data = data;
        this.leaderElectionListener = leaderElectionListener;
        this.scheduledExecutor = settings.getScheduledExecutor();
        this.blockingExecutor = Executors.newSingleThreadExecutor(); // TODO: thread factory
        this.retryPolicy = settings.getRetryPolicy();

        this.coordinationSession = client.createSession(coordinationNodePath);
        this.sessionListenable = new ListenableContainer<>();
        coordinationSession.addStateListener(sessionState -> {
            if (sessionState == CoordinationSession.State.LOST || sessionState == CoordinationSession.State.CLOSED) {
                logger.error("Coordination session unexpectedly changed to {} state, marking election as FAILED",
                        sessionState);
                state.set(State.FAILED);
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
                    state.set(State.FAILED);
                    semaphoreObserver.close();
                    startingLatch.countDown();
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

    public void autoRequeue() {
        autoRequeue = true;
    }

    public boolean isLeader() {
        return isLeader;
    }

    /**
     * Re-queue an attempt for leadership. If this instance is already queued, nothing
     * happens and false is returned. If the instance was not queued, it is re-queued and true
     * is returned
     *
     * @return true if re-enqueue was successful
     */
    public boolean requeue() {
        State localState = state.get();
        Preconditions.checkState(
                localState == State.STARTED || localState == State.STARTING,
                "Unexpected state: " + localState.name()
        );

        return enqueueElection();
    }

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

    private void doWork() throws Exception {
        isLeader = false;

        try {
            waitStartedState();
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
                logger.debug("takeLeadership exception", e);
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

    private void waitStartedState() throws InterruptedException {
        State localState = state.get();
        if (localState == State.STARTING) {
            startingLatch.await();
            localState = state.get();
        }

        if (localState == State.INITIAL || localState == State.CLOSED || localState == State.FAILED) {
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
     * Не гарантированы все, кроме лидера
     *
     * @return
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

    @Override
    public synchronized void close() {
        // TODO: Учесть все стейты
        Preconditions.checkState(state.compareAndSet(State.STARTED, State.CLOSED), "Already closed");

        Future<Void> localTask = electionTask;
        if (localTask != null) {
            localTask.cancel(true);
            electionTask = null;
        }

        blockingExecutor.shutdown();
        semaphoreObserver.close();
    }
}
