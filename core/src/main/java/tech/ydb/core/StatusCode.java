package tech.ydb.core;

import java.util.EnumSet;

import tech.ydb.proto.StatusCodesProtos.StatusIds;

import static tech.ydb.core.Constants.INTERNAL_CLIENT_FIRST;
import static tech.ydb.core.Constants.SERVER_STATUSES_FIRST;
import static tech.ydb.core.Constants.TRANSPORT_STATUSES_FIRST;
import static tech.ydb.core.Constants.TRANSPORT_STATUSES_LAST;


/**
 * @author Sergey Polovko
 */
public enum StatusCode {
    UNUSED_STATUS(0),

    // Server statuses
    SUCCESS(SERVER_STATUSES_FIRST),
    BAD_REQUEST(SERVER_STATUSES_FIRST + 10),
    UNAUTHORIZED(SERVER_STATUSES_FIRST + 20),
    INTERNAL_ERROR(SERVER_STATUSES_FIRST + 30),
    ABORTED(SERVER_STATUSES_FIRST + 40),
    UNAVAILABLE(SERVER_STATUSES_FIRST + 50),
    OVERLOADED(SERVER_STATUSES_FIRST + 60),
    SCHEME_ERROR(SERVER_STATUSES_FIRST + 70),
    GENERIC_ERROR(SERVER_STATUSES_FIRST + 80),
    TIMEOUT(SERVER_STATUSES_FIRST + 90),
    BAD_SESSION(SERVER_STATUSES_FIRST + 100),
    PRECONDITION_FAILED(SERVER_STATUSES_FIRST + 120),
    ALREADY_EXISTS(SERVER_STATUSES_FIRST + 130),
    NOT_FOUND(SERVER_STATUSES_FIRST + 140),
    SESSION_EXPIRED(SERVER_STATUSES_FIRST + 150),
    CANCELLED(SERVER_STATUSES_FIRST + 160),
    UNDETERMINED(SERVER_STATUSES_FIRST + 170),
    UNSUPPORTED(SERVER_STATUSES_FIRST + 180),
    SESSION_BUSY(SERVER_STATUSES_FIRST + 190),
    EXTERNAL_ERROR(SERVER_STATUSES_FIRST + 200),

    // Client statuses
    /** Cannot connect or unrecoverable network error. (map from gRPC UNAVAILABLE) */
    TRANSPORT_UNAVAILABLE(TRANSPORT_STATUSES_FIRST + 10),

    /** No more resources to accept RPC call */
    CLIENT_RESOURCE_EXHAUSTED(TRANSPORT_STATUSES_FIRST + 20),

    /** Network layer does not receive response in given time */
    CLIENT_DEADLINE_EXCEEDED(TRANSPORT_STATUSES_FIRST + 30),

    /** Unknown client error */
    CLIENT_INTERNAL_ERROR(TRANSPORT_STATUSES_FIRST + 50),
    CLIENT_CANCELLED(TRANSPORT_STATUSES_FIRST + 60),
    CLIENT_UNAUTHENTICATED(TRANSPORT_STATUSES_FIRST + 70),

    // Unknown gRPC call
    CLIENT_CALL_UNIMPLEMENTED(TRANSPORT_STATUSES_FIRST + 80),
    CLIENT_DISCOVERY_FAILED(INTERNAL_CLIENT_FIRST + 10),
    CLIENT_LIMITS_REACHED(INTERNAL_CLIENT_FIRST + 20),

    /** Deadline expired before request was sent to server */
    CLIENT_DEADLINE_EXPIRED(INTERNAL_CLIENT_FIRST + 30);

    private static final EnumSet<StatusCode> RETRYABLE_STATUSES = EnumSet.of(
            ABORTED,
            UNAVAILABLE,
            OVERLOADED,
            CLIENT_RESOURCE_EXHAUSTED,
            BAD_SESSION,
            SESSION_BUSY
    );

    private static final EnumSet<StatusCode> IDEMPOTENT_RETRYABLE_STATUSES = EnumSet.of(
            CLIENT_CANCELLED,
            CLIENT_INTERNAL_ERROR,
            UNDETERMINED,
            TRANSPORT_UNAVAILABLE
    );

    private final int code;

    StatusCode(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }

    public boolean isTransportError() {
        return code >= TRANSPORT_STATUSES_FIRST && code <= TRANSPORT_STATUSES_LAST;
    }

    public boolean isRetryable(boolean isOperationIdempotent, boolean retryNotFound) {
        if (RETRYABLE_STATUSES.contains(this)) {
            return true;
        }
        switch (this) {
            case NOT_FOUND:
                return retryNotFound;
            case CLIENT_CANCELLED:
            case CLIENT_INTERNAL_ERROR:
            case UNDETERMINED:
            case TRANSPORT_UNAVAILABLE:
                return isOperationIdempotent;
            default:
                break;
        }
        return false;
    }

    public static StatusCode fromProto(StatusIds.StatusCode code) {
        switch (code) {
            case SUCCESS: return SUCCESS;
            case BAD_REQUEST: return BAD_REQUEST;
            case UNAUTHORIZED: return UNAUTHORIZED;
            case INTERNAL_ERROR: return INTERNAL_ERROR;
            case ABORTED: return ABORTED;
            case UNAVAILABLE: return UNAVAILABLE;
            case OVERLOADED: return OVERLOADED;
            case SCHEME_ERROR: return SCHEME_ERROR;
            case GENERIC_ERROR: return GENERIC_ERROR;
            case TIMEOUT: return TIMEOUT;
            case BAD_SESSION: return BAD_SESSION;
            case PRECONDITION_FAILED: return PRECONDITION_FAILED;
            case ALREADY_EXISTS: return ALREADY_EXISTS;
            case NOT_FOUND: return NOT_FOUND;
            case SESSION_EXPIRED: return SESSION_EXPIRED;
            case CANCELLED: return CANCELLED;
            case UNDETERMINED: return UNDETERMINED;
            case UNSUPPORTED: return UNSUPPORTED;
            case SESSION_BUSY: return SESSION_BUSY;
            case EXTERNAL_ERROR: return EXTERNAL_ERROR;
            default:
                return UNUSED_STATUS;
        }
    }

    public boolean isRetryable(boolean idempotent) {
        return RETRYABLE_STATUSES.contains(this) || (idempotent && IDEMPOTENT_RETRYABLE_STATUSES.contains(this));
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(name());
        if (this != SUCCESS) {
            sb = sb.append("(code=").append(code).append(")");
        }
        return sb.toString();
    }
}
