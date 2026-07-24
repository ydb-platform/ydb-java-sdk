package tech.ydb.coordination.recipes.locks.exception;

/**
 * Thrown when lock operation is attempted in invalid state
 */
public class LockStateException extends LockException {
    public LockStateException(String lockName) {
        super("Invalid state for lock operation on lock '" + lockName + "'", lockName);
    }

    public LockStateException(String message, String lockName) {
        super(message, lockName);
    }

    public LockStateException(String message, Throwable cause, String lockName) {
        super(message, cause, lockName);
    }
}

