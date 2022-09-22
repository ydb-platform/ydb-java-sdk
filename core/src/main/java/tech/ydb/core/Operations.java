package tech.ydb.core;

import java.util.function.Function;

import com.google.common.annotations.VisibleForTesting;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;

import tech.ydb.OperationProtos;
import tech.ydb.OperationProtos.Operation;


/**
 * @author Sergey Polovko
 */
public final class Operations {
    private static final Status ASYNC_ARE_UNSUPPORTED = Status.of(
            StatusCode.CLIENT_INTERNAL_ERROR, null,
            Issue.of("Async operations are not supported", Issue.Severity.ERROR)
    );

    private Operations() { }

    @VisibleForTesting
    static Status status(Operation operation) {
        StatusCode code = StatusCode.fromProto(operation.getStatus());
        Double consumedRu = null;
        if (operation.hasCostInfo()) {
            consumedRu = operation.getCostInfo().getConsumedUnits();
        }
        return Status.of(code, consumedRu, Issue.fromPb(operation.getIssuesList()));
    }

    public static <R, M extends Message> Function<Result<R>, Result<M>> resultUnwrapper(
        Function<R, OperationProtos.Operation> operationExtractor,
        Class<M> resultClass) {
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
                    M resultMessage = operation.getResult().unpack(resultClass);
                    return Result.success(resultMessage, status);
                } catch (InvalidProtocolBufferException ex) {
                    return Result.error("Can't unpack message " + resultClass.getName(), ex);
                }
            }
            return Result.fail(ASYNC_ARE_UNSUPPORTED);
        };
    }

    public static <R> Function<Result<R>, Status> statusUnwrapper(
        Function<R, OperationProtos.Operation> operationExtractor) {
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
}
