package tech.ydb.coordination.recipes.locks;

import java.io.Closeable;
import java.time.Duration;
import java.time.Instant;

import tech.ydb.coordination.CoordinationClient;
import tech.ydb.coordination.CoordinationSession;
import tech.ydb.coordination.recipes.util.Listenable;

// TODO: add documentation and logs
public class InterProcessMutex implements InterProcessLock, Closeable {
    private final LockInternals lockInternals;

    public InterProcessMutex(
            CoordinationClient client,
            String coordinationNodePath,
            String lockName,
            InterProcessMutexSettings settings
    ) {
        lockInternals = new LockInternals(client, coordinationNodePath, lockName);
        lockInternals.start();
        if (settings.isWaitConnection()) {
             lockInternals.getConnectedCoordinationSession();
        }
    }

    @Override
    public void acquire() throws Exception {
        lockInternals.tryAcquire(
                null,
                true,
                null
        );
    }

    @Override
    public boolean acquire(Duration waitDuration) throws Exception {
        Instant deadline = Instant.now().plus(waitDuration);
        return lockInternals.tryAcquire(
                deadline,
                true,
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
    public void close() {
        lockInternals.close();
    }

    @Override
    public Listenable<CoordinationSession.State> getSessionListenable() {
        return lockInternals.getSessionListenable();
    }
}
