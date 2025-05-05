package tech.ydb.coordination.recipes.locks;

public class LockReleaseFailedException extends RuntimeException {
    private final String coordinationNodePath;
    private final String semaphoreName;

    public LockReleaseFailedException(String message, String coordinationNodePath, String semaphoreName, Throwable e) {
        super(
                "Failed to release semaphore=" + semaphoreName + ", on coordination node=" + coordinationNodePath +
                        ": '" + message + "'", e
        );
        this.coordinationNodePath = coordinationNodePath;
        this.semaphoreName = semaphoreName;
    }

    public LockReleaseFailedException(String message, String coordinationNodePath, String semaphoreName) {
        super(
                "Failed to release semaphore=" + semaphoreName + ", on coordination node=" + coordinationNodePath +
                        ": '" + message + "'"
        );
        this.coordinationNodePath = coordinationNodePath;
        this.semaphoreName = semaphoreName;
    }

    public LockReleaseFailedException(String coordinationNodePath, String semaphoreName) {
        super("Failed to release semaphore=" + semaphoreName + ", on coordination node=" + coordinationNodePath);
        this.coordinationNodePath = coordinationNodePath;
        this.semaphoreName = semaphoreName;
    }

    public String getCoordinationNodePath() {
        return coordinationNodePath;
    }

    public String getSemaphoreName() {
        return semaphoreName;
    }
}
