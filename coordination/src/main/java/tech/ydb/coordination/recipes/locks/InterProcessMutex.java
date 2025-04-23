package tech.ydb.coordination.recipes.locks;

import tech.ydb.coordination.CoordinationClient;
import tech.ydb.coordination.CoordinationSession;
import tech.ydb.coordination.recipes.util.Listenable;
import tech.ydb.coordination.recipes.util.ListenableProvider;

import javax.annotation.concurrent.ThreadSafe;
import java.io.Closeable;
import java.time.Duration;
import java.time.Instant;

@ThreadSafe
public class InterProcessMutex implements InterProcessLock, ListenableProvider<CoordinationSession.State>, Closeable {
    private final LockInternals lockInternals;

    public InterProcessMutex(
            CoordinationClient client,
            String coordinationNodePath,
            String lockName
    ) {
        lockInternals = new LockInternals(client, coordinationNodePath, lockName);
        lockInternals.start();
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
    public boolean release() throws Exception {
        return lockInternals.release();
    }

    @Override
    public boolean isAcquiredInThisProcess() {
        return lockInternals.isAcquired();
    }

    @Override
    public Listenable<CoordinationSession.State> getListenable() {
        return null;
    }

    @Override
    public void close() {
        lockInternals.close();
    }
}
