package tech.ydb.coordination.recipes.locks;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.ydb.coordination.CoordinationClient;
import tech.ydb.coordination.CoordinationSession;
import tech.ydb.coordination.recipes.util.Listenable;
import tech.ydb.coordination.recipes.util.ListenableProvider;

import java.io.Closeable;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

public class ReadWriteInterProcessLock implements Closeable {
    private static final Logger logger = LoggerFactory.getLogger(ReadWriteInterProcessLock.class);

    private final LockInternals lockInternals;
    private final InternalLock readLock;
    private final InternalLock writeLock;

    public ReadWriteInterProcessLock(
            CoordinationClient client,
            String coordinationNodePath,
            String lockName
    ) {
        this.lockInternals = new LockInternals(
                client, coordinationNodePath, lockName
        );
        lockInternals.start();

        this.readLock = new InternalLock(lockInternals, false);
        this.writeLock = new InternalLock(lockInternals, true);
    }

    public InterProcessLock writeLock() {
        return writeLock;
    }

    public InterProcessLock readLock() {
        // TODO: Если сделали acquire для read lock, когда уже есть write lock? Сейчас игнорим
        return readLock;
    }

    private static class InternalLock implements InterProcessLock, ListenableProvider<CoordinationSession.State> {
        private final LockInternals lockInternals;
        private final boolean isExclusive;

        private InternalLock(LockInternals lockInternals, boolean isExclusive) {
            this.lockInternals = lockInternals;
            this.isExclusive = isExclusive;
        }

        @Override
        public void acquire() throws Exception {
            if (!isExclusive && isAcquired(true)) {
                logger.debug("Write lock acquired, skipping for read lock");
                return;
            }

            lockInternals.tryAcquire(
                    null,
                    isExclusive,
                    null
            );
        }

        @Override
        public boolean acquire(Duration waitDuration) throws Exception {
            Objects.requireNonNull(waitDuration, "wait duration must not be null");

            if (!isExclusive && isAcquired(true)) {
                logger.debug("Write lock acquired, skipping for read lock");
                return true;
            }

            Instant deadline = Instant.now().plus(waitDuration);
            return lockInternals.tryAcquire(
                    deadline,
                    isExclusive,
                    null
            ) != null;
        }

        @Override
        public boolean release() {
            if (!isAcquiredInThisProcess()) {
                return false;
            }

            return lockInternals.release();
        }

        @Override
        public boolean isAcquiredInThisProcess() {
            return isAcquired(isExclusive);
        }

        private boolean isAcquired(boolean exclusive) {
            LockInternals.LeaseData leaseData = lockInternals.getLeaseData();
            if (leaseData == null) {
                return false;
            }
            return leaseData.isExclusive() == exclusive;
        }

        @Override
        public Listenable<CoordinationSession.State> getListenable() {
            return lockInternals.getListenable();
        }
    }

    @Override
    public void close() {
        lockInternals.close();
    }

}
