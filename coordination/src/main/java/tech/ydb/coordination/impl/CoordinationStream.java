package tech.ydb.coordination.impl;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import javax.annotation.Nonnull;

import tech.ydb.coordination.description.SemaphoreChangedEvent;
import tech.ydb.coordination.description.SemaphoreDescription;
import tech.ydb.core.Result;
import tech.ydb.core.Status;

public interface CoordinationStream {
    CompletableFuture<Result<Boolean>> sendAcquireSemaphore(String semaphoreName, long count, Duration timeout,
                                                            boolean ephemeral, int requestId);

    CompletableFuture<Result<Boolean>> sendAcquireSemaphore(String semaphoreName, long count, Duration timeout,
                                                            boolean ephemeral, byte[] data, int requestId);

    CompletableFuture<Result<Boolean>> sendReleaseSemaphore(String semaphoreName, int requestId);

    CompletableFuture<Result<SemaphoreDescription>> sendDescribeSemaphore(
            String semaphoreName, boolean includeOwners, boolean includeWaiters,
            boolean watchData, boolean watchOwners, Consumer<SemaphoreChangedEvent> updateWatcher);

    CompletableFuture<Status> sendCreateSemaphore(String semaphoreName, long limit, int requestId);

    CompletableFuture<Status> sendCreateSemaphore(String semaphoreName, long limit, @Nonnull byte[] data,
                                                  int requestId);

    CompletableFuture<Status> sendUpdateSemaphore(String semaphoreName, int requestId);

    CompletableFuture<Status> sendUpdateSemaphore(String semaphoreName, byte[] data, int requestId);

    CompletableFuture<Status> sendDeleteSemaphore(String semaphoreName, boolean force, int requestId);


}
