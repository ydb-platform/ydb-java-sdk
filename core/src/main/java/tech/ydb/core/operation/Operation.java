package tech.ydb.core.operation;

import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

import com.google.protobuf.Message;

import tech.ydb.core.Result;

/**
 * @author Kirill Kurdyukov
 */
public class Operation<V> {
    private final String operationId;
    private final OperationManager operationManager;
    protected final CompletableFuture<Result<V>> resultCompletableFuture;
    final Class<V> resultClass;

    Operation(
            String operationId,
            Class<V> resultClass,
            OperationManager operationManager
    ) {
        this.operationId = operationId;
        this.resultClass = resultClass;
        this.operationManager = operationManager;
        this.resultCompletableFuture = new CompletableFuture<>();
    }

    private Operation(
            String operationId,
            OperationManager operationManager,
            CompletableFuture<Result<V>> resultCompletableFuture
    ) {
        this.operationId = operationId;
        this.operationManager = operationManager;
        this.resultCompletableFuture = resultCompletableFuture;
        this.resultClass = null; // unused class token;
    }

    public static <A extends Message, B> Function<Operation<A>, Operation<B>> transformOperation(
            Function<A, B> transform
    ) {
        return operation -> new Operation<>(
                operation.operationId,
                operation.operationManager,
                operation.resultCompletableFuture.thenApply(aResult -> aResult.map(transform))
        );
    }

    public String getOperationId() {
        return operationId;
    }

    public CompletableFuture<Result<V>> getResultFuture() {
        return resultCompletableFuture;
    }

    public CompletableFuture<Result<V>> cancel() {
        return operationManager.cancel(this)
                .thenCompose(cancelOperationResponseResult -> getResultFuture());
    }
}
