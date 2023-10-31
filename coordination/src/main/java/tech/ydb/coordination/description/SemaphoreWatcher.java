package tech.ydb.coordination.description;

import java.util.concurrent.CompletableFuture;

/**
 *
 * @author Aleksandr Gorshenin
 */
public class SemaphoreWatcher {
    private final SemaphoreDescription description;
    private final CompletableFuture<SemaphoreChangedEvent> changedFuture;

    public SemaphoreWatcher(SemaphoreDescription description, CompletableFuture<SemaphoreChangedEvent> changedFuture) {
        this.description = description;
        this.changedFuture = changedFuture;
    }

    public SemaphoreDescription getDescription() {
        return description;
    }

    public CompletableFuture<SemaphoreChangedEvent> getChangedFuture() {
        return changedFuture;
    }
}
