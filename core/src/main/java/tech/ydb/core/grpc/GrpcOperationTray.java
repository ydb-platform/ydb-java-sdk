package tech.ydb.core.grpc;

import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import com.google.protobuf.Message;
import tech.ydb.OperationProtos;
import tech.ydb.OperationProtos.GetOperationRequest;
import tech.ydb.OperationProtos.GetOperationResponse;
import tech.ydb.core.Operations;
import tech.ydb.core.Result;
import tech.ydb.core.Status;
import tech.ydb.core.rpc.OperationTray;
import tech.ydb.operation.v1.OperationServiceGrpc;
import io.netty.util.HashedWheelTimer;
import io.netty.util.Timeout;
import io.netty.util.Timer;
import io.netty.util.TimerTask;
import io.netty.util.concurrent.DefaultThreadFactory;


/**
 * @author Sergey Polovko
 */
final class GrpcOperationTray implements OperationTray {

    private static final long INITIAL_DELAY_MILLIS = 10; // The delay before first operations service call, ms
    private static final long MAX_DELAY_MILLIS = 10_000; // The max delay between getOperation calls for one operation, ms

    private final GrpcTransport transport;
    private final Timer timer;

    GrpcOperationTray(GrpcTransport transport) {
        this.transport = transport;
        this.timer = new HashedWheelTimer(
            new DefaultThreadFactory("OperationTrayTimer"),
            INITIAL_DELAY_MILLIS,
            TimeUnit.MILLISECONDS);
    }

    @Override
    public CompletableFuture<Status> waitStatus(OperationProtos.Operation operation, long deadlineAfter) {
        CompletableFuture<Status> promise = new CompletableFuture<>();

        if (operation.getReady()) {
            try {
                promise.complete(Operations.status(operation));
            } catch (Throwable t) {
                promise.completeExceptionally(t);
            }
        } else {
            new WaitStatusTask(operation.getId(), promise, deadlineAfter)
                .scheduleNext(timer);
        }

        return promise;
    }

    @Override
    public <M extends Message, R> CompletableFuture<Result<R>> waitResult(
        OperationProtos.Operation operation,
        Class<M> resultClass,
        Function<M, R> mapper,
        long deadlineAfter)
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
            new WaitResultTask<>(operation.getId(), promise, resultClass, mapper, deadlineAfter)
                .scheduleNext(timer);
        }

        return promise;
    }

    private CompletableFuture<Result<GetOperationResponse>> callGetOperation(GetOperationRequest request, long deadlineAfter) {
        return transport.unaryCall(OperationServiceGrpc.METHOD_GET_OPERATION, request, deadlineAfter);
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
    public abstract class BaseTask<T> implements TimerTask {

        private final GetOperationRequest request;
        private final CompletableFuture<T> promise;
        private final long deadlineAfter;

        private long delayMillis = INITIAL_DELAY_MILLIS / 2;

        BaseTask(String id, CompletableFuture<T> promise, long deadlineAfter) {
            this.request = GetOperationRequest.newBuilder().setId(id).build();
            this.promise = promise;
            this.deadlineAfter = deadlineAfter;
        }

        protected abstract T mapFailedRpc(Result<?> result);
        protected abstract T mapReadyOperation(OperationProtos.Operation operation);

        @Override
        public void run(Timeout timeout) {
            if (promise.isCancelled()) {
                return;
            }

            callGetOperation(request, deadlineAfter)
                .whenComplete((response, throwable) -> {
                    if (throwable != null) {
                        promise.completeExceptionally(throwable);
                        return;
                    }

                    if (!response.isSuccess()) {
                        promise.complete(mapFailedRpc(response));
                        return;
                    }

                    OperationProtos.Operation operation = response.expect("getOperation()").getOperation();
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

        void scheduleNext(Timer timer) {
            // exponentially growing delay
            delayMillis = Math.min(delayMillis * 2, MAX_DELAY_MILLIS);
            timer.newTimeout(this, delayMillis, TimeUnit.MILLISECONDS);
        }
    }

    /**
     * Task for waiting status.
     */
    public final class WaitStatusTask extends BaseTask<Status> {

        WaitStatusTask(String id, CompletableFuture<Status> promise, long deadlineAfter) {
            super(id, promise, deadlineAfter);
        }

        @Override
        protected Status mapFailedRpc(Result<?> result) {
            return result.toStatus();
        }

        @Override
        protected Status mapReadyOperation(OperationProtos.Operation operation) {
            return Operations.status(operation);
        }
    }

    /**
     * Task for waiting result.
     */
    public final class WaitResultTask<M extends Message, R> extends BaseTask<Result<R>> {

        private final Class<M> resultClass;
        private final Function<M, R> mapper;

        WaitResultTask(
            String id,
            CompletableFuture<Result<R>> promise,
            Class<M> resultClass,
            Function<M, R> mapper,
            long deadlineAfter)
        {
            super(id, promise, deadlineAfter);
            this.resultClass = resultClass;
            this.mapper = mapper;
        }

        @Override
        protected Result<R> mapFailedRpc(Result<?> result) {
            return result.cast();
        }

        @Override
        protected Result<R> mapReadyOperation(OperationProtos.Operation operation) {
            Status status = Operations.status(operation);
            if (!status.isSuccess()) {
                return Result.fail(status);
            }
            M resultMessage = Operations.unpackResult(operation, resultClass);
            return Result.success(mapper.apply(resultMessage));
        }
    }
}
