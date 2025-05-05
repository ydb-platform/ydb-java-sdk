package tech.ydb.coordination.recipes.locks;

import java.io.Closeable;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicReference;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.ydb.coordination.CoordinationClient;
import tech.ydb.coordination.CoordinationSession;
import tech.ydb.coordination.recipes.locks.exception.LockException;
import tech.ydb.coordination.recipes.locks.exception.LockInitializationException;
import tech.ydb.coordination.recipes.locks.exception.LockStateException;
import tech.ydb.coordination.recipes.util.Listenable;
import tech.ydb.coordination.recipes.util.ListenableContainer;

/**
 * Distributed mutex implementation using a coordination service.
 * This implementation is thread-safe and supports session state listening.
 */
public class InterProcessMutex implements InterProcessLock, Closeable {
    private static final Logger logger = LoggerFactory.getLogger(InterProcessMutex.class);

    private final String lockName;
    private final CoordinationSession coordinationSession;
    private final Future<?> sessionConnectionTask;
    private final LockInternals lockInternals;
    private final ListenableContainer<CoordinationSession.State> sessionListenable;

    private final AtomicReference<State> state = new AtomicReference<>(State.INITIAL);

    /**
     * Internal state machine states
     */
    private enum State {
        INITIAL,
        STARTING,
        STARTED,
        FAILED,
        CLOSED
    }

    /**
     * Creates a new distributed mutex instance with default settings.
     *
     * @param client               coordination client
     * @param coordinationNodePath path to the coordination node
     * @param lockName             name of the lock
     * @throws IllegalArgumentException if any parameter is null
     */
    public InterProcessMutex(
            CoordinationClient client,
            String coordinationNodePath,
            String lockName
    ) {
        this(
                client,
                coordinationNodePath,
                lockName,
                InterProcessMutexSettings.newBuilder().build()
        );
    }

    /**
     * Creates a new distributed mutex instance.
     *
     * @param client               coordination client
     * @param coordinationNodePath path to the coordination node
     * @param lockName             name of the lock
     * @param settings             configuration settings
     * @throws IllegalArgumentException if any parameter is null
     */
    public InterProcessMutex(
            CoordinationClient client,
            String coordinationNodePath,
            String lockName,
            InterProcessMutexSettings settings
    ) {
        if (client == null || coordinationNodePath == null || lockName == null || settings == null) {
            throw new IllegalArgumentException("All parameters must be non-null");
        }

        state.set(State.STARTING);
        this.lockName = lockName;
        this.coordinationSession = client.createSession(coordinationNodePath);
        this.sessionListenable = new ListenableContainer<>();
        this.lockInternals = new LockInternals(coordinationSession, lockName);

        coordinationSession.addStateListener(sessionState -> {
            if (sessionState == CoordinationSession.State.LOST || sessionState == CoordinationSession.State.CLOSED) {
                logger.error("Coordination session unexpectedly changed to '{}' state, marking mutex as 'FAILED'",
                        sessionState);
                state.set(State.FAILED);
            }
            sessionListenable.notifyListeners(sessionState);
        });

        sessionConnectionTask = coordinationSession.connect().thenAccept(sessionConnectStatus -> {
            if (!sessionConnectStatus.isSuccess()) {
                state.set(State.FAILED);
                logger.error("Failed to establish coordination session for lock '{}'", lockName);
            } else {
                state.set(State.STARTED);
                logger.info("Successfully established session for lock '{}'", lockName);
            }
        });

        if (settings.isWaitConnection()) {
            try {
                logger.debug("Waiting for session connection to complete for lock '{}'", lockName);
                sessionConnectionTask.get();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                logger.error("Interrupted while waiting for session connection for lock '{}'", lockName, e);
                throw new LockInitializationException("Interrupted while initializing lock '" + lockName + "'", e,
                        lockName);
            } catch (ExecutionException e) {
                logger.error("Failed to initialize lock '{}' due to execution error", lockName, e);
                throw new LockInitializationException("Failed to initialize lock '" + lockName + "'", e.getCause(),
                        lockName);
            }
        }
    }

    @Override
    public void acquire() throws Exception {
        checkState();
        logger.debug("Attempting to acquire lock '{}'", lockName);
        lockInternals.tryAcquire(
                null,
                true,
                null
        );
        logger.debug("Lock '{}' acquired successfully", lockName);
    }

    @Override
    public boolean acquire(Duration waitDuration) throws Exception {
        Objects.requireNonNull(waitDuration, "wait duration must not be null");

        checkState();
        logger.debug("Attempting to acquire lock '{}' with timeout {}", lockName, waitDuration);
        Instant deadline = Instant.now().plus(waitDuration);
        boolean acquired = lockInternals.tryAcquire(
                deadline,
                true,
                null
        ) != null;
        logger.debug("Lock '{}' acquisition {}successful", lockName, acquired ? "" : "un");
        return acquired;
    }

    @Override
    public boolean release() throws InterruptedException {
        checkState();
        logger.debug("Attempting to release lock '{}'", lockName);
        boolean released = lockInternals.release();
        if (released) {
            logger.debug("Lock '{}' released successfully", lockName);
        } else {
            logger.debug("No lock to release");
        }
        return released;
    }

    @Override
    public boolean isAcquiredInThisProcess() {
        try {
            checkState();
            boolean acquired = lockInternals.isAcquired();
            logger.trace("Lock acquisition check: {}", acquired);
            return acquired;
        } catch (LockException e) {
            logger.debug("Lock state check failed", e);
            return false;
        }
    }

    @Override
    public Listenable<CoordinationSession.State> getSessionListenable() {
        return sessionListenable;
    }

    @Override
    public void close() {
        logger.debug("Closing mutex '{}'", lockName);
        state.set(State.CLOSED);
        try {
            lockInternals.close();
        } catch (Exception e) {
            logger.warn("Error while closing lock internals '{}'", lockName, e);
        }
        logger.info("Mutex '{}' closed", lockName);
    }

    private void checkState() throws LockStateException {
        State currentState = state.get();
        if (currentState == State.FAILED) {
            throw new LockStateException("Lock '" + lockName + "' is in FAILED state", lockName);
        }
        if (currentState == State.CLOSED) {
            throw new LockStateException("Lock '" + lockName + "' is already closed", lockName);
        }
        if (currentState != State.STARTED) {
            throw new LockStateException("Lock '" + lockName + "' is not ready (current state: " + currentState + ")",
                    lockName);
        }
    }
}
