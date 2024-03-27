package tech.ydb.core.operation;

import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

import tech.ydb.core.Result;
import tech.ydb.core.Status;

/**
 *
 * @author Aleksandr Gorshenin
 */
class FailedOperation<T> implements Operation<T> {
    private final T value;
    private final Status status;

    FailedOperation(T value, Status status) {
        this.value = value;
        this.status = status;
    }

    @Override
    public String getId() {
        return null;
    }

    @Override
    public boolean isReady() {
        return true;
    }

    @Override
    public T getValue() {
        return value;
    }

    @Override
    public CompletableFuture<Status> cancel() {
        return CompletableFuture.completedFuture(status);
    }

    @Override
    public CompletableFuture<Status> forget() {
        return CompletableFuture.completedFuture(status);
    }

    @Override
    public CompletableFuture<Result<Boolean>> fetch() {
        return CompletableFuture.completedFuture(Result.fail(status));
    }

    @Override
    public <R> Operation<R> transform(Function<T, R> func) {
        return new FailedOperation<>(func.apply(value), status);
    }

    @Override
    public String toString() {
        return "FailedOperation{status=" + status + "}";
    }
}
