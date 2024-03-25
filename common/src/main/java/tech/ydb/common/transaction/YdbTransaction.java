package tech.ydb.common.transaction;

import java.util.concurrent.CompletableFuture;

import tech.ydb.core.Status;

/**
 * A base interface for all YDB transactions from different services
 * @author Nikolay Perfilov
 */
public interface YdbTransaction {

    /**
     * Returns identifier of the transaction or null if the transaction is not active = (not
     * started/committed/rolled back). When {@link YdbTransaction} is not active - any query on this object
     * starts a new transaction on server. When transaction is active any call of {@code commit},
     * {@code rollback} or execution of any query with {@code commitAtEnd}=true finishes this transaction
     *
     * @return identifier of the transaction or null if the transaction is not active
     */
    String getId();

    /**
     * Returns {@link TxMode} with mode of the transaction
     *
     * @return the transaction mode
     */
    TxMode getTxMode();

    default boolean isActive() {
        return getId() != null;
    }

    String getSessionId();

    CompletableFuture<Status> getStatusFuture();
}
