package tech.ydb.core.operation;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import com.google.common.annotations.VisibleForTesting;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tech.ydb.OperationProtos;
import tech.ydb.core.Issue;
import tech.ydb.core.Result;
import tech.ydb.core.Status;
import tech.ydb.core.StatusCode;
import tech.ydb.core.grpc.GrpcRequestSettings;
import tech.ydb.core.grpc.GrpcTransport;
import tech.ydb.operation.v1.OperationServiceGrpc;

/**
 * @author Kirill Kurdyukov
 */
public final class OperationManager {

    private static final Logger logger = LoggerFactory.getLogger(OperationManager.class);
    private static final Status ASYNC_ARE_UNSUPPORTED = Status.of(StatusCode.CLIENT_INTERNAL_ERROR)
            .withIssues(Issue.of("Async operations are not supported", Issue.Severity.ERROR));
    private static final long OPERATION_CHECK_TIMEOUT_MS = 1_000;

    private final GrpcTransport grpcTransport;
    private final ScheduledExecutorService scheduledExecutorService;
    private final GrpcRequestSettings requestSettings = GrpcRequestSettings.newBuilder().build();

    public OperationManager(GrpcTransport grpcTransport) {
        this.grpcTransport = grpcTransport;
        this.scheduledExecutorService = grpcTransport.getScheduler();
    }

    @VisibleForTesting
    static Status status(OperationProtos.Operation operation) {
        StatusCode code = StatusCode.fromProto(operation.getStatus());
        Double consumedRu = null;
        if (operation.hasCostInfo()) {
            consumedRu = operation.getCostInfo().getConsumedUnits();
        }

        return Status.of(code, consumedRu, Issue.fromPb(operation.getIssuesList()));
    }

    public static <
            ResultRpc,
            UnwrapperResult extends Message
            >
    Function<Result<ResultRpc>, Result<UnwrapperResult>> syncResultUnwrapper(
            Function<ResultRpc, OperationProtos.Operation> operationExtractor,
            Class<UnwrapperResult> resultClass
    ) {
        return (result) -> {
            if (!result.isSuccess()) {
                return result.map(null);
            }
            OperationProtos.Operation operation = operationExtractor.apply(result.getValue());
            if (operation.getReady()) {
                Status status = status(operation);
                if (!status.isSuccess()) {
                    return Result.fail(status);
                }

                try {
                    UnwrapperResult resultMessage = operation.getResult().unpack(resultClass);
                    return Result.success(resultMessage, status);
                } catch (InvalidProtocolBufferException ex) {
                    return Result.error("Can't unpack message " + resultClass.getName(), ex);
                }
            }
            return Result.fail(ASYNC_ARE_UNSUPPORTED);
        };
    }

    public static <ResultRpc> Function<Result<ResultRpc>, Status> syncStatusUnwrapper(
            Function<ResultRpc, OperationProtos.Operation> operationExtractor
    ) {
        return (result) -> {
            if (!result.isSuccess()) {
                return result.getStatus();
            }

            OperationProtos.Operation operation = operationExtractor.apply(result.getValue());
            if (operation.getReady()) {
                return status(operation);
            }

            return ASYNC_ARE_UNSUPPORTED;
        };
    }

    public <
            ResultRpc,
            UnwrapperResult extends Message
            >
    Function<Result<ResultRpc>, Operation<UnwrapperResult>> operationUnwrapper(
            Function<ResultRpc, OperationProtos.Operation> operationExtractor,
            Class<UnwrapperResult> resultClass
    ) {
        return (result) -> {
            if (!result.isSuccess()) {
                return new FailOperation<>(
                        result.map(null)
                );
            }

            OperationProtos.Operation operationProto = operationExtractor.apply(result.getValue());

            Operation<UnwrapperResult> operation = new Operation<>(
                    operationProto.getId(),
                    resultClass,
                    this
            );

            completeOperation(operationProto, operation);

            return operation;
        };
    }

    private <Value extends Message> void completeOperation(
            final OperationProtos.Operation operationProto,
            final Operation<Value> operation
    ) {
        if (operation.resultCompletableFuture.isCancelled()) {
            return;
        }

        final Status status = status(operationProto);

        if (operationProto.getReady()) {
            if (status.isSuccess()) {
                try {
                    operation.resultCompletableFuture.complete(
                            Result.success(operationProto.getResult().unpack(operation.resultClass), status)
                    );
                } catch (InvalidProtocolBufferException ex) {
                    operation.resultCompletableFuture.complete(
                            Result.error(
                                    "Can't unpack message " + operation.resultClass.getName(),
                                    ex
                            )
                    );
                }
            } else {
                operation.resultCompletableFuture.complete(Result.fail(status));
            }

            return;
        }

        scheduledExecutorService.schedule(
                () -> {
                    OperationProtos.GetOperationRequest request = OperationProtos.GetOperationRequest
                            .newBuilder()
                            .setId(operation.operationId)
                            .build();

                    grpcTransport.unaryCall(
                            OperationServiceGrpc.getGetOperationMethod(),
                            requestSettings,
                            request
                    ).whenComplete(
                            (getOperationResponseResult, throwable) -> {
                                if (throwable != null) {
                                    operation.resultCompletableFuture.completeExceptionally(throwable);
                                } else if (getOperationResponseResult != null) {
                                    if (getOperationResponseResult.isSuccess()) {
                                        completeOperation(
                                                getOperationResponseResult.getValue().getOperation(),
                                                operation
                                        );
                                    } else {
                                        operation.resultCompletableFuture.complete(
                                                getOperationResponseResult.map(null)
                                        );
                                    }
                                }
                            }
                    );
                },
                OPERATION_CHECK_TIMEOUT_MS,
                TimeUnit.MILLISECONDS
        );
    }

    private CompletableFuture<Result<OperationProtos.CancelOperationResponse>> cancel(
            final Operation<?> operation
    ) {
        return grpcTransport.unaryCall(
                OperationServiceGrpc.getCancelOperationMethod(),
                GrpcRequestSettings.newBuilder()
                        .build(),
                OperationProtos.CancelOperationRequest.newBuilder()
                        .setId(operation.operationId)
                        .build()
        ).whenComplete(
                (cancelOperationResponseResult, throwable) -> {
                    if (throwable != null) {
                        logger.error("Fail cancel polling operation with id: {}", operation.operationId, throwable);
                    }

                    if (cancelOperationResponseResult.isSuccess()) {
                        logger.info("Success cancel polling operation with id: {}", operation.operationId);

                        operation.resultCompletableFuture.complete(
                                Result.fail(Status.of(StatusCode.CANCELLED))
                        );
                    } else {
                        logger.error("Fail cancel polling operation with id: {}", operation.operationId);
                    }
                }
        );
    }

    public static class Operation<Value extends Message> {
        private final String operationId;
        private final Class<Value> resultClass;
        private final OperationManager operationManager;
        protected final CompletableFuture<Result<Value>> resultCompletableFuture;

        private Operation(
                String operationId,
                Class<Value> resultClass,
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

        public CompletableFuture<Result<Value>> getResultFuture() {
            return resultCompletableFuture;
        }

        public CompletableFuture<Result<Value>> cancel() {
            return operationManager.cancel(this)
                    .thenCompose(cancelOperationResponseResult -> getResultFuture());
        }
    }

    private static class FailOperation<Value extends Message> extends Operation<Value> {

        private FailOperation(Result<Value> resultFailed) {
            super(null, null, null);

            resultCompletableFuture.complete(resultFailed);
        }

        @Override
        public CompletableFuture<Result<Value>> cancel() {
            return getResultFuture();
        }
    }
}
