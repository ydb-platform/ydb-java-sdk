package tech.ydb.core;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Objects;


/**
 * @author Sergey Polovko
 * @author Alexandr Gorshenin
 */
public final class Status implements Serializable {
    private static final long serialVersionUID = -2966026377652094862L;

    public static final Status SUCCESS = new Status(StatusCode.SUCCESS, null, Issue.EMPTY_ARRAY);

    private final StatusCode code;
    private final Double consumedRu;
    private final Issue[] issues;

    private Status(StatusCode code, Double consumedRu, Issue[] issues) {
        this.code = code;
        this.consumedRu = consumedRu;
        this.issues = issues;
    }

    public static Status of(StatusCode code, Double consumedRu, Issue... issues) {
        boolean hasIssues = issues != null && issues.length > 0;
        if (code == StatusCode.SUCCESS && consumedRu == null && !hasIssues) {
            return SUCCESS;
        }
        return new Status(code, consumedRu, hasIssues ? issues : Issue.EMPTY_ARRAY);
    }

    public boolean hasConsumedRu() {
        return consumedRu != null;
    }

    public Double getConsumedRu() {
        return consumedRu;
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

    public void expectSuccess(String errorMsg) throws UnexpectedResultException {
        if (!isSuccess()) {
            throw new UnexpectedResultException(errorMsg, this);
        }
    }

    public void expectSuccess() throws UnexpectedResultException {
        expectSuccess("Expected success status, but got " + getCode());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        Status status = (Status) o;
        return code == status.code
                && Objects.equals(consumedRu, status.consumedRu)
                && Arrays.equals(issues, status.issues);
    }

    @Override
    public int hashCode() {
        return Objects.hash(code, consumedRu, issues);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("Status{code = ").append(code);
        if (consumedRu != null) {
            sb.append(", consumed RU = ").append(consumedRu);
        }
        if (issues != null && issues.length > 0) {
            sb.append(", issues = ").append(Arrays.toString(issues));
        }
        return sb.append("}").toString();
    }
}
