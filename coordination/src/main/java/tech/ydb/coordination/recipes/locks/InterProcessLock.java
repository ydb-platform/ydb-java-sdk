package tech.ydb.coordination.recipes.locks;

import tech.ydb.coordination.CoordinationSession;
import tech.ydb.coordination.recipes.util.Listenable;

import java.time.Duration;

public interface InterProcessLock extends Listenable<CoordinationSession.State> {
    void acquire() throws Exception, LockAlreadyAcquiredException, LockAcquireFailedException;

    /**
     * @return true - if successfully acquired lock, false - if lock waiting time expired
     */
    boolean acquire(Duration waitDuration) throws Exception, LockAlreadyAcquiredException, LockAcquireFailedException;

    /**
     * @return false if nothing to release
     */
    boolean release() throws Exception;

    /**
     * @return true if the lock is acquired by a thread in this JVM
     */
    boolean isAcquiredInThisProcess();
}
