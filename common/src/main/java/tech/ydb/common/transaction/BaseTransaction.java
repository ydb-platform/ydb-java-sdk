package tech.ydb.common.transaction;

import java.util.concurrent.CompletableFuture;

import tech.ydb.core.Status;

/**
 * @author Nikolay Perfilov
 */
public interface BaseTransaction {

    String getId();

    String getSessionId();

    CompletableFuture<Status> getStatusFuture();
}
