package tech.ydb.coordination.recipes.locks.exception;

/**
 * Thrown when lock initialization fails
 */
public class LockInitializationException extends LockException {
    public LockInitializationException(String lockName) {
        super("Failed to initialize lock '" + lockName + "'", lockName);
    }

    public LockInitializationException(String message, String lockName) {
        super(message, lockName);
    }

    public LockInitializationException(String message, Throwable cause, String lockName) {
        super(message, cause, lockName);
    }
}
