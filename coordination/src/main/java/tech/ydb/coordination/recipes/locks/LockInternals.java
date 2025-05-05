package tech.ydb.coordination.recipes.locks;

import java.io.Closeable;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tech.ydb.coordination.CoordinationClient;
import tech.ydb.coordination.CoordinationSession;
import tech.ydb.coordination.SemaphoreLease;
import tech.ydb.coordination.recipes.util.ListenableContainer;
import tech.ydb.coordination.recipes.util.Listenable;
import tech.ydb.coordination.recipes.util.SessionListenableProvider;
import tech.ydb.core.Result;
import tech.ydb.core.Status;
import tech.ydb.core.StatusCode;

public class LockInternals implements Closeable, SessionListenableProvider {
    private static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(30);

    private static final Logger logger = LoggerFactory.getLogger(LockInternals.class);

    private final boolean persistent;
    private final long maxPersistentLease;
    private final String coordinationNodePath;
    private final String semaphoreName;
    private final CoordinationSession session;
    private final ListenableContainer<CoordinationSession.State> sessionListenable = new ListenableContainer();

    private CompletableFuture<Status> sessionConnectionTask = null;
    private volatile LeaseData leaseData = null;

    public static class LeaseData {
        private final SemaphoreLease processLease;
        private final boolean exclusive;
        private final long leaseSessionId;

        public LeaseData(SemaphoreLease processLease, boolean exclusive, long leaseSessionId) {
            this.processLease = processLease;
            this.exclusive = exclusive;
            this.leaseSessionId = leaseSessionId;
        }

        public boolean isExclusive() {
            return exclusive;
        }

        public SemaphoreLease getProcessLease() {
            return processLease;
        }

        public long getLeaseSessionId() {
            return leaseSessionId;
        }

        @Override
        public String toString() {
            return "LeaseData{" +
                    "processLease=" + processLease +
                    ", isExclusive=" + exclusive +
                    ", leaseSessionId=" + leaseSessionId +
                    '}';
        }
    }

    public LockInternals(
            CoordinationSession session,
            String lockName,
            long maxPersistentLease
    ) {
        this.persistent = false;
        this.maxPersistentLease = -1;
        this.coordinationNodePath = coordinationNodePath;
        this.semaphoreName = lockName;
        this.session = client.createSession(coordinationNodePath);
    }

    public LockInternals(
            CoordinationClient client,
            String coordinationNodePath,
            String lockName
    ) {
        this.persistent = false;
        this.maxPersistentLease = -1;
        this.coordinationNodePath = coordinationNodePath;
        this.semaphoreName = lockName;
        this.session = client.createSession(coordinationNodePath);
    }

    public LockInternals(
            long maxPersistentLease,
            CoordinationClient client,
            String coordinationNodePath,
            String lockName
    ) {
        this.persistent = true;
        this.maxPersistentLease = maxPersistentLease;
        this.coordinationNodePath = coordinationNodePath;
        this.semaphoreName = lockName;
        this.session = client.createSession(coordinationNodePath);
    }

    public void start() {
        this.sessionConnectionTask = session.connect().thenApply(status -> {
            logger.debug("Session connection status: {}", status);
            return status;
        });

        Consumer<CoordinationSession.State> listener = state -> {
            switch (state) {
                case RECONNECTED: {
                    logger.debug("Session RECONNECTED");
                    reconnect();
                    break;
                }
                case CLOSED: {
                    logger.debug("Session CLOSED, releasing lock");
                    leaseData = null;
                    break;
                }
                case LOST: {
                    logger.debug("Session LOST, releasing lock");
                    leaseData = null;
                    break;
                }
                default:
            }
            sessionListenable.notifyListeners(state);
        };

        session.addStateListener(listener);
    }

    private CoordinationSession connectedSession() {
        if (sessionConnectionTask == null) {
            throw new IllegalStateException("Not started yet");
        }
        try {
            sessionConnectionTask.get().expectSuccess("Unable to connect to session on: " + coordinationNodePath);
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
        return session;
    }

    private void reconnect() {
        LeaseData currentLeaseData = leaseData;
        long oldId = currentLeaseData.getLeaseSessionId();
        long newId = session.getId();
        if (oldId != newId) {
            logger.warn(
                    "Current session with new id: {} lost lease after reconnection on semaphore: {}",
                    newId,
                    semaphoreName
            );
            leaseData = null;
        } else {
            logger.debug("Successfully reestablished session with same id: {}", newId);
        }
    }

    public synchronized boolean release() throws LockReleaseFailedException, InterruptedException {
        logger.debug("Trying to release semaphore '{}'", semaphoreName);

        if (!connectedSession().getState().isActive()) {
            throw new LockReleaseFailedException(
                    "Coordination session is inactive",
                    coordinationNodePath,
                    semaphoreName
            );
        }

        LeaseData localLeaseData = leaseData;
        if (localLeaseData == null) {
            logger.debug("Semaphore '{}' already released", semaphoreName);
            return false;
        }

        try {
            localLeaseData.getProcessLease().release().get();
            leaseData = null;
            logger.debug("Successfully released semaphore '{}'", semaphoreName);
            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw e;
        } catch (ExecutionException e) {
            throw new LockReleaseFailedException(
                    "Failed to release lock: " + e.getCause().getMessage(),
                    coordinationNodePath,
                    semaphoreName,
                    e.getCause()
            );
        }
    }

    public synchronized LeaseData tryAcquire(
            @Nullable Instant deadline,
            boolean exclusive,
            byte @Nullable [] data
    ) throws Exception {
        logger.debug("Trying to acquire lock: {} with deadline: {}, exclusive: {}", semaphoreName, deadline, exclusive);

        if (leaseData != null) {
            throw new LockAlreadyAcquiredException(coordinationNodePath, semaphoreName);
        }

        Optional<SemaphoreLease> lease = tryBlockingLock(deadline, exclusive, data);
        if (lease.isPresent()) {
            LeaseData localLeaseData = new LeaseData(lease.get(), exclusive, 1);
            leaseData = localLeaseData;
            logger.debug("Successfully acquired lock: {}", semaphoreName);
            return localLeaseData;
        }

        logger.debug("Unable to acquire lock: {}", semaphoreName);
        return null;
    }

    private Optional<SemaphoreLease> tryBlockingLock(
            @Nullable Instant deadline,
            boolean exclusive,
            byte @Nullable [] data
    ) throws Exception {
        int retryCount = 0;
        CoordinationSession coordinationSession = connectedSession();

        while (coordinationSession.getState().isActive() && (deadline == null || Instant.now().isBefore(deadline))) {
            retryCount++;

            Duration timeout;
            if (deadline == null) {
                timeout = DEFAULT_TIMEOUT;
            } else {
                timeout = Duration.between(Instant.now(), deadline);
            }

            CompletableFuture<Result<SemaphoreLease>> acquireTask = acquire(
                    exclusive, data, coordinationSession, timeout
            );

            Result<SemaphoreLease> leaseResult;
            try {
                leaseResult = acquireTask.get();
            } catch (InterruptedException e) {
                // If acquire is interrupted, then release immediately
                Thread.currentThread().interrupt();
                acquireTask.thenAccept(acquireResult -> {
                    if (!acquireResult.getStatus().isSuccess()) {
                        return;
                    }
                    SemaphoreLease lease = acquireResult.getValue();
                    lease.release();
                });
                throw e;
            }

            Status status = leaseResult.getStatus();
            logger.debug("Lease result status: {}", status);

            if (status.isSuccess()) {
                logger.debug("Successfully acquired the lock '{}'", semaphoreName);
                return Optional.of(leaseResult.getValue());
            }

            if (status.getCode() == StatusCode.TIMEOUT) {
                logger.debug("Trying to acquire semaphore {} again, retries: {}", semaphoreName, retryCount);
                continue;
            }

            if (!status.getCode().isRetryable(true)) {
                logger.debug("Unable to retry acquiring semaphore '{}'", semaphoreName);
                throw new LockAcquireFailedException(coordinationNodePath, semaphoreName);
            }
        }

        if (deadline != null && Instant.now().compareTo(deadline) >= 0) {
            return Optional.empty();
        }

        throw new LockAcquireFailedException(coordinationNodePath, semaphoreName);
    }

    private CompletableFuture<Result<SemaphoreLease>> acquire(
            boolean exclusive,
            byte[] data,
            CoordinationSession coordinationSession,
            Duration timeout
    ) {
        if (!persistent) {
            return coordinationSession.acquireEphemeralSemaphore(semaphoreName, exclusive, data, timeout);
        }

        if (exclusive) {
            return coordinationSession.acquireSemaphore(semaphoreName, maxPersistentLease, data, timeout);
        }

        return coordinationSession.acquireSemaphore(semaphoreName, 1, data, timeout);
    }

    public String getCoordinationNodePath() {
        return coordinationNodePath;
    }

    public String getSemaphoreName() {
        return semaphoreName;
    }

    public CoordinationSession getCoordinationSession() {
        return session;
    }

    public CoordinationSession getConnectedCoordinationSession() {
        return connectedSession();
    }

    public @Nullable LeaseData getLeaseData() {
        return leaseData;
    }

    public boolean isAcquired() {
        return leaseData != null;
    }

    public boolean isPersistent() {
        return persistent;
    }

    @Override
    public Listenable<CoordinationSession.State> getSessionListenable() {
        return sessionListenable;
    }

    @Override
    public void close() {
        try {
            release();
        } catch (Exception ignored) {
        }

        session.close();
    }
}
