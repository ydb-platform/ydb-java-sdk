package tech.ydb.coordination.recipes.election;

import java.io.Closeable;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.common.base.Preconditions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tech.ydb.common.retry.RetryForever;
import tech.ydb.coordination.CoordinationClient;
import tech.ydb.coordination.CoordinationSession;
import tech.ydb.coordination.description.SemaphoreDescription;
import tech.ydb.coordination.recipes.locks.LockInternals;
import tech.ydb.coordination.recipes.util.Listenable;
import tech.ydb.coordination.recipes.util.SessionListenableProvider;
import tech.ydb.coordination.recipes.util.SemaphoreObserver;
import tech.ydb.coordination.settings.DescribeSemaphoreMode;
import tech.ydb.coordination.settings.WatchSemaphoreMode;
import tech.ydb.core.StatusCode;

// TODO: настройки + переименовать переменные + рекомендации по коду + логгирование + backoff политика
public class LeaderElection implements Closeable, SessionListenableProvider {
    private static final Logger logger = LoggerFactory.getLogger(LeaderElection.class);
    private static final long MAX_LEASE = 1L;

    private final CoordinationClient client;
    private final LeaderElectionListener leaderElectionListener;
    private final String coordinationNodePath;
    private final String electionName;
    private final byte[] data;
    private final ExecutorService electionExecutor;
    private final LockInternals lock;
    private final SemaphoreObserver semaphoreObserver;

    private AtomicReference<State> state = new AtomicReference<>(State.CREATED);
    private volatile boolean autoRequeue = false;
    private volatile boolean isLeader = false;
    private Future<Void> electionTask = null;

    private enum State {
        CREATED,
        STARTED,
        CLOSED
    }

    public LeaderElection(
            CoordinationClient client,
            LeaderElectionListener leaderElectionListener,
            String coordinationNodePath,
            String electionName,
            byte[] data
    ) {
        this(
                client,
                leaderElectionListener,
                coordinationNodePath,
                electionName,
                data,
                Executors.newSingleThreadExecutor()
        );
    }

    public LeaderElection(
            CoordinationClient client,
            LeaderElectionListener leaderElectionListener,
            String coordinationNodePath,
            String electionName,
            byte[] data,
            ExecutorService executorService
    ) {
        this.client = client;
        this.leaderElectionListener = leaderElectionListener;
        this.coordinationNodePath = coordinationNodePath;
        this.electionName = electionName;
        this.data = data;
        this.electionExecutor = executorService;
        this.lock = new LockInternals(
                MAX_LEASE,
                client,
                coordinationNodePath,
                electionName
        );
        this.semaphoreObserver = new SemaphoreObserver(
                lock.getCoordinationSession(),
                electionName,
                WatchSemaphoreMode.WATCH_OWNERS,
                DescribeSemaphoreMode.WITH_OWNERS_AND_WAITERS,
                new RetryForever(100) // TODO: передавать снаружи
        );
    }

    public void start() {
        Preconditions.checkState(state.compareAndSet(State.CREATED, State.STARTED), "Already started or closed");
        // TODO: create session?
        CoordinationSession coordinationSession = lock.getCoordinationSession();
        // TODO: retry on create? Non idempotent - will not be retried automatically
        lock.start();
        coordinationSession.createSemaphore(electionName, MAX_LEASE).thenAccept(status -> {
            if (status.isSuccess() || status.getCode() == StatusCode.ALREADY_EXISTS) {
                semaphoreObserver.start();
            }
            status.expectSuccess("Unable to create semaphore");
            // TODO: set status == error
        });

        if (autoRequeue) {
            enqueueElection();
        }
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
        Preconditions.checkState(state.get() == State.STARTED, "Already closed or not yet started");

        // TODO: корректно обрабатывать если старт еще не кончился
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
        if (!isQueued() && state.get() == State.STARTED) {
            electionTask = electionExecutor.submit(new Callable<Void>() {
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
            lock.getConnectedCoordinationSession(); // asserts that session is connected or throws exception
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

    private synchronized void finishTask() {
        electionTask = null;
        if (autoRequeue) {
            enqueueElection();
        }
    }

    private boolean isQueued() {
        return electionTask != null;
    }

    /**
     * Не гарантированы все, кроме лидера
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
        return lock.getSessionListenable();
    }

    @Override
    public synchronized void close() {
        Preconditions.checkState(state.compareAndSet(State.STARTED, State.CLOSED), "Already closed");

        Future<Void> localTask = electionTask;
        if (localTask != null) {
            localTask.cancel(true);
            electionTask = null;
        }

        electionExecutor.shutdown();
        semaphoreObserver.close();
    }
}
