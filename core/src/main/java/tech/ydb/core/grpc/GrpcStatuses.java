package tech.ydb.core.grpc;

import io.grpc.Status;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tech.ydb.core.Issue;
import tech.ydb.core.Result;
import tech.ydb.core.StatusCode;

/**
 * @author Sergey Polovko
 */
public final class GrpcStatuses {
    private static final Logger logger = LoggerFactory.getLogger(GrpcStatuses.class);

    private GrpcStatuses() { }

    public static <T> Result<T> toResult(Status status) {
        assert !status.isOk();
        String message = getMessage(status);

        Throwable cause = status.getCause();
        if (cause != null && status.getCode() == Status.Code.INTERNAL) {
            return Result.error(message, cause);
        }

        StatusCode code = getStatusCode(status.getCode());
        return Result.fail(tech.ydb.core.Status.of(code, null, Issue.of(message, Issue.Severity.ERROR)));
    }

    public static tech.ydb.core.Status toStatus(Status status) {
        if (status.isOk()) {
            return tech.ydb.core.Status.SUCCESS;
        }
        Issue message = Issue.of(getMessage(status), Issue.Severity.ERROR);
        StatusCode code = getStatusCode(status.getCode());
        Throwable cause = status.getCause();

        if (cause == null) {
            return tech.ydb.core.Status.of(code, null, message);
        }

        return tech.ydb.core.Status.of(code, null, message, Issue.of(cause.getMessage(), Issue.Severity.ERROR));
    }

    private static String getMessage(Status status) {
        if (status.getCode() == Status.Code.CANCELLED) {
            logger.debug("gRPC cancellation: {}, {}", status.getCode(), status.getDescription());
        } else {
            logger.warn("gRPC issue: {}, {}", status.getCode(), status.getDescription());
        }

        String message = "gRPC error: (" + status.getCode() + ')';
        return status.getDescription() == null
            ? message
            : message + ' ' + status.getDescription();
    }

    private static StatusCode getStatusCode(Status.Code code) {
        switch (code) {
            case UNAVAILABLE: return StatusCode.TRANSPORT_UNAVAILABLE;
            case UNAUTHENTICATED: return StatusCode.CLIENT_UNAUTHENTICATED;
            case CANCELLED: return StatusCode.CLIENT_CANCELLED;
            case UNIMPLEMENTED: return StatusCode.CLIENT_CALL_UNIMPLEMENTED;
            case DEADLINE_EXCEEDED: return StatusCode.CLIENT_DEADLINE_EXCEEDED;
            case RESOURCE_EXHAUSTED: return StatusCode.CLIENT_RESOURCE_EXHAUSTED;
            default:
                return StatusCode.CLIENT_INTERNAL_ERROR;
        }
    }
}
