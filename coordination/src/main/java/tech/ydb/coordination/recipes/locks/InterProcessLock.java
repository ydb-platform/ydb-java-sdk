package tech.ydb.coordination.recipes.locks;

import java.time.Duration;

import tech.ydb.coordination.recipes.locks.exception.LockAcquireFailedException;
import tech.ydb.coordination.recipes.locks.exception.LockAlreadyAcquiredException;
import tech.ydb.coordination.recipes.locks.exception.LockReleaseFailedException;
import tech.ydb.coordination.recipes.locks.exception.LockStateException;
import tech.ydb.coordination.recipes.util.SessionListenableProvider;

public interface InterProcessLock extends SessionListenableProvider {
    /**
     * Acquires the distributed lock, blocking until it is obtained.
     *
     * @throws Exception                    if an unexpected error occurs
     * @throws LockAlreadyAcquiredException if the lock is already acquired by this process
     * @throws LockAcquireFailedException   if the lock acquisition fails
     * @throws LockStateException           if the lock is in invalid state for acquisition
     */
    void acquire() throws Exception, LockAlreadyAcquiredException, LockAcquireFailedException, LockStateException;

    /**
     * Attempts to acquire the lock within the given waiting time.
     *
     * @param waitDuration maximum time to wait for the lock
     * @return true if the lock was acquired, false if the waiting time elapsed
     * @throws Exception                    if an unexpected error occurs
     * @throws LockAlreadyAcquiredException if the lock is already acquired by this process
     * @throws LockAcquireFailedException   if the lock acquisition fails
     * @throws LockStateException           if the lock is in invalid state for acquisition
     */
    boolean acquire(Duration waitDuration) throws Exception, LockAlreadyAcquiredException, LockAcquireFailedException,
            LockStateException;

    /**
     * Releases the lock if it is held by this process.
     *
     * @return false if there was nothing to release
     * @throws Exception                  if an unexpected error occurs
     * @throws LockReleaseFailedException if the lock release fails
     * @throws LockStateException         if the lock is in invalid state for release
     */
    boolean release() throws Exception, LockReleaseFailedException, LockStateException;


    /**
     * Checks if the lock is currently acquired by this process.
     *
     * @return true if the lock is acquired by this process
     */
    boolean isAcquiredInThisProcess();
}
