package tech.ydb.table.impl;

import java.util.concurrent.CompletableFuture;

import tech.ydb.common.transaction.impl.BaseTransactionImpl;
import tech.ydb.core.Status;
import tech.ydb.table.Session;
import tech.ydb.table.settings.CommitTxSettings;
import tech.ydb.table.settings.RollbackTxSettings;
import tech.ydb.table.transaction.Transaction;


/**
 * @author Sergey Polovko
 */
public final class TransactionImpl extends BaseTransactionImpl implements Transaction {
    private final Session session;

    public TransactionImpl(Session session, String txId) {
        super(txId);
        this.session = session;
    }

    @Override
    public String getSessionId() {
        return session.getId();
    }

    @Override
    public CompletableFuture<Status> commit(CommitTxSettings settings) {
        if (futuresToWaitBeforeCommit.isEmpty()) {
            return session.commitTransaction(txId, settings);
        } else {
            return CompletableFuture.allOf(futuresToWaitBeforeCommit.toArray(new CompletableFuture<?>[0]))
                    .thenCompose((unused) -> session.commitTransaction(txId, settings));
        }
    }

    @Override
    public CompletableFuture<Status> rollback(RollbackTxSettings settings) {
        return session.rollbackTransaction(txId, settings)
                .whenComplete((status, throwable) -> onRollbackActions.forEach(Runnable::run));
    }
}
