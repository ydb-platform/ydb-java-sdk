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
    private final CompletableFuture<Result<V>> resultFuture;

    Operation(String operationId, OperationManager operationManager, CompletableFuture<Result<V>> resultFuture) {
        this.operationId = operationId;
        this.operationManager = operationManager;
        this.resultFuture = resultFuture;
    }

    public <T> Operation<T> transform(Function<V, T> transform) {
        return new Operation<>(
                this.operationId,
                this.operationManager,
                this.resultFuture.thenApply(result -> result.map(transform))
        );
    }

    @Nullable
    public String getOperationId() {
        return operationId;
    }

    public CompletableFuture<Result<V>> getResultFuture() {
        return resultFuture;
    }

    public CompletableFuture<Result<V>> cancel() {
        if (resultFuture.isDone()) {
            return resultFuture;
        }

        return operationManager.cancel(this)
                .thenCompose(cancelOperationResponseResult -> getResultFuture());
    }
}
