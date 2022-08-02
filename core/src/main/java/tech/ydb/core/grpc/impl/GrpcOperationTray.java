package tech.ydb.core.grpc.impl;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import javax.annotation.concurrent.GuardedBy;

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
import tech.ydb.core.grpc.GrpcRequestSettings;
import tech.ydb.core.grpc.GrpcTransport;


/**
 * @author Sergey Polovko
 */
public class GrpcOperationTray implements OperationTray {

    private static final long INITIAL_DELAY_MILLIS = 10; // The delay before first operations service call, ms
    private static final long MAX_DELAY_MILLIS = 10_000; // The max delay between getOperation calls for one operation, ms

    // From https://netty.io/4.1/api/io/netty/util/HashedWheelTimer.html
    // HashedWheelTimer creates a new thread whenever it is instantiated and started.
    // Therefore, you should make sure to create only one instance and share it across
    // your application. One of the common mistakes, that makes your application unresponsive,
    // is to create a new instance for every connection.
    private static final Timer WHEEL_TIMER = new HashedWheelTimer(
            new DefaultThreadFactory("SharedOperationTrayTimer"),
            INITIAL_DELAY_MILLIS,
            TimeUnit.MILLISECONDS
    );

    private final GrpcTransport transport;

    @GuardedBy("this")
    private final Set<Timeout> timeouts = new HashSet<>();
    @GuardedBy("this")
    private  CancellationException cancelEx = null;

    public GrpcOperationTray(GrpcTransport transport) {
        this.transport = transport;
    }

    @Override
    public CompletableFuture<Status> waitStatus(OperationProtos.Operation operation, GrpcRequestSettings settings) {
        CompletableFuture<Status> promise = new CompletableFuture<>();

        if (operation.getReady()) {
            try {
                promise.complete(Operations.status(operation));
            } catch (Throwable t) {
                promise.completeExceptionally(t);
            }
        } else {
            new WaitStatusTask(operation.getId(), promise, settings)
                .scheduleNext(WHEEL_TIMER);
        }

        return promise;
    }

    @Override
    public <M extends Message, R> CompletableFuture<Result<R>> waitResult(
        OperationProtos.Operation operation,
        Class<M> resultClass,
        Function<M, R> mapper,
        GrpcRequestSettings settings)
    {
        CompletableFuture<Result<R>> promise = new CompletableFuture<>();
        if (operation.getReady()) {
            try {
                Status status = Operations.status(operation);
                if (status.isSuccess()) {
                    M resultMessage = Operations.unpackResult(operation, resultClass);
                    promise.complete(Result.success(mapper.apply(resultMessage), status.getIssues()));
                } else {
                    promise.complete(Result.fail(status));
                }
            } catch (Throwable t) {
                promise.completeExceptionally(t);
            }
        } else {
            new WaitResultTask<>(operation.getId(), promise, resultClass, mapper, settings)
                .scheduleNext(WHEEL_TIMER);
        }

        return promise;
    }

    private CompletableFuture<Result<GetOperationResponse>> callGetOperation(GetOperationRequest request,
                                                                             GrpcRequestSettings settings) {
        return transport.unaryCall(OperationServiceGrpc.getGetOperationMethod(), request, settings);
    }

    @Override
    public void close() {
        synchronized (this) {
            this.cancelEx = new CancellationException();
            for (Timeout timeout : timeouts) {
                TimerTask task = timeout.task();
                if (task instanceof BaseTask) {
                    ((BaseTask) task).promise.completeExceptionally(cancelEx);
                }
            }
        }
    }

    /**
     * Base waiting task.
     */
    public abstract class BaseTask<T> implements TimerTask {

        private final GetOperationRequest request;
        private final CompletableFuture<T> promise;
        private final GrpcRequestSettings settings;

        private long delayMillis = INITIAL_DELAY_MILLIS / 2;

        BaseTask(String id, CompletableFuture<T> promise, GrpcRequestSettings settings) {
            this.request = GetOperationRequest.newBuilder().setId(id).build();
            this.promise = promise;
            this.settings = settings;
        }

        protected abstract T mapFailedRpc(Result<?> result);
        protected abstract T mapReadyOperation(OperationProtos.Operation operation);

        @Override
        public void run(Timeout timeout) {
            synchronized (GrpcOperationTray.this) {
                // Check if operation tray is stopped and all tasks already completed with exception
                if (cancelEx != null) {
                    return;
                }
                timeouts.remove(timeout);
            }

            if (promise.isCancelled()) {
                return;
            }

            callGetOperation(request, settings)
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
            synchronized (GrpcOperationTray.this) {
                // Check if operation tray is stopped and all tasks already completed with exception
                if (cancelEx != null) {
                    return;
                }
                // exponentially growing delay
                delayMillis = Math.min(delayMillis * 2, MAX_DELAY_MILLIS);
                timeouts.add(timer.newTimeout(this, delayMillis, TimeUnit.MILLISECONDS));
            }
        }
    }

    /**
     * Task for waiting status.
     */
    public final class WaitStatusTask extends BaseTask<Status> {

        WaitStatusTask(String id, CompletableFuture<Status> promise, GrpcRequestSettings settings) {
            super(id, promise, settings);
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
            GrpcRequestSettings settings)
        {
            super(id, promise, settings);
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
            return Result.success(mapper.apply(resultMessage), status.getIssues());
        }
    }
}
