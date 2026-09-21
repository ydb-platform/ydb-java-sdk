package tech.ydb.core.grpc;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tech.ydb.core.Issue;
import tech.ydb.core.Result;
import tech.ydb.core.Status;
import tech.ydb.core.StatusCode;

/**
 * @author Sergey Polovko
 */
public final class GrpcStatuses {
    private static final Logger logger = LoggerFactory.getLogger(GrpcStatuses.class);

    private GrpcStatuses() { }

    public static <T> Result<T> toResult(io.grpc.Status status, String endpoint) {
        assert !status.isOk();
        return Result.fail(toStatus(status, endpoint));
    }

    public static tech.ydb.core.Status toStatus(io.grpc.Status status, String endpoint) {
        if (status.isOk()) {
            return Status.SUCCESS;
        }
        Issue message = Issue.of(getMessage(status, endpoint), Issue.Severity.ERROR);
        StatusCode code = getStatusCode(status);
        Throwable cause = status.getCause();

        if (cause == null) {
            return Status.of(code, cause, message);
        }
        return Status.of(code, cause, message, Issue.of(cause.toString(), Issue.Severity.ERROR));
    }

    private static String getMessage(io.grpc.Status status, String endpoint) {
        if (status.getCode() == io.grpc.Status.Code.CANCELLED) {
            logger.debug("gRPC cancellation: {}, {}", status.getCode(), status.getDescription());
        } else {
            logger.warn("gRPC issue: {}, {}", status.getCode(), status.getDescription());
        }

        String message = "gRPC error: (" + status.getCode() + ") on " + endpoint;
        if (status.getDescription() != null) {
            message += ", " + status.getDescription();
        }
        return message;
    }

    private static StatusCode getStatusCode(io.grpc.Status status) {
        switch (status.getCode()) {
            case UNAVAILABLE: return StatusCode.TRANSPORT_UNAVAILABLE;
            case UNAUTHENTICATED: return StatusCode.CLIENT_UNAUTHENTICATED;
            case CANCELLED: return StatusCode.CLIENT_CANCELLED;
            case UNIMPLEMENTED: return StatusCode.CLIENT_CALL_UNIMPLEMENTED;
            case DEADLINE_EXCEEDED: return StatusCode.CLIENT_DEADLINE_EXCEEDED;
            case RESOURCE_EXHAUSTED:
                // A message that exceeds the gRPC size limit is a deterministic client-side error:
                // retrying it with the same payload always fails identically. Map it to a non-retryable
                // status so the caller fails fast instead of spinning through the retry budget.
                // Other ResourceExhausted errors (e.g. quota or rate limiting) stay retryable.
                if (isMessageLargerThanMax(status.getDescription())) {
                    return StatusCode.BAD_REQUEST;
                }
                return StatusCode.CLIENT_RESOURCE_EXHAUSTED;
            default:
                return StatusCode.CLIENT_GRPC_ERROR;
        }
    }

    private static boolean isMessageLargerThanMax(String description) {
        return description != null && (description.contains("trying to send message larger than max")
                || description.contains("received message larger than max"));
    }
}
