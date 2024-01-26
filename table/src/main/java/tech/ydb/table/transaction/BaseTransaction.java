package tech.ydb.table.transaction;

import java.util.concurrent.CompletableFuture;

/**
 * @author Nikolay Perfilov
 */
public interface BaseTransaction {

    String getId();

    String getSessionId();

    void addOnRollbackAction(Runnable action);

    void addFutureToWaitBeforeCommit(CompletableFuture<?> future);
}
