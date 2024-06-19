package tech.ydb.core.operation;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Function;

import tech.ydb.core.Result;
import tech.ydb.core.Status;
import tech.ydb.core.grpc.GrpcRequestSettings;
import tech.ydb.core.grpc.GrpcTransport;
import tech.ydb.proto.OperationProtos;
import tech.ydb.proto.operation.v1.OperationServiceGrpc;

/**
 *
 * @author Aleksandr Gorshenin
 */
class OperationImpl<T> implements AsyncOperation<T> {
    private static final StatusMapper<OperationProtos.CancelOperationResponse> CANCEL_OPERATION = StatusMapper.of(
            OperationProtos.CancelOperationResponse::getStatus,
            OperationProtos.CancelOperationResponse::getIssuesList
    );

    private static final StatusMapper<OperationProtos.ForgetOperationResponse> FORGET_OPERATION = StatusMapper.of(
            OperationProtos.ForgetOperationResponse::getStatus,
            OperationProtos.ForgetOperationResponse::getIssuesList
    );

    private final GrpcTransport transport;
    private final String id;
    private final Function<OperationProtos.Operation, T> valueExtractor;
    private volatile T value = null;

    OperationImpl(GrpcTransport transport, String id, Function<OperationProtos.Operation, T> extractor) {
        this.transport = transport;
        this.id = id;
        this.valueExtractor = extractor;
    }

    @Override
    public ScheduledExecutorService getScheduler() {
        return transport.getScheduler();
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public boolean isReady() {
        return value != null;
    }

    @Override
    public T getValue() {
        return value;
    }

    @Override
    public String toString() {
        return "AsyncOperation{id=" + id + ", ready=" + (value != null) + "}";
    }

    @Override
    public CompletableFuture<Status> cancel() {
        GrpcRequestSettings settings = GrpcRequestSettings.newBuilder().build();
        OperationProtos.CancelOperationRequest request = OperationProtos.CancelOperationRequest.newBuilder()
                        .setId(id)
                        .build();

        return transport
                .unaryCall(OperationServiceGrpc.getCancelOperationMethod(), settings, request)
                .thenApply(CANCEL_OPERATION);
    }

    @Override
    public CompletableFuture<Status> forget() {
        GrpcRequestSettings settings = GrpcRequestSettings.newBuilder().build();
        OperationProtos.ForgetOperationRequest request = OperationProtos.ForgetOperationRequest.newBuilder()
                        .setId(id)
                        .build();

        return transport
                .unaryCall(OperationServiceGrpc.getForgetOperationMethod(), settings, request)
                .thenApply(FORGET_OPERATION);
    }

    @Override
    public CompletableFuture<Result<Boolean>> fetch() {
        if (value != null) {
            return CompletableFuture.completedFuture(Result.success(Boolean.TRUE));
        }

        GrpcRequestSettings settings = GrpcRequestSettings.newBuilder().build();
        OperationProtos.GetOperationRequest request = OperationProtos.GetOperationRequest.newBuilder()
                        .setId(id)
                        .build();

        return transport
                .unaryCall(OperationServiceGrpc.getGetOperationMethod(), settings, request)
                .thenApply(res -> res.map(this::handleOperation));
    }

    private boolean handleOperation(OperationProtos.GetOperationResponse resp) {
        OperationProtos.Operation operation = resp.getOperation();
        if (!operation.getReady()) {
            return false;
        }

        this.value = valueExtractor.apply(operation);
        return true;
    }

    @Override
    public <R> Operation<R> transform(Function<T, R> mapper) {
        return new Proxy<>(mapper);
    }

    private class Proxy<R> implements AsyncOperation<R> {
        private final Function<T, R> mapper;

        Proxy(Function<T, R> mapper) {
            this.mapper = mapper;
        }

        @Override
        public ScheduledExecutorService getScheduler() {
            return transport.getScheduler();
        }

        @Override
        public String getId() {
            return id;
        }

        @Override
        public boolean isReady() {
            return value != null;
        }

        @Override
        public R getValue() {
            if (value == null) {
                return null;
            }
            return mapper.apply(value);
        }

        @Override
        public CompletableFuture<Status> cancel() {
            return OperationImpl.this.cancel();
        }

        @Override
        public CompletableFuture<Status> forget() {
            return OperationImpl.this.forget();
        }

        @Override
        public CompletableFuture<Result<Boolean>> fetch() {
            return OperationImpl.this.fetch();
        }

        @Override
        public <Z> Operation<Z> transform(Function<R, Z> func) {
            return new Proxy<>(mapper.andThen(func));
        }

        @Override
        public String toString() {
            return "ProxyAsyncOperation{id=" + id + ", ready=" + (value != null) + "}";
        }
    }
}
