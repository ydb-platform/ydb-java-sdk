package tech.ydb.core;

import tech.ydb.core.operation.Operation;

import javax.annotation.Nonnull;

import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

public class OperationResult<T> implements Result<T>{

    Operation<Result<T>> operation;
    Result<T> result;

    public OperationResult(Operation<Result<T>> resultOperation) {
        this.operation = resultOperation;
        this.result = operation.getValue();
    }

    public Operation<Result<T>> getOperation() {
        return operation;
    }

    @Nonnull
    @Override
    public Status getStatus() {
        return result.getStatus();
    }

    @Nonnull
    @Override
    public T getValue() throws UnexpectedResultException {
        return result.getValue();
    }

    @Nonnull
    @Override
    public <U> Result<U> map(@Nonnull Function<T, U> mapper) {
        return result.map(mapper);
    }

    @Nonnull
    @Override
    public <U> CompletableFuture<Result<U>> mapResultFuture(@Nonnull Function<T, CompletableFuture<Result<U>>> mapper) {
        return result.mapResultFuture(mapper);
    }

    @Nonnull
    @Override
    public CompletableFuture<Status> mapStatusFuture(@Nonnull Function<T, CompletableFuture<Status>> mapper) {
        return result.mapStatusFuture(mapper);
    }
}
