package tech.ydb.table.impl.pool;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

import tech.ydb.core.Result;
import tech.ydb.table.Session;

import static java.util.concurrent.CompletableFuture.completedFuture;

/**
 * @author Sergey Polovko
 */
class ResourceHandler implements PooledObjectHandler<Resource> {
    private final AtomicInteger idSeq = new AtomicInteger();

    @Override
    public CompletableFuture<Resource> create(long deadlineAfter) {
        return completedFuture(new Resource(idSeq.incrementAndGet()));
    }

    @Override
    public CompletableFuture<Void> destroy(Resource r) {
        r.markDestroyed();
        return completedFuture(null);
    }

    @Override
    public boolean isValid(Resource r) {
        return r.getState() == Resource.State.OK;
    }

    @Override
    public CompletableFuture<Result<Session.State>> keepAlive(Resource r) {
        r.markKeepAlived();
        return completedFuture(Result.success(Session.State.READY));
    }
}
