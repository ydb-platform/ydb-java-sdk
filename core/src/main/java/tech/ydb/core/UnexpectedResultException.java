package tech.ydb.core;

import java.util.Arrays;


/**
 * @author Sergey Polovko
 */
public class UnexpectedResultException extends RuntimeException {

    private final StatusCode statusCode;
    private final Issue[] issues;

    public UnexpectedResultException(String message, StatusCode statusCode, Throwable cause) {
        super(message, cause);
        this.statusCode = statusCode;
        this.issues = Issue.EMPTY_ARRAY;
    }

    public UnexpectedResultException(String message, StatusCode statusCode, Issue[] issues) {
        super(message + ", code: " + statusCode + ", issues: " + Arrays.toString(issues));
        this.statusCode = statusCode;
        this.issues = issues;
    }

    public StatusCode getStatusCode() {
        return statusCode;
    }

    public Issue[] getIssues() {
        return issues;
    }
}
