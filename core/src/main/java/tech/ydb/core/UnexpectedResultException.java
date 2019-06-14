package tech.ydb.core;

import javax.annotation.ParametersAreNonnullByDefault;


/**
 * @author Sergey Polovko
 */
@ParametersAreNonnullByDefault
public class UnexpectedResultException extends RuntimeException {

    private final StatusCode statusCode;
    private final Issue[] issues;

    public UnexpectedResultException(String message, StatusCode statusCode) {
        this(message, statusCode, Issue.EMPTY_ARRAY);
    }

    public UnexpectedResultException(String message, StatusCode statusCode, Throwable cause) {
        super(message, cause);
        this.statusCode = statusCode;
        this.issues = Issue.EMPTY_ARRAY;
    }

    public UnexpectedResultException(String message, StatusCode statusCode, Issue... issues) {
        super(formatMessage(message, statusCode, issues));
        this.statusCode = statusCode;
        this.issues = issues;
    }

    public StatusCode getStatusCode() {
        return statusCode;
    }

    public Issue[] getIssues() {
        return issues;
    }

    private static String formatMessage(String message, StatusCode statusCode, Issue[] issues) {
        StringBuilder sb = new StringBuilder(64);
        if (!message.isEmpty()) {
            sb.append(message).append(", ");
        }
        sb.append("code: ").append(statusCode.name());
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
