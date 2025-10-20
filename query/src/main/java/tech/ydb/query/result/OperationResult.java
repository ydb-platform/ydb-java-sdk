package tech.ydb.query.result;

import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

import javax.annotation.Nonnull;

import tech.ydb.core.Result;
import tech.ydb.core.Status;
import tech.ydb.core.UnexpectedResultException;
import tech.ydb.core.operation.Operation;
import tech.ydb.query.QuerySession;
import tech.ydb.query.settings.ExecuteScriptSettings;
import tech.ydb.table.query.Params;

/**
 * Represents the result of an executed YQL script operation.
 * <p>
 * This class wraps a {@link Operation} that contains a {@link Result} object
 * and provides convenient access to both the operation metadata and the
 * actual execution result.
 * </p>
 *
 * <p>Typically used as the return type for
 * {@link QuerySession#executeScriptYql(String, Params, ExecuteScriptSettings)}
 * and similar asynchronous script execution APIs.</p>
 *
 * @param <T> the type of value contained in the result
 *
 * <p>Author: Evgeny Kuvardin
 */
public class OperationResult<T> implements Result<T> {

    private final Operation<Result<T>> operation;
    private final Result<T> result;

    public OperationResult(Operation<Result<T>> resultOperation) {
        this.operation = resultOperation;
        this.result = operation.getValue();
    }

    /**
     * Returns the underlying {@link Operation} associated with this result.
     * <p>
     * The operation object contains metadata such as operation ID, execution status,
     * and timing information.
     * </p>
     *
     * @return the wrapped {@link Operation} object
     */
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
