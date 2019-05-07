package ru.yandex.ydb.table;

import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import com.google.protobuf.Message;
import io.netty.util.Timeout;
import io.netty.util.Timer;
import io.netty.util.TimerTask;

import ru.yandex.ydb.OperationProtos.GetOperationRequest;
import ru.yandex.ydb.OperationProtos.Operation;
import ru.yandex.ydb.core.Operations;
import ru.yandex.ydb.core.Result;
import ru.yandex.ydb.core.Status;
import ru.yandex.ydb.table.rpc.OperationRpc;


/**
 * @author Sergey Polovko
 */
public class OperationsTray implements AutoCloseable {

    public static final long INITIAL_DELAY_MILLIS = 10; // The delay before first operations service call, ms
    public static final long MAX_DELAY_MILLIS = 10_000; // The max delay between getOperation calls for one operation, ms

    private final OperationRpc rpc;
    private final Timer timer;

    public OperationsTray(OperationRpc rpc, Timer timer) {
        this.rpc = rpc;
        this.timer = timer;
    }

    public CompletableFuture<Status> waitStatus(Operation operation) {
        CompletableFuture<Status> promise = new CompletableFuture<>();

        if (operation.getReady()) {
            try {
                promise.complete(Operations.status(operation));
            } catch (Throwable t) {
                promise.completeExceptionally(t);
            }
        } else {
            new WaitStatusTask(rpc, operation.getId(), promise)
                .scheduleNext(timer);
        }

        return promise;
    }

    public <M extends Message, R> CompletableFuture<Result<R>> waitResult(
        Operation operation,
        Class<M> resultClass,
        Function<M, R> mapper)
    {
        CompletableFuture<Result<R>> promise = new CompletableFuture<>();
        if (operation.getReady()) {
            try {
                Status status = Operations.status(operation);
                if (status.isSuccess()) {
                    M resultMessage = Operations.unpackResult(operation, resultClass);
                    promise.complete(Result.success(mapper.apply(resultMessage)));
                } else {
                    promise.complete(Result.fail(status));
                }
            } catch (Throwable t) {
                promise.completeExceptionally(t);
            }
        } else {
            new WaitResultTask<>(rpc, operation.getId(), promise, resultClass, mapper)
                .scheduleNext(timer);
        }

        return promise;
    }

    @Override
    public void close() {
        CancellationException cancelEx = new CancellationException();
        for (Timeout timeout : timer.stop()) {
            TimerTask task = timeout.task();
            if (task instanceof BaseTask) {
                ((BaseTask) task).promise.completeExceptionally(cancelEx);
            }
        }
    }

    /**
     * Base waiting task.
     */
    public abstract static class BaseTask<T> implements TimerTask {

        private final OperationRpc rpc;
        private final GetOperationRequest request;
        private final CompletableFuture<T> promise;

        private long delayMillis = INITIAL_DELAY_MILLIS / 2;

        BaseTask(OperationRpc rpc, String id, CompletableFuture<T> promise) {
            this.rpc = rpc;
            this.request = GetOperationRequest.newBuilder().setId(id).build();
            this.promise = promise;
        }

        protected abstract T mapFailedRpc(Result<?> result);
        protected abstract T mapReadyOperation(Operation operation);

        @Override
        public void run(Timeout timeout) {
            if (promise.isCancelled()) {
                return;
            }

            rpc.getOperation(request)
                .whenComplete((response, throwable) -> {
                    if (throwable != null) {
                        promise.completeExceptionally(throwable);
                        return;
                    }

                    if (!response.isSuccess()) {
                        promise.complete(mapFailedRpc(response));
                        return;
                    }

                    Operation operation = response.expect("getOperation()").getOperation();
                    if (operation.getReady()) {
                        try {
                            promise.complete(mapReadyOperation(operation));
                        } catch (Throwable t) {
                            promise.completeExceptionally(t);
                        }
                    } else {
                        scheduleNext(timeout.timer());
                    }
                });
        }

        public void scheduleNext(Timer timer) {
            // exponentially growing delay
            delayMillis = Math.min(delayMillis * 2, MAX_DELAY_MILLIS);
            timer.newTimeout(this, delayMillis, TimeUnit.MILLISECONDS);
        }
    }

    /**
     * Task for waiting status.
     */
    public static final class WaitStatusTask extends BaseTask<Status> {

        WaitStatusTask(OperationRpc rpc, String id, CompletableFuture<Status> promise) {
            super(rpc, id, promise);
        }

        @Override
        protected Status mapFailedRpc(Result<?> result) {
            return result.toStatus();
        }

        @Override
        protected Status mapReadyOperation(Operation operation) {
            return Operations.status(operation);
        }
    }

    /**
     * Task for waiting result.
     */
    public static final class WaitResultTask<M extends Message, R> extends BaseTask<Result<R>> {

        private final Class<M> resultClass;
        private final Function<M, R> mapper;

        WaitResultTask(
            OperationRpc rpc,
            String id,
            CompletableFuture<Result<R>> promise,
            Class<M> resultClass,
            Function<M, R> mapper)
        {
            super(rpc, id, promise);
            this.resultClass = resultClass;
            this.mapper = mapper;
        }

        @Override
        protected Result<R> mapFailedRpc(Result<?> result) {
            return result.cast();
        }

        @Override
        protected Result<R> mapReadyOperation(Operation operation) {
            Status status = Operations.status(operation);
            if (!status.isSuccess()) {
                return Result.fail(status);
            }
            M resultMessage = Operations.unpackResult(operation, resultClass);
            return Result.success(mapper.apply(resultMessage));
        }
    }
}
