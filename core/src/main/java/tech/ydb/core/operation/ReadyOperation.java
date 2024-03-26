package tech.ydb.core.operation;

import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

import tech.ydb.core.Issue;
import tech.ydb.core.Result;
import tech.ydb.core.Status;
import tech.ydb.core.StatusCode;

/**
 *
 * @author Aleksandr Gorshenin
 */
class ReadyOperation<T> implements Operation<T> {
    static final Status ALREADY_DONE_STATUS = Status.of(StatusCode.CLIENT_INTERNAL_ERROR)
            .withIssues(Issue.of("Operation is already done", Issue.Severity.ERROR));

    private final String id;
    private final T value;

    ReadyOperation(String id, T value) {
        this.id = id;
        this.value = value;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public T getValue() {
        return value;
    }

    @Override
    public boolean isReady() {
        return true;
    }

    @Override
    public CompletableFuture<Status> cancel() {
        return CompletableFuture.completedFuture(ALREADY_DONE_STATUS);
    }

    @Override
    public CompletableFuture<Status> forget() {
        return CompletableFuture.completedFuture(ALREADY_DONE_STATUS);
    }

    @Override
    public CompletableFuture<Result<Boolean>> fetch() {
        return CompletableFuture.completedFuture(Result.success(Boolean.TRUE));
    }

    @Override
    public <R> Operation<R> transform(Function<T, R> func) {
        return new ReadyOperation<>(id, func.apply(value));
    }

    @Override
    public String toString() {
        return "ReadyOperation{id=" + id + ", value=" + value + "}";
    }
}
