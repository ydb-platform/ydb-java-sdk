package tech.ydb.coordination.scenario.semaphore.impl;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import tech.ydb.coordination.CoordinationClient;
import tech.ydb.coordination.scenario.semaphore.AsyncSemaphore;
import tech.ydb.coordination.scenario.semaphore.Semaphore;
import tech.ydb.coordination.scenario.semaphore.settings.SemaphoreSettings;
import tech.ydb.core.Status;


public class SemaphoreImpl extends AsyncSemaphoreImpl implements Semaphore {

    @SuppressWarnings("unchecked")
    protected SemaphoreImpl(CoordinationClient client, String nodePath, String semaphoreName, long limit,
                            CompletableFuture<? super Semaphore> initFuture) {
        super(client, nodePath, semaphoreName, limit, (CompletableFuture<? super AsyncSemaphore>) initFuture);
    }

    public static CompletableFuture<Semaphore> newSemaphore(
            CoordinationClient client, String path, String semaphoreName, long limit) {
        CompletableFuture<Semaphore> initFuture = new CompletableFuture<>();
        new SemaphoreImpl(client, path, semaphoreName, limit, initFuture);
        return initFuture;
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
