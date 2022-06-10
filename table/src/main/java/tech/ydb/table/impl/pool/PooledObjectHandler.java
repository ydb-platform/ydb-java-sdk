package tech.ydb.table.impl.pool;

import java.util.concurrent.CompletableFuture;

import tech.ydb.core.Result;
import tech.ydb.table.Session;

/**
 * @author Sergey Polovko
 */
public interface PooledObjectHandler<T> {

    CompletableFuture<T> create(long deadlineAfter);

    CompletableFuture<Void> destroy(T object);

    boolean isValid(T object);

    CompletableFuture<Result<Session.State>> keepAlive(T object);
}
