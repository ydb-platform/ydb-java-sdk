package tech.ydb.core;

import java.util.function.Function;

import tech.ydb.OperationProtos;
import tech.ydb.OperationProtos.Operation;
import tech.ydb.StatusCodesProtos.StatusIds;

import com.google.common.annotations.VisibleForTesting;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;


/**
 * @author Sergey Polovko
 */
public final class Operations {
    private Operations() {}
    
    private final static Status ASYNC_ARE_UNSUPPORTED = Status.of(
            StatusCode.CLIENT_INTERNAL_ERROR,
            Issue.of("Async operations are not supported", Issue.Severity.ERROR)
    );

    @VisibleForTesting
    static Status status(Operation operation) {
        if (operation.getStatus() == StatusIds.StatusCode.SUCCESS && operation.getIssuesCount() == 0) {
            return Status.SUCCESS;
        }
        StatusCode code = StatusCode.fromProto(operation.getStatus());
        return Status.of(code, Issue.fromPb(operation.getIssuesList()));
    }

    public static <R, M extends Message> Function<Result<R>, Result<M>> resultUnwrapper(
        Function<R, OperationProtos.Operation> operationExtractor,
        Class<M> resultClass)
    {
        return (result) -> {
            if (!result.isSuccess()) {
                return result.cast();
            }
            OperationProtos.Operation operation = operationExtractor.apply(result.expect("can't read message"));
            if (operation.getReady()) {
                Status status = status(operation);
                if (!status.isSuccess()) {
                    return Result.fail(status);
                }

                try {
                    Double consumedRu = null;
                    if (operation.hasCostInfo()) {
                        consumedRu = operation.getCostInfo().getConsumedUnits();
                    }
                    M resultMessage = operation.getResult().unpack(resultClass);
                    return Result.success(resultMessage, consumedRu, status.getIssues());
                } catch (InvalidProtocolBufferException ex) {
                    return Result.error("Can't unpack message " + resultClass.getName(), ex);
                }
            }
            return Result.fail(ASYNC_ARE_UNSUPPORTED);
        };
    }

    public static <R> Function<Result<R>, Status> statusUnwrapper(
        Function<R, OperationProtos.Operation> operationExtractor)
    {
        return (result) -> {
            if (!result.isSuccess()) {
                return result.toStatus();
            }

            OperationProtos.Operation operation = operationExtractor.apply(result.expect("can't read message"));
            if (operation.getReady()) {
                return status(operation);
            }

            return ASYNC_ARE_UNSUPPORTED;
        };
    }
}
