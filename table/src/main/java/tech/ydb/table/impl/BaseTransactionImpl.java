package tech.ydb.table.impl;

import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;

import tech.ydb.table.transaction.BaseTransaction;

/**
 * @author Nikolay Perfilov
 */
public abstract class BaseTransactionImpl implements BaseTransaction {
    protected final String txId;
    protected final Queue<Runnable> onRollbackActions = new ConcurrentLinkedQueue<>();
    protected final Queue<CompletableFuture<?>> futuresToWaitBeforeCommit = new ConcurrentLinkedQueue<>();

    BaseTransactionImpl(String txId) {
        this.txId = txId;
    }

    @Override
    public String getId() {
        return txId;
    }

    @Override
    public void addOnRollbackAction(Runnable action) {
        onRollbackActions.add(action);
    }

    @Override
    public void addFutureToWaitBeforeCommit(CompletableFuture<?> future) {
        futuresToWaitBeforeCommit.add(future);
    }

}
