package tech.ydb.coordination.description;

import java.util.concurrent.CompletableFuture;

import tech.ydb.core.Result;

/**
 *
 * @author Aleksandr Gorshenin
 */
public class SemaphoreWatcher {
    private final SemaphoreDescription description;
    private final CompletableFuture<Result<SemaphoreChangedEvent>> changedFuture;

    public SemaphoreWatcher(SemaphoreDescription desc, CompletableFuture<Result<SemaphoreChangedEvent>> changed) {
        this.description = desc;
        this.changedFuture = changed;
    }

    public SemaphoreDescription getDescription() {
        return description;
    }

    public CompletableFuture<Result<SemaphoreChangedEvent>> getChangedFuture() {
        return changedFuture;
    }
}
