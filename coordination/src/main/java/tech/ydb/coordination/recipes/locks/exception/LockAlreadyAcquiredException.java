package tech.ydb.coordination.recipes.locks.exception;

/**
 * Thrown when attempting to acquire a lock that is already held by current process
 */
public class LockAlreadyAcquiredException extends LockException {
    public LockAlreadyAcquiredException(String lockName) {
        super("Lock '" + lockName + "' is already acquired by this process", lockName);
    }

    public LockAlreadyAcquiredException(String message, String lockName) {
        super(message, lockName);
    }

    public LockAlreadyAcquiredException(String message, Throwable cause, String lockName) {
        super(message, cause, lockName);
    }
}
