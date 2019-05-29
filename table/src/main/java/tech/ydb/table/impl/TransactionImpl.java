package tech.ydb.table.impl;

import java.util.concurrent.CompletableFuture;

import tech.ydb.core.Status;
import tech.ydb.table.settings.CommitTxSettings;
import tech.ydb.table.settings.RollbackTxSettings;
import tech.ydb.table.transaction.Transaction;


/**
 * @author Sergey Polovko
 */
final class TransactionImpl implements Transaction {

    private final SessionImpl session;
    private final String txId;

    TransactionImpl(SessionImpl session, String txId) {
        this.session = session;
        this.txId = txId;
    }

    @Override
    public String getId() {
        return txId;
    }

    @Override
    public CompletableFuture<Status> commit(CommitTxSettings settings) {
        return session.commitTransaction(txId, settings);
    }

    @Override
    public CompletableFuture<Status> rollback(RollbackTxSettings settings) {
        return session.rollbackTransaction(txId, settings);
    }
}
