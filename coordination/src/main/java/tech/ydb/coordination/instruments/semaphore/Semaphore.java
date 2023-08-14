package tech.ydb.coordination.instruments.semaphore;

import java.util.concurrent.CompletableFuture;

public interface Semaphore {
    boolean acquire(SemaphoreSettings settings);

    CompletableFuture<Boolean> acquireAsync(SemaphoreSettings settings);

    boolean release();

    CompletableFuture<Boolean> releaseAsync();

    boolean isAcquired();
}
