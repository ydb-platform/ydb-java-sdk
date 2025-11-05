package tech.ydb.query.script.result;

import tech.ydb.core.Result;
import tech.ydb.core.Status;
import tech.ydb.core.operation.Operation;
import tech.ydb.core.operation.OperationTray;

import javax.annotation.Nullable;

import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

/**
 *
 */
public class OperationScript implements Operation<Status> {

    private final Operation<Status> operation;
    private volatile CompletableFuture<Status> futureResultOfScriptExecution;

    public OperationScript(Operation<Status> resultOperation) {
        this.operation = resultOperation;
    }

    public CompletableFuture<Status> getStatus() {
        if (futureResultOfScriptExecution == null) {
            synchronized (this) {
                if (futureResultOfScriptExecution == null) {
                    futureResultOfScriptExecution = OperationTray.fetchOperation(
                            operation, 1);
                }
            }
        }
        return futureResultOfScriptExecution;
    }

    public Status waitForResult() {
        return getStatus().join();
    }

    public String getId() {
        return operation.getId();
    }

    public boolean isReady() {
        return operation.isReady();
    }

    @Nullable
    @Override
    public Status getValue() {
        return operation.getValue();
    }

    @Override
    public CompletableFuture<Status> cancel() {
        return operation.cancel();
    }

    @Override
    public CompletableFuture<Status> forget() {
        return operation.forget();
    }

    @Override
    public CompletableFuture<Result<Boolean>> fetch() {
        return operation.fetch();
    }

    @Override
    public <R> Operation<R> transform(Function<Status, R> mapper) {
        return null;
    }

}
