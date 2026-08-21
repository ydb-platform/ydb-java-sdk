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
import tech.ydb.coordination.recipes.locks.exception.LockInitializationException;
import tech.ydb.coordination.recipes.locks.exception.LockStateException;
import tech.ydb.coordination.recipes.util.Listenable;
import tech.ydb.coordination.recipes.util.ListenableContainer;

/**
 * Distributed read-write lock implementation that allows multiple readers or a single writer.
 *
 * <p>This implementation provides non-reentrant read/write locking semantics across multiple processes.
 * Multiple processes can hold the read lock simultaneously, while only one process can hold
 * the write lock (with no concurrent readers).</p>
 *
 * <p>Thread-safety: Instances of this class are thread-safe and can be used from multiple threads.</p>
 */
public class ReadWriteInterProcessLock implements Closeable {
    private static final Logger logger = LoggerFactory.getLogger(ReadWriteInterProcessLock.class);

    private final InternalLock readLock;
    private final InternalLock writeLock;

    /**
     * Creates a new distributed read-write lock with default settings.
     *
     * @param client               the coordination client instance
     * @param coordinationNodePath the base path for coordination nodes
     * @param lockName             the name of the lock (must be unique within the coordination namespace)
     * @throws IllegalArgumentException if any parameter is null
     */
    public ReadWriteInterProcessLock(
            CoordinationClient client,
            String coordinationNodePath,
            String lockName
    ) {
        this(
                client,
                coordinationNodePath,
                lockName,
                ReadWriteInterProcessLockSettings.newBuilder().build()
        );
    }

    /**
     * Creates a new distributed read-write lock with custom settings.
     *
     * @param client               the coordination client instance
     * @param coordinationNodePath the base path for coordination nodes
     * @param lockName             the name of the lock (must be unique within the coordination namespace)
     * @param settings             the lock configuration settings
     * @throws IllegalArgumentException    if any parameter is null
     * @throws LockInitializationException if the lock cannot be initialized
     */
    public ReadWriteInterProcessLock(
            CoordinationClient client,
            String coordinationNodePath,
            String lockName,
            ReadWriteInterProcessLockSettings settings
    ) {
        if (client == null || coordinationNodePath == null || lockName == null || settings == null) {
            throw new IllegalArgumentException("All parameters must be non-null");
        }

        this.readLock = new InternalLock(
                client,
                coordinationNodePath,
                lockName,
                false
        );
        this.writeLock = new InternalLock(
                client,
                coordinationNodePath,
                lockName,
                true
        );

        if (settings.isWaitConnection()) {
            try {
                logger.debug("Waiting for session connection to complete for rwlock {}", lockName);
                readLock.sessionConnectionTask.get();
                writeLock.sessionConnectionTask.get();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                logger.error("Interrupted while waiting for session connection for rwlock '{}'", lockName, e);
                throw new LockInitializationException(
                        "Interrupted while initializing rwlock '" + lockName + "'", e, lockName
                );
            } catch (ExecutionException e) {
                logger.error("Failed to initialize rwlock '{}' due to execution error", lockName, e);
                throw new LockInitializationException(
                        "Failed to initialize rwlock '" + lockName + "'", e.getCause(), lockName
                );
            }
        }
    }

    /**
     * Returns the write lock instance.
     *
     * @return the write lock (exclusive)
     */
    public InterProcessLock writeLock() {
        return writeLock;
    }

    /**
     * Returns the read lock instance.
     *
     * @return the read lock (shared)
     */
    public InterProcessLock readLock() {
        return readLock;
    }

    /**
     * Internal lock implementation that handles both read and write operations.
     */
    private static class InternalLock implements InterProcessLock {
        private final String lockName;
        private final boolean isExclusive;
        private final Future<?> sessionConnectionTask;
        private final CoordinationSession coordinationSession;
        private final LockInternals lockInternals;
        private final ListenableContainer<CoordinationSession.State> sessionListenable;

        private final AtomicReference<State> state = new AtomicReference<>(State.INITIAL);

        /**
         * Internal state of the lock.
         */
        private enum State {
            INITIAL,
            STARTING,
            STARTED,
            FAILED,
            CLOSED
        }

        InternalLock(
                CoordinationClient client,
                String coordinationNodePath,
                String lockName,
                boolean isExclusive
        ) {
            state.set(State.STARTING);
            logger.debug("Initializing InterProcessMutex for lock '{}'", lockName);

            this.lockName = lockName;
            this.coordinationSession = client.createSession(coordinationNodePath);
            this.sessionListenable = new ListenableContainer<>();
            this.lockInternals = new LockInternals(coordinationSession, lockName);
            this.isExclusive = isExclusive;

            coordinationSession.addStateListener(sessionState -> {
                if (sessionState == CoordinationSession.State.LOST ||
                        sessionState == CoordinationSession.State.CLOSED) {
                    logger.error("Coordination session unexpectedly changed to {} state, marking lock as FAILED",
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
        }

        @Override
        public void acquire() throws Exception {
            checkState();
            logger.debug("Attempting to acquire lock {}", lockName);
            lockInternals.tryAcquire(
                    null,
                    isExclusive,
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
                    isExclusive,
                    null
            ) != null;
            logger.debug("Lock '{}' acquisition {}successful", lockName, acquired ? "" : "un");
            return acquired;
        }

        @Override
        public boolean release() throws Exception {
            checkState();
            logger.debug("Attempting to release lock '{}'", lockName);
            boolean released = lockInternals.release();
            if (released) {
                logger.debug("Lock {} released successfully", lockName);
            } else {
                logger.debug("No lock to release");
            }
            return released;
        }

        @Override
        public boolean isAcquiredInThisProcess() {
            return lockInternals.isAcquired();
        }

        @Override
        public Listenable<CoordinationSession.State> getSessionListenable() {
            return sessionListenable;
        }

        private void close() {
            logger.debug("Closing rwlock {}", lockName);
            state.set(State.CLOSED);
            try {
                lockInternals.close();
            } catch (Exception e) {
                logger.warn("Error while closing rwlock internals {}", lockName, e);
            }
            logger.info("Rwlock {} closed", lockName);
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
                throw new LockStateException(
                        "Lock '" + lockName + "' is not ready (current state: " + currentState + ")", lockName
                );
            }
        }
    }

    /**
     * Closes both read and write locks and releases all associated resources.
     * After closing, the lock instance can no longer be used.
     */
    @Override
    public void close() {
        readLock.close();
        writeLock.close();
    }
}
