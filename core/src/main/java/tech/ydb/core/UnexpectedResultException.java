package tech.ydb.core;

import java.util.Objects;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;


/**
 * @author Sergey Polovko
 */
@ParametersAreNonnullByDefault
public class UnexpectedResultException extends RuntimeException {
    private static final long serialVersionUID = 2450224259821940035L;

    private final Status status;

    public UnexpectedResultException(String message, Status status) {
        this(message, status, null);
    }

    public UnexpectedResultException(String message, Status status, Throwable cause) {
        super(formatMessage(message, status), cause);
        this.status = Objects.requireNonNull(status);
    }

    UnexpectedResultException(String message, UnexpectedResultException other) {
        super(message + ": " + other.getMessage(), other.getCause());
        this.status = other.status;
        this.setStackTrace(other.getStackTrace());
    }

    @Nonnull
    public Status getStatus() {
        return status;
    }

    private static String formatMessage(String message, Status status) {
        StringBuilder sb = new StringBuilder(64);
        if (!message.isEmpty()) {
            sb.append(message).append(", ");
        }
        sb.append("code: ").append(status.getCode().name());
        if (status.hasConsumedRu()) {
            sb.append(", consumed ").append(status.getConsumedRu()).append(" RU");
        }
        Issue[] issues = status.getIssues();
        if (issues.length != 0) {
            sb.append(", issues: [");
            for (Issue issue : issues) {
                issue.toString(sb);
                sb.append(", ");
            }
            sb.setLength(sb.length() - 2); // cut last ", "
            sb.append(']');
        }
        return sb.toString();
    }
}
