package tech.ydb.core.operation;

import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

import javax.annotation.Nullable;

import tech.ydb.core.Result;

/**
 * @author Kirill Kurdyukov
 */
public class Operation<V> {
    private final String operationId;
    private final OperationManager operationManager;
    final CompletableFuture<Result<V>> resultCompletableFuture;

    Operation(
            String operationId,
            OperationManager operationManager,
            CompletableFuture<Result<V>> resultCompletableFuture
    ) {
        this.operationId = operationId;
        this.operationManager = operationManager;
        this.resultCompletableFuture = resultCompletableFuture;
    }

    public <T> Operation<T> transform(
            Function<V, T> transform
    ) {
        return new Operation<>(
                this.operationId,
                this.operationManager,
                this.resultCompletableFuture.thenApply(result -> result.map(transform))
        );
    }

    @Nullable
    public String getOperationId() {
        return operationId;
    }

    public CompletableFuture<Result<V>> getResultFuture() {
        return resultCompletableFuture;
    }

    public CompletableFuture<Result<V>> cancel() {
        if (resultCompletableFuture.isDone()) {
            return resultCompletableFuture;
        }

        return operationManager.cancel(this)
                .thenCompose(cancelOperationResponseResult -> getResultFuture());
    }
}
