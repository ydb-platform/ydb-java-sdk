package tech.ydb.core.impl.polling;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

import io.grpc.MethodDescriptor;
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
public final class PollingOperationManager {

    private static final Logger logger = LoggerFactory
            .getLogger(PollingOperationManager.class);
    private static final long OPERATION_CHECK_TIMEOUT_MS = 1_000;


    private final GrpcTransport transport;
    private final ScheduledExecutorService scheduledExecutorService;
    private final GrpcRequestSettings requestSettings = GrpcRequestSettings.newBuilder().build();

    public PollingOperationManager(GrpcTransport transport) {
        this.transport = transport;
        this.scheduledExecutorService = transport.getScheduler();
    }

    public <ReqT, RespT> CompletableFuture<Result<PollingOperation>> startPollingOperation(
            MethodDescriptor<ReqT, RespT> serviceRpcMethod,
            ReqT request,
            Function<RespT, OperationProtos.Operation> operationIdFetcher
    ) {
        return transport
                .unaryCall(serviceRpcMethod, requestSettings, request)
                .handleAsync(
                        (result, ex) -> {
                            if (ex != null || result == null) {
                                return Result.error("Fail start polling operation", ex);
                            }

                            if (!result.isSuccess()) {
                                return result.map(null);
                            } else {
                                final OperationProtos.Operation operation = operationIdFetcher
                                        .apply(result.getValue());

                                Status status = fetchStatus(operation);

                                final PollingOperation pollingOperation = new PollingOperation(
                                        this,
                                        status,
                                        operation.getId()
                                );

                                completeOperation(operation, pollingOperation);

                                return Result.success(pollingOperation);
                            }
                        }
                );
    }


    private void completeOperation(
            final OperationProtos.Operation operation,
            final PollingOperation pollingOperation
    ) {
        if (!operation.getReady() &&
                pollingOperation.getStatus() != Status.of(StatusCode.CANCELLED)) {
            scheduledExecutorService.schedule(
                    () -> {
                        OperationProtos.GetOperationRequest operationRequest = OperationProtos
                                .GetOperationRequest
                                .newBuilder()
                                .setId(operation.getId())
                                .build();

                        transport
                                .unaryCall(
                                        OperationServiceGrpc.getGetOperationMethod(),
                                        requestSettings,
                                        operationRequest
                                )
                                .whenCompleteAsync(
                                        (getOperationResponseResult, throwable) -> {
                                            if (throwable != null) {
                                                logger.error("Fail get status poll operation, id: {}",
                                                        pollingOperation.id, throwable);

                                                return;
                                            }

                                            if (getOperationResponseResult.isSuccess()) {
                                                completeOperation(
                                                        getOperationResponseResult.getValue().getOperation(),
                                                        pollingOperation
                                                );
                                            }
                                        }
                                );
                    },
                    OPERATION_CHECK_TIMEOUT_MS,
                    TimeUnit.MILLISECONDS
            );
        }

    }

    private CompletableFuture<Result<OperationProtos.CancelOperationResponse>> cancel(
            final PollingOperation operation
    ) {
        return transport.unaryCall(
                OperationServiceGrpc.getCancelOperationMethod(),
                GrpcRequestSettings.newBuilder()
                        .build(),
                OperationProtos.CancelOperationRequest.newBuilder()
                        .setId(operation.id)
                        .build()
        ).whenCompleteAsync(
                (cancelOperationResponseResult, throwable) -> {
                    if (throwable != null) {
                        logger.error("Fail cancel polling operation with id: {}", operation.id, throwable);
                    }

                    if (cancelOperationResponseResult.isSuccess()) {
                        logger.info("Success cancel polling operation with id: {}", operation.id);

                        while (true) {
                            Status status = operation.getStatus();

                            if (status == Status.SUCCESS ||
                                    operation.status.compareAndSet(
                                            status,
                                            Status.of(StatusCode.CANCELLED)
                                    )
                            ) {
                                break;
                            }
                        }
                    } else {
                        logger.error("Fail cancel polling operation with id: {}", operation.id);
                    }
                }
        );
    }

    private static Status fetchStatus(OperationProtos.Operation operation) {
        StatusCode code = StatusCode.fromProto(operation.getStatus());
        Double consumedRu = null;
        if (operation.hasCostInfo()) {
            consumedRu = operation.getCostInfo().getConsumedUnits();
        }

        return Status.of(code, consumedRu, Issue.fromPb(operation.getIssuesList()));
    }

    public static class PollingOperation {

        private final PollingOperationManager operationManager;
        private final AtomicReference<Status> status;
        private final String id;

        private PollingOperation(
                PollingOperationManager operationManager,
                Status status,
                String id
        ) {
            this.operationManager = operationManager;
            this.status = new AtomicReference<>(status);
            this.id = id;
        }

        public Status getStatus() {
            return status.get();
        }

        public CompletableFuture<Result<OperationProtos.CancelOperationResponse>> cancel() {
            return operationManager.cancel(this);
        }
    }
}
