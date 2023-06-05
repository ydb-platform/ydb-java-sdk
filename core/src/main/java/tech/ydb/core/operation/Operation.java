package tech.ydb.core.operation;

import java.util.concurrent.CompletableFuture;

import com.google.protobuf.Message;

import tech.ydb.core.Result;

/**
 * @author Kirill Kurdyukov
 */
public class Operation<V extends Message> {
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
