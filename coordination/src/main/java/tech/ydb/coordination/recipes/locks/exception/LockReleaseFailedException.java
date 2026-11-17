package tech.ydb.coordination.recipes.locks.exception;

/**
 * Thrown when lock release operation fails
 */
public class LockReleaseFailedException extends LockException {
    public LockReleaseFailedException(String lockName) {
        super("Failed to release lock '" + lockName + "'", lockName);
    }

    public LockReleaseFailedException(String message, String lockName) {
        super(message, lockName);
    }

    public LockReleaseFailedException(String message, Throwable cause, String lockName) {
        super(message, cause, lockName);
    }
}
