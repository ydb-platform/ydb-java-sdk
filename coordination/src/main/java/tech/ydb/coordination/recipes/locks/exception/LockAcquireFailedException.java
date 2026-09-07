package tech.ydb.coordination.recipes.locks.exception;

/**
 * Thrown when lock acquisition fails (excluding cases when lock is already acquired)
 */
public class LockAcquireFailedException extends LockException {
    public LockAcquireFailedException(String lockName) {
        super("Failed to acquire lock '" + lockName + "'", lockName);
    }

    public LockAcquireFailedException(String message, String lockName) {
        super(message, lockName);
    }

    public LockAcquireFailedException(String message, Throwable cause, String lockName) {
        super(message, cause, lockName);
    }
}
