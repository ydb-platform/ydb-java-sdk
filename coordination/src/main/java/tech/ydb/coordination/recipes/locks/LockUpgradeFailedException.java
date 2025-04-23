package tech.ydb.coordination.recipes.locks;

public class LockUpgradeFailedException extends LockAcquireFailedException {
    public LockUpgradeFailedException(String coordinationNodePath, String semaphoreName) {
        super(
                "Unable to upgrade lease from inclusive to exclusive, " +
                        "name=" + semaphoreName + ", " + "path=" + coordinationNodePath,
                coordinationNodePath,
                semaphoreName
        );
    }
}
