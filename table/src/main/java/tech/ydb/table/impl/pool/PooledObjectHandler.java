package tech.ydb.table.impl.pool;

import java.util.concurrent.CompletableFuture;


/**
 * @author Sergey Polovko
 */
public interface PooledObjectHandler<T> {

    CompletableFuture<T> create(long deadlineAfter);

    CompletableFuture<Void> destroy(T object);

    boolean isValid(T object);

    CompletableFuture<Void> keepAlive(T object);
}
