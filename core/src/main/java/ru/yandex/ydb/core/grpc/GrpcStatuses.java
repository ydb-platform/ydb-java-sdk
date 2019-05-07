package ru.yandex.ydb.core.grpc;

import io.grpc.Status;

import ru.yandex.ydb.core.Issue;
import ru.yandex.ydb.core.Result;
import ru.yandex.ydb.core.StatusCode;

import static ru.yandex.yql.proto.IssueSeverity.TSeverityIds.ESeverityId.S_ERROR;


/**
 * @author Sergey Polovko
 */
public final class GrpcStatuses {
    private GrpcStatuses() {}


    public static <T> Result<T> translate(Status status) {
        final StatusCode code;
        switch (status.getCode()) {
            case UNAVAILABLE:
                code = StatusCode.TRANSPORT_UNAVAILABLE;
                break;
            case UNAUTHENTICATED:
                code = StatusCode.CLIENT_UNAUTHENTICATED;
                break;
            case CANCELLED:
                code = StatusCode.CLIENT_CANCELLED;
                break;
            case UNIMPLEMENTED:
                code = StatusCode.CLIENT_CALL_UNIMPLEMENTED;
                break;
            default:
                code = StatusCode.CLIENT_INTERNAL_ERROR;
                break;
        }

        String message = "gRPC error: (" + status.getCode() + ')';
        if (status.getDescription() != null) {
            message += ' ' + status.getDescription();
        }
        return Result.fail(code, Issue.of(message, S_ERROR));
    }
}
