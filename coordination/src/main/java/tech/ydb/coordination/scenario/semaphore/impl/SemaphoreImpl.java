package tech.ydb.coordination.scenario.semaphore.impl;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import tech.ydb.coordination.CoordinationClient;
import tech.ydb.coordination.CoordinationSession;
import tech.ydb.coordination.scenario.semaphore.Semaphore;
import tech.ydb.coordination.scenario.semaphore.settings.SemaphoreSettings;
import tech.ydb.core.Status;


public class SemaphoreImpl extends AsyncSemaphoreImpl implements Semaphore {

    protected SemaphoreImpl(CoordinationSession session, SemaphoreObserver observer) {
        super(session, observer);
    }

    public static CompletableFuture<Semaphore> newSemaphore(
            CoordinationClient client, String path, String semaphoreName, long limit) {
        final CompletableFuture<Semaphore> creationFuture = new CompletableFuture<>();
        prepareSessionAndCreateCoordinationSemaphore(client, path, semaphoreName, limit, creationFuture)
                .handle((pair, ex) -> {
                    if (ex == null) {
                        return creationFuture.complete(new SemaphoreImpl(pair.getKey(), pair.getValue()));
                    } else {
                        return creationFuture.completeExceptionally(ex);
                    }
                });
        return creationFuture;
    }

    public static Status deleteSemaphore(CoordinationClient client, String path,
                                         String semaphoreName, boolean force) {
        return AsyncSemaphoreImpl.deleteSemaphoreAsync(client, path, semaphoreName, force).join();
    }

    @Override
    public boolean acquire(SemaphoreSettings settings) {
        try {
            return acquireAsync(settings).get(settings.getTimeout(), TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            return false;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean release() {
        return releaseAsync().join();
    }
}
