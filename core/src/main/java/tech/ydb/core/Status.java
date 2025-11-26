package tech.ydb.core;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.function.Supplier;


/**
 * @author Sergey Polovko
 * @author Alexandr Gorshenin
 */
public final class Status implements Serializable {
    private static final long serialVersionUID = -2966026377652094862L;

    public static final Status SUCCESS = new Status(StatusCode.SUCCESS, null, Issue.EMPTY_ARRAY, null);

    private final StatusCode code;
    private final Double consumedRu;
    private final Issue[] issues;
    private final Throwable cause;

    private Status(StatusCode code, Double consumedRu, Issue[] issues, Throwable cause) {
        this.code = code;
        this.consumedRu = consumedRu;
        this.issues = issues;
        this.cause = cause;
    }

    /**
     * Create functor to compose the successful status to next completable future with another status. Failed statuses
     * will be passed as is.
     * <p>
     * This helper is designed to be used as {@link CompletableFuture#thenCompose(java.util.function.Function) }
     * argument
     * <p>
     * Example of usage:
     * <pre> {@code
     * // Create one table
     * session.createTable(...)
     *    // Create second table if first was created successful
     *    .thenCompose(Status.compose(() -> session.createTable(...)));
     * }</pre>
     * @param supplier generator for completable future with another status
     * @return functor which composes successful status to completable future with another status
     */
    public static Function<Status, CompletableFuture<Status>> compose(
            Supplier<CompletableFuture<Status>> supplier) {
        return status -> status.isSuccess() ? supplier.get() : CompletableFuture.completedFuture(status);
    }

    /**
     * Create functor to compose the successful status to next completable future with result. Failed statuses
     * will be composed to failed results.
     * <p>
     * This helper is designed to be used as {@link CompletableFuture#thenCompose(java.util.function.Function) }
     * argument
     * <p>
     * Example of usage:
     * <pre> {@code
     * // Create one table
     * session.createTable(...)
     *    // Execute query if table was created successful
     *    .thenCompose(Status.composeResult(() -> session.executeDataQuery(...)));
     * }</pre>
     * @param <T> type of value in Result
     * @param supplier generator for completable future with result
     * @return functor which composes successful status to completable future with result
     */
    public static <T> Function<Status, CompletableFuture<Result<T>>> composeResult(
            Supplier<CompletableFuture<Result<T>>> supplier) {
        return status -> status.isSuccess() ? supplier.get() : CompletableFuture.completedFuture(Result.fail(status));
    }

    /**
     * Create functor to compose the successful status to completed future with specified value. Failed statuses
     * will be composed to failed results.
     * <p>
     * This helper is designed to be used as {@link CompletableFuture#thenCompose(java.util.function.Function) }
     * argument
     * <p>
     * Example of usage:
     * <pre> {@code
     * // Start new transaction
     * session.beginTransaction(...)
     *    // Execute query in opened transaction
     *    .thenCompose(Result.compose(transaction -> session.executeDataQuery(...)
     *        // Commit transaction
     *        .thenCompose(Result.compose(result -> transaction.commit()
     *            // And return result of query if commit was successful
     *            .thenCompose(Status.bindValue(result.getResultSet(0)))
     *        ))
     *    ));
     * }</pre>
     * @param <T> type of value in Result
     * @param value value to create completed future
     * @return functor which composes successful status to completed future with specified value
     */
    public static <T> Function<Status, CompletableFuture<Result<T>>> bindValue(T value) {
        return status ->  {
            Result<T> res = status.isSuccess() ? Result.success(value) : Result.fail(status);
            return CompletableFuture.completedFuture(res);
        };
    }

    public static Status of(StatusCode code, Double consumedRu, Issue... issues) {
        boolean hasIssues = issues != null && issues.length > 0;
        if (code == StatusCode.SUCCESS && consumedRu == null && !hasIssues) {
            return SUCCESS;
        }
        return new Status(code, consumedRu, hasIssues ? issues : Issue.EMPTY_ARRAY, null);
    }

    public static Status of(StatusCode code, Issue... issues) {
        boolean hasIssues = issues != null && issues.length > 0;
        if (code == StatusCode.SUCCESS && !hasIssues) {
            return SUCCESS;
        }
        return new Status(code, null, issues, null);
    }

    public static Status of(StatusCode code, Throwable cause, Issue... issues) {
        boolean hasIssues = issues != null && issues.length > 0;
        if (code == StatusCode.SUCCESS && cause == null && !hasIssues) {
            return SUCCESS;
        }
        return new Status(code, null, hasIssues ? issues : Issue.EMPTY_ARRAY, cause);
    }

    public static Status of(StatusCode code) {
        if (code == StatusCode.SUCCESS) {
            return SUCCESS;
        }
        return new Status(code, null, Issue.EMPTY_ARRAY, null);
    }

    public Status withIssues(Issue... newIssues) {
        if (Arrays.equals(this.issues, newIssues)) {
            return this;
        }
        return new Status(this.code, this.consumedRu, newIssues, null);
    }

    public Status withConsumedRu(Double newConsumedRu) {
        if (Objects.equals(this.consumedRu, newConsumedRu)) {
            return this;
        }
        return new Status(this.code, newConsumedRu, this.issues, null);
    }

    public Status withCause(Throwable cause) {
        if (Objects.equals(this.cause, cause)) {
            return this;
        }
        return new Status(this.code, this.consumedRu, this.issues, cause);
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

    public Throwable getCause() {
        return cause;
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
                && Arrays.equals(issues, status.issues)
                && Objects.equals(cause, status.cause);
    }

    @Override
    public int hashCode() {
        int h1 = Objects.hash(code, consumedRu, cause);
        int h2 = Objects.hash((Object[]) issues);
        return 31 * h1 + h2;
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
        if (cause != null) {
            sb.append(", cause = ").append(cause.getMessage());
        }
        return sb.append("}").toString();
    }
}
