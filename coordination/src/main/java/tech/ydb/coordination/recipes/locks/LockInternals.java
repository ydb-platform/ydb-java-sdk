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

import tech.ydb.coordination.CoordinationSession;
import tech.ydb.coordination.SemaphoreLease;
import tech.ydb.coordination.recipes.locks.exception.LockAcquireFailedException;
import tech.ydb.coordination.recipes.locks.exception.LockAlreadyAcquiredException;
import tech.ydb.coordination.recipes.locks.exception.LockReleaseFailedException;
import tech.ydb.core.Result;
import tech.ydb.core.Status;
import tech.ydb.core.StatusCode;

public class LockInternals implements Closeable {
    private static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(30);

    private static final Logger logger = LoggerFactory.getLogger(LockInternals.class);

    private final boolean persistent;
    private final long maxPersistentLease;
    private final String lockName;
    private final CoordinationSession coordinationSession;

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
            CoordinationSession coordinationSession,
            String lockName
    ) {
        this(coordinationSession, lockName, null);
    }

    public LockInternals(
            CoordinationSession coordinationSession,
            String lockName,
            @Nullable Long maxPersistentLease
    ) {
        if (maxPersistentLease == null) {
            this.persistent = false;
            this.maxPersistentLease = -1;
        } else {
            this.persistent = true;
            this.maxPersistentLease = maxPersistentLease;
        }

        this.lockName = lockName;
        this.coordinationSession = coordinationSession;
        this.coordinationSession.addStateListener(new Consumer<CoordinationSession.State>() {
            @Override
            public void accept(CoordinationSession.State state) {
                switch (state) {
                    case RECONNECTED:
                        onReconnect();
                        break;
                    case CLOSED:
                    case LOST:
                        leaseData = null;
                        break;
                    default:
                }
            }
        });
    }

    private void onReconnect() {
        LeaseData currentLeaseData = leaseData;
        long oldId = currentLeaseData.getLeaseSessionId();
        long newId = coordinationSession.getId();
        if (oldId != newId) {
            logger.warn(
                    "Current session with new id: {} lost lease after reconnection on semaphore: {}",
                    newId,
                    lockName
            );
            leaseData = null;
        } else {
            logger.debug("Successfully reestablished session with same id: {}", newId);
        }
    }

    public synchronized boolean release() throws LockReleaseFailedException, InterruptedException {
        logger.debug("Trying to release semaphore '{}'", lockName);

        if (!coordinationSession.getState().isActive()) {
            throw new LockReleaseFailedException(
                    "Coordination session is inactive",
                    lockName
            );
        }

        LeaseData localLeaseData = leaseData;
        if (localLeaseData == null) {
            logger.debug("Semaphore '{}' already released", lockName);
            return false;
        }

        try {
            localLeaseData.getProcessLease().release().get();
            leaseData = null;
            logger.debug("Successfully released semaphore '{}'", lockName);
            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw e;
        } catch (ExecutionException e) {
            throw new LockReleaseFailedException(
                    "Failed to release lock: " + e.getCause().getMessage(),
                    e.getCause(),
                    lockName
            );
        }
    }

    public synchronized LeaseData tryAcquire(
            @Nullable Instant deadline,
            boolean exclusive,
            byte @Nullable [] data
    ) throws Exception {
        logger.debug("Trying to acquire lock: {} with deadline: {}, exclusive: {}", lockName, deadline, exclusive);

        if (leaseData != null) {
            throw new LockAlreadyAcquiredException(lockName);
        }

        Optional<SemaphoreLease> lease = tryBlockingLock(deadline, exclusive, data);
        if (lease.isPresent()) {
            LeaseData localLeaseData = new LeaseData(lease.get(), exclusive, 1);
            leaseData = localLeaseData;
            logger.debug("Successfully acquired lock: {}", lockName);
            return localLeaseData;
        }

        logger.debug("Unable to acquire lock: {}", lockName);
        return null;
    }

    private Optional<SemaphoreLease> tryBlockingLock(
            @Nullable Instant deadline,
            boolean exclusive,
            byte @Nullable [] data
    ) throws Exception {
        int retryCount = 0;
        while (coordinationSession.getState().isActive() && (deadline == null || Instant.now().isBefore(deadline))) {
            retryCount++;

            Duration timeout;
            if (deadline == null) {
                timeout = DEFAULT_TIMEOUT;
            } else {
                timeout = Duration.between(Instant.now(), deadline);
            }

            CompletableFuture<Result<SemaphoreLease>> acquireTask = acquireCall(exclusive, data, timeout);

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
                logger.debug("Successfully acquired the lock '{}'", lockName);
                return Optional.of(leaseResult.getValue());
            }

            if (status.getCode() == StatusCode.TIMEOUT) {
                logger.debug("Trying to acquire semaphore {} again, retries: {}", lockName, retryCount);
                continue;
            }

            if (!status.getCode().isRetryable(true)) {
                logger.debug("Unable to retry acquiring semaphore '{}'", lockName);
                throw new LockAcquireFailedException(lockName);
            }
        }

        if (deadline != null && Instant.now().compareTo(deadline) >= 0) {
            return Optional.empty();
        }

        throw new LockAcquireFailedException(lockName);
    }

    private CompletableFuture<Result<SemaphoreLease>> acquireCall(
            boolean exclusive,
            byte[] data,
            Duration timeout
    ) {
        if (!persistent) {
            return coordinationSession.acquireEphemeralSemaphore(lockName, exclusive, data, timeout);
        }

        if (exclusive) {
            return coordinationSession.acquireSemaphore(lockName, maxPersistentLease, data, timeout);
        }

        return coordinationSession.acquireSemaphore(lockName, 1, data, timeout);
    }

    public String getLockName() {
        return lockName;
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
    public void close() {
        try {
            release();
        } catch (Exception exception) {
            logger.error("Exception during closing release", exception);
        }
    }
}
