package tech.ydb.common.transaction.impl;

import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;

import tech.ydb.common.transaction.BaseTransaction;
import tech.ydb.core.Status;

/**
 * @author Nikolay Perfilov
 */
public abstract class BaseTransactionImpl implements BaseTransaction {
    protected final String txId;
    protected final CompletableFuture<Status> statusFuture = new CompletableFuture<>();
    protected final Queue<CompletableFuture<?>> futuresToWaitBeforeCommit = new ConcurrentLinkedQueue<>();

    protected BaseTransactionImpl(String txId) {
        this.txId = txId;
    }

    @Override
    public String getId() {
        return txId;
    }

    @Override
    public CompletableFuture<Status> getStatusFuture() {
        return statusFuture;
    }

    @Override
    public void addFutureToWaitBeforeCommit(CompletableFuture<?> future) {
        futuresToWaitBeforeCommit.add(future);
    }

}
