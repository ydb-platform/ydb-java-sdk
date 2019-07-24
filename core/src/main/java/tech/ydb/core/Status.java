package tech.ydb.core;

import java.util.Arrays;


/**
 * @author Sergey Polovko
 */
public final class Status {

    public static final Status SUCCESS = new Status(StatusCode.SUCCESS, Issue.EMPTY_ARRAY);

    private final StatusCode code;
    private final Issue[] issues;

    private Status(StatusCode code, Issue[] issues) {
        this.code = code;
        this.issues = issues;
    }

    public static Status of(StatusCode code) {
        if (code == StatusCode.SUCCESS) {
            return SUCCESS;
        }
        return new Status(code, Issue.EMPTY_ARRAY);
    }

    public static Status of(StatusCode code, Issue... issues) {
        if (code == StatusCode.SUCCESS && issues.length == 0) {
            return SUCCESS;
        }
        return new Status(code, issues);
    }

    public StatusCode getCode() {
        return code;
    }

    public Issue[] getIssues() {
        return issues;
    }

    public boolean isSuccess() {
        return code == StatusCode.SUCCESS;
    }

    public void expect(String message) {
        if (!isSuccess()) {
            throw new UnexpectedResultException(message, code, issues);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Status status = (Status) o;
        if (code != status.code) return false;
        return Arrays.equals(issues, status.issues);
    }

    @Override
    public int hashCode() {
        return 31 * code.hashCode() + Arrays.hashCode(issues);
    }

    @Override
    public String toString() {
        return "Status{code=" + code + ", issues=" + Arrays.toString(issues) + '}';
    }
}
