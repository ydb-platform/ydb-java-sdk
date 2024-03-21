package tech.ydb.common.transaction.impl;

import java.util.concurrent.CompletableFuture;

import tech.ydb.common.transaction.BaseTransaction;
import tech.ydb.core.Status;

/**
 * @author Nikolay Perfilov
 */
public abstract class BaseTransactionImpl implements BaseTransaction {
    protected final String txId;
    protected final CompletableFuture<Status> statusFuture = new CompletableFuture<>();

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
}
