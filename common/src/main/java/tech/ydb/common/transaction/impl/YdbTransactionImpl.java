package tech.ydb.common.transaction.impl;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

import tech.ydb.common.transaction.TxMode;
import tech.ydb.common.transaction.YdbTransaction;
import tech.ydb.core.Status;

/**
 * @author Nikolay Perfilov
 */
public abstract class YdbTransactionImpl implements YdbTransaction {
    protected final TxMode txMode;
    protected final AtomicReference<String> txId;
    protected final AtomicReference<CompletableFuture<Status>> statusFuture = new AtomicReference<>(
            new CompletableFuture<>());

    protected YdbTransactionImpl(TxMode txMode, String txId) {
        this.txMode = txMode;
        this.txId = new AtomicReference<>(txId);
    }

    @Override
    public String getId() {
        return txId.get();
    }

    @Override
    public TxMode getTxMode() {
        return txMode;
    }

    @Override
    public CompletableFuture<Status> getStatusFuture() {
        return statusFuture.get();
    }
}
