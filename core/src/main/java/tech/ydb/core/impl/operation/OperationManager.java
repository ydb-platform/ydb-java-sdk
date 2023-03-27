package tech.ydb.core.impl.operation;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

import com.google.common.annotations.VisibleForTesting;
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

    public Operation startPollingOperation() {
        return null;
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
        } else {
            while (true) {
                Status status = pollingOperation.getStatus();

                if (status == Status.of(StatusCode.CANCELLED) ||
                        pollingOperation.status.compareAndSet(
                                status,
                                Status.of(StatusCode.fromProto(operation.getStatus()))
                        )
                ) {
                    return;
                }
            }
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
        ).whenComplete(
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

    @VisibleForTesting
    static Status status(OperationProtos.Operation operation) {
        StatusCode code = StatusCode.fromProto(operation.getStatus());
        Double consumedRu = null;
        if (operation.hasCostInfo()) {
            consumedRu = operation.getCostInfo().getConsumedUnits();
        }

        return Status.of(
                code,
                consumedRu,
                Issue.fromPb(operation.getIssuesList())
        );
    }

    public static class PollingOperation<T> {

        private final OperationManager operationManager;
        private final AtomicReference<Status> status; // ComFuture
        private final String id;

        private Result<T> result;

        private PollingOperation(
                OperationManager operationManager,
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
