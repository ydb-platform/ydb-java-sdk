package tech.ydb.core.operation;

import java.util.function.Function;

import com.google.common.annotations.VisibleForTesting;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;

import tech.ydb.core.Issue;
import tech.ydb.core.Result;
import tech.ydb.core.Status;
import tech.ydb.core.StatusCode;
import tech.ydb.core.grpc.GrpcTransport;
import tech.ydb.proto.OperationProtos;

/**
 *
 * @author Aleksandr Gorshenin
 */
public class OperationBinder {
    @VisibleForTesting
    static final Status UNEXPECTED_ASYNC = Status.of(StatusCode.CLIENT_INTERNAL_ERROR)
            .withIssues(Issue.of("Unexpected async result of operation", Issue.Severity.ERROR));

    private OperationBinder() { }

    private static Status status(OperationProtos.Operation operation) {
        StatusCode code = StatusCode.fromProto(operation.getStatus());
        Double consumedRu = null;
        if (operation.hasCostInfo()) {
            consumedRu = operation.getCostInfo().getConsumedUnits();
        }

        return Status.of(code, consumedRu, Issue.fromPb(operation.getIssuesList()));
    }

    private static <M extends Message> Result<M> result(OperationProtos.Operation operation, Class<M> resultClass) {
        Status status = status(operation);
        if (!status.isSuccess()) {
            return Result.fail(status);
        }

        try {
            M resultMessage = operation.getResult().unpack(resultClass);
            return Result.success(resultMessage, status);
        } catch (InvalidProtocolBufferException ex) {
            return Result.error("Can't unpack message " + resultClass.getName(), ex);
        }
    }

    public static <R, M extends Message> Function<Result<R>, Result<M>> bindSync(
            Function<R, OperationProtos.Operation> method, Class<M> resultClass
    ) {
        return (result) -> {
            if (!result.isSuccess()) {
                return result.map(null);
            }
            OperationProtos.Operation operation = method.apply(result.getValue());
            if (!operation.getReady()) {
                return Result.fail(UNEXPECTED_ASYNC);
            }
            return result(operation, resultClass);
        };
    }

    public static <R> Function<Result<R>, Status> bindSync(Function<R, OperationProtos.Operation> method) {
        return (result) -> {
            if (!result.isSuccess()) {
                return result.getStatus();
            }
            OperationProtos.Operation operation = method.apply(result.getValue());
            if (!operation.getReady()) {
                return UNEXPECTED_ASYNC;
            }
            return status(operation);
        };
    }

    public static <R> Function<Result<R>, Operation<Status>> bindAsync(
            GrpcTransport transport, Function<R, OperationProtos.Operation> method
    ) {
        return (result) -> {
            if (!result.isSuccess()) {
                Status status = result.getStatus();
                return new FailedOperation<>(status, status);
            }

            OperationProtos.Operation operation = method.apply(result.getValue());
            return new OperationImpl<>(transport, operation, OperationBinder::status);
        };
    }

    public static <R, M extends Message> Function<Result<R>, Operation<Result<M>>> bindAsync(
            GrpcTransport transport, Function<R, OperationProtos.Operation> method, Class<M> resultClass
    ) {
        return (result) -> {
            if (!result.isSuccess()) {
                Status status = result.getStatus();
                return new FailedOperation<>(Result.fail(status), status);
            }

            OperationProtos.Operation operation = method.apply(result.getValue());
            return new OperationImpl<>(transport, operation, o -> result(o, resultClass));
        };
    }
}
