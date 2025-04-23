package tech.ydb.coordination.recipes.locks;

public class LockAlreadyAcquiredException extends LockAcquireFailedException {
    public LockAlreadyAcquiredException(String coordinationNodePath, String semaphoreName) {
        super(
                "Lock=" + semaphoreName + " on path=" + coordinationNodePath + " is already acquired",
                coordinationNodePath,
                semaphoreName
        );
    }
}
