package tech.ydb.coordination.recipes.locks;

import java.time.Duration;

import tech.ydb.coordination.recipes.util.SessionListenableProvider;

public interface InterProcessLock extends SessionListenableProvider {
    void acquire() throws Exception, LockAlreadyAcquiredException, LockAcquireFailedException;

    /**
     * @return true - if successfully acquired lock, false - if lock waiting time expired
     */
    boolean acquire(Duration waitDuration) throws Exception, LockAlreadyAcquiredException, LockAcquireFailedException;

    /**
     * @return false if nothing to release
     */
    boolean release() throws InterruptedException, LockReleaseFailedException;

    /**
     * @return true if the lock is acquired by a thread in this JVM
     */
    boolean isAcquiredInThisProcess();
}
