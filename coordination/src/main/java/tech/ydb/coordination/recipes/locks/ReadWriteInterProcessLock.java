package tech.ydb.coordination.recipes.locks;

import java.io.Closeable;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tech.ydb.coordination.CoordinationClient;
import tech.ydb.coordination.CoordinationSession;
import tech.ydb.coordination.recipes.util.Listenable;
import tech.ydb.coordination.recipes.util.ListenableContainer;

// TODO: state management + (документация, логи, рекомендации)
public class ReadWriteInterProcessLock implements Closeable {
    private static final Logger logger = LoggerFactory.getLogger(ReadWriteInterProcessLock.class);

    private final InternalLock readLock;
    private final InternalLock writeLock;

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

    public ReadWriteInterProcessLock(
            CoordinationClient client,
            String coordinationNodePath,
            String lockName,
            ReadWriteInterProcessLockSettings settings
    ) {
        this.readLock = new InternalLock(client, coordinationNodePath, lockName, false);
        this.writeLock = new InternalLock(client, coordinationNodePath, lockName, true);
    }

    public InterProcessLock writeLock() {
        return writeLock;
    }

    public InterProcessLock readLock() {
        return readLock;
    }

    private static class InternalLock implements InterProcessLock {
        private final boolean isExclusive;

        private final AtomicReference<State> state = new AtomicReference<>(State.INITIAL);
        private final CoordinationSession coordinationSession;
        private final LockInternals lockInternals;
        private final ListenableContainer<CoordinationSession.State> sessionListenable;

        private enum State {
            INITIAL,
            STARTING,
            STARTED,
            FAILED,
            CLOSED
        }

        public InternalLock(
                CoordinationClient client,
                String coordinationNodePath,
                String lockName,
                boolean isExclusive
        ) {
            this.isExclusive = isExclusive;

            this.coordinationSession = client.createSession(coordinationNodePath);
            this.lockInternals = new LockInternals(
                    coordinationSession,
                    lockName
            );
            this.sessionListenable = new ListenableContainer<>();
            coordinationSession.addStateListener(sessionListenable::notifyListeners);
            // TODO: add settings to block
            coordinationSession.connect().thenAccept(status -> {
                status.expectSuccess("Unable to establish session");
            });
        }

        private void start() {
            ;
        }

        @Override
        public void acquire() throws Exception {
            lockInternals.tryAcquire(
                    null,
                    isExclusive,
                    null
            );
        }

        @Override
        public boolean acquire(Duration waitDuration) throws Exception {
            Objects.requireNonNull(waitDuration, "wait duration must not be null");

            Instant deadline = Instant.now().plus(waitDuration);
            return lockInternals.tryAcquire(
                    deadline,
                    isExclusive,
                    null
            ) != null;
        }

        @Override
        public boolean release() throws InterruptedException {
            return lockInternals.release();
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
            lockInternals.close();
        }
    }

    @Override
    public void close() {
        readLock.close();
        writeLock.close();
    }

}
