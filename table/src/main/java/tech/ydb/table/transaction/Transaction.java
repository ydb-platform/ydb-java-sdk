package tech.ydb.table.transaction;

import java.util.concurrent.CompletableFuture;

import tech.ydb.core.Status;
import tech.ydb.table.settings.CommitTxSettings;
import tech.ydb.table.settings.RollbackTxSettings;


/**
 * @author Sergey Polovko
 */
public interface Transaction {
    public enum Mode {
        SERIALIZABLE_READ_WRITE,
        ONLINE_READ_ONLY,
        STALE_READ_ONLY,
        ;
    }

    String getId();

    CompletableFuture<Status> commit(CommitTxSettings settings);
    CompletableFuture<Status> rollback(RollbackTxSettings settings);

    default CompletableFuture<Status> commit() {
        return commit(new CommitTxSettings());
    }

    default CompletableFuture<Status> rollback() {
        return rollback(new RollbackTxSettings());
    }
}
