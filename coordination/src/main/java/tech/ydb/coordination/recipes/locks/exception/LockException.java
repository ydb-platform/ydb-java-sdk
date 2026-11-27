package tech.ydb.coordination.recipes.locks.exception;

public class LockException extends RuntimeException {
    protected final String lockName;

    public LockException(String lockName) {
        this.lockName = lockName;
    }

    public LockException(String message, String lockName) {
        super(message);
        this.lockName = lockName;
    }

    public LockException(String message, Throwable cause, String lockName) {
        super(message, cause);
        this.lockName = lockName;
    }

    public String getLockName() {
        return lockName;
    }
}
