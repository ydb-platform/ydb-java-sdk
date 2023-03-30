package tech.ydb.core.impl.operation;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import javax.annotation.Nullable;

import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tech.ydb.OperationProtos;
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

    private static final Logger logger = LoggerFactory
            .getLogger(OperationManager.class);
    private static final long OPERATION_CHECK_TIMEOUT_MS = 1_000;

    private final GrpcTransport transport;
    private final ScheduledExecutorService scheduledExecutorService;
    private final GrpcRequestSettings requestSettings = GrpcRequestSettings.newBuilder().build();

    public OperationManager(GrpcTransport transport) {
        this.transport = transport;
        this.scheduledExecutorService = transport.getScheduler();
    }

    public <
            ResultRpc,
            UnwrapperResult extends Message
            >
    Function<Result<ResultRpc>, CompletableFuture<Result<UnwrapperResult>>> resultUnwrapper(
            Function<ResultRpc, OperationProtos.Operation> operationExtractor,
            Class<UnwrapperResult> resultClass
    ) {
        return ((Function<Operation<UnwrapperResult>, CompletableFuture<Result<UnwrapperResult>>>)
                Operation::getResultFuture)
                .compose(operationUnwrapper(operationExtractor, resultClass));
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

            return createOperation(
                    operationExtractor.apply(result.getValue()),
                    resultClass
            );
        };
    }

    public <ResultRpc> Function<Result<ResultRpc>, CompletableFuture<Status>> statusUnwrapper(
            Function<ResultRpc, OperationProtos.Operation> operationExtractor) {
        return (result) -> {
            if (!result.isSuccess()) {
                return CompletableFuture.completedFuture(result.getStatus());
            }

            return createOperation(
                    operationExtractor.apply(result.getValue()),
                    null
            ).getResultFuture()
                    .thenApply(Result::getStatus);
        };
    }

    private <Value extends Message> Operation<Value> createOperation(
            final OperationProtos.Operation operationProto,
            final Class<Value> resultClass
    ) {
        Operation<Value> operation = new Operation<>(
                operationProto.getId(),
                resultClass,
                this
        );

        completeOperation(operationProto, operation);

        return operation;
    }


    private <Value extends Message> void completeOperation(
            final OperationProtos.Operation operationProto,
            final Operation<Value> operation
    ) {
        if (operation.resultCompletableFuture.isCancelled()) {
            return;
        }

        final Status status = OperationUtils.status(operationProto);

        if (operationProto.getReady()) {
            if (status.isSuccess()) {
                try {
                    Value resultMessage = null;

                    if (operation.resultClass != null) {
                        resultMessage = operationProto
                                .getResult()
                                .unpack(operation.resultClass);
                    }

                    operation.resultCompletableFuture
                            .complete(Result.success(resultMessage, status));
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

                    transport.unaryCall(
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
        return transport.unaryCall(
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

        @Nullable
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
