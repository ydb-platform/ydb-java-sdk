package tech.ydb.table.impl;

import java.util.concurrent.CompletableFuture;

import tech.ydb.common.transaction.impl.BaseTransactionImpl;
import tech.ydb.core.Issue;
import tech.ydb.core.Status;
import tech.ydb.core.StatusCode;
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
        CompletableFuture<Status> result;
        if (futuresToWaitBeforeCommit.isEmpty()) {
            result = session.commitTransaction(txId, settings);
        } else {
            result = CompletableFuture.allOf(futuresToWaitBeforeCommit.toArray(new CompletableFuture<?>[0]))
                    .thenCompose((unused) -> session.commitTransaction(txId, settings));
        }
        result.whenComplete(((status, throwable) -> {
            if (throwable != null) {
                statusFuture.completeExceptionally(throwable);
            } else {
                statusFuture.complete(status);
            }
        }));
        return result;
    }

    @Override
    public CompletableFuture<Status> rollback(RollbackTxSettings settings) {
        return session.rollbackTransaction(txId, settings)
                .whenComplete((status, throwable) -> statusFuture.complete(Status
                        .of(StatusCode.ABORTED)
                        .withIssues(Issue.of("Transaction was rolled back", Issue.Severity.ERROR))));
    }
}
