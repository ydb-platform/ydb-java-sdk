package tech.ydb.core;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

import javax.annotation.Nonnull;


/**
 * @author Sergey Polovko
 * @author Alexandr Gorshenin
 * @param <T> type of result value
 */
public interface Result<T> {
    @Nonnull
    Status getStatus();

    @Nonnull
    T getValue() throws UnexpectedResultException;

    @Nonnull
    <U> Result<U> map(Function<T, U> mapper);

    @Nonnull
    <U> CompletableFuture<Result<U>> mapResultFuture(Function<T, CompletableFuture<Result<U>>> mapper);

    @Nonnull
    CompletableFuture<Status> mapStatusFuture(Function<T, CompletableFuture<Status>> mapper);

    default boolean isSuccess() {
        return getStatus().getCode() == StatusCode.SUCCESS;
    }

    static <V> Result<V> success(@Nonnull V value) {
        return new Success<>(value, Status.SUCCESS);
    }

    static <V> Result<V> success(@Nonnull V value, Status status) {
        return new Success<>(value, Objects.requireNonNull(status));
    }

    static <V> Result<V> fail(@Nonnull Status status) {
        return new Fail<>(Objects.requireNonNull(status));
    }

    static <V> Result<V> fail(UnexpectedResultException unexpected) {
        return new Unexpected<>(Objects.requireNonNull(unexpected));
    }

    static <V> Result<V> error(String message, Throwable throwable) {
        if (throwable != null && throwable instanceof UnexpectedResultException) {
            return new Unexpected<>(message, (UnexpectedResultException) throwable);
        }
        return new Error<>(message, throwable);
    }

    /**
     * Create functor to compose the successful result to next completable future with another result. Failed results
     * will be passed as is.
     * <p>
     * This helper is designed to be used as {@link CompletableFuture#thenCompose(java.util.function.Function) }
     * argument
     * <p>
     * Example of usage:
     * <pre> {@code
     * // Execute one query, with opening new transaction
     * session.executeDataQuery(...)
     *    // Execute second query if first was successful
     *    .thenCompose(Result.compose(fisrt -> session.executeDataQuery(...)))
     *    // Commit transaction after two successful query executions
     *    .thenCompose(Result.composeStatus(second -> session.commitTransaction(...)));
     * }</pre>
     * @param <T> type of value in Result
     * @param <U> type of resulting value in returning future
     * @param mapper mapper from successful value to completable future with another result
     * @return functor which composes successful results to completable future with another result
     */
    static <T, U> Function<Result<T>, CompletableFuture<Result<U>>> compose(
            Function<T, CompletableFuture<Result<U>>> mapper) {
        return result -> result.mapResultFuture(mapper);
    }

    /**
     * Create functor to compose the successful result to next completable future with status. Failed results
     * will be composed to its statuses.
     * <p>
     * This helper is designed to be used as {@link CompletableFuture#thenCompose(java.util.function.Function) }
     * argument
     * <p>
     * Example of usage:
     * <pre> {@code
     * // Execute one query, with opening new transaction
     * session.executeDataQuery(...)
     *    // Execute second query if first was successful
     *    .thenCompose(Result.compose(fisrt -> session.executeDataQuery(...)))
     *    // Commit transaction after two successful query executions
     *    .thenCompose(Result.composeStatus(second -> session.commitTransaction(...)));
     * }</pre>
     * @param <T> type of value in Result
     * @param mapper mapper from successful value to completable future with status
     * @return functor which composes successful results to completable future with status
     */
    static <T> Function<Result<T>, CompletableFuture<Status>> composeStatus(
            Function<T, CompletableFuture<Status>> mapper) {
        return result -> result.mapStatusFuture(mapper);
    }

    /**
     * Create functor to compose the successful result to completed future with specified value. Failed results
     * will be passed as is.
     * <p>
     * This helper is designed to be used as {@link CompletableFuture#thenCompose(java.util.function.Function) }
     * argument
     * <p>
     * Example of usage:
     * <pre> {@code
     * // Execute one query
     * session.executeDataQuery(...)
     *    // Execute second query if first was successful
     *    .thenCompose(Result.compose(fisrt -> session
     *        .executeDataQuery(...)
     *        // But use first request result as the result of
     *        .thenCompose(Result.composeValue(first))
     *    )
     * )
     * }</pre>
     * @param <T> type of value in Result
     * @param <U> type of composed value
     * @param value value to create completed future
     * @return functor which composes successful results to completed future with specified value
     */
    static <T, U> Function<Result<T>, CompletableFuture<Result<U>>> composeValue(U value) {
        return result -> result.mapResultFuture(v -> CompletableFuture.completedFuture(Result.success(value)));
    }

    /*
     * SUCCESS
     */
    final class Success<V> implements Result<V> {
        private final V value;
        private final Status status;

        private Success(V value, Status status) {
            assert status.getCode() == StatusCode.SUCCESS;
            this.value = value;
            this.status = status;
        }

        @Override
        public Status getStatus() {
            return status;
        }

        @Override
        public V getValue() {
            return value;
        }

        @Override
        public <U> Success<U> map(Function<V, U> mapper) {
            return new Success<>(mapper.apply(value), status);
        }

        @Override
        public <U> CompletableFuture<Result<U>> mapResultFuture(Function<V, CompletableFuture<Result<U>>> mapper) {
            return mapper.apply(value);
        }

        @Override
        public CompletableFuture<Status> mapStatusFuture(Function<V, CompletableFuture<Status>> mapper) {
            return mapper.apply(value);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            Success<?> success = (Success<?>) o;
            return Objects.equals(status, success.status)
                    && Objects.equals(value, success.value);
        }

        @Override
        public int hashCode() {
            return Objects.hash(status, value);
        }

        @Override
        public String toString() {
            return "Success{" + value + ", status=" + status + "}";
        }
    }

    /*
     * FAIL
     */
    final class Fail<V> implements Result<V> {
        private final Status status;

        private Fail(Status status) {
            assert status.getCode() != StatusCode.SUCCESS;
            this.status = status;
        }

        @Override
        @SuppressWarnings("unchecked")
        public <U> Fail<U> map(Function<V, U> mapper) {
            return (Fail<U>) this;
        }

        @Override
        @SuppressWarnings("unchecked")
        public <U> CompletableFuture<Result<U>> mapResultFuture(Function<V, CompletableFuture<Result<U>>> mapper) {
            return CompletableFuture.completedFuture((Fail<U>) this);
        }

        @Override
        public CompletableFuture<Status> mapStatusFuture(Function<V, CompletableFuture<Status>> mapper) {
            return CompletableFuture.completedFuture(status);
        }

        @Override
        public Status getStatus() {
            return status;
        }

        @Override
        public V getValue() {
            throw new UnexpectedResultException("Cannot get value", status);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            Fail<?> fail = (Fail<?>) o;
            return Objects.equals(status, fail.status);
        }

        @Override
        public int hashCode() {
            return status.hashCode();
        }

        @Override
        public String toString() {
            return "Fail{" + status + "}";
        }
    }

    final class Unexpected<V> implements Result<V> {
        private final UnexpectedResultException cause;

        private Unexpected(UnexpectedResultException cause) {
            this.cause = cause;
        }

        private Unexpected(String message, UnexpectedResultException cause) {
            this.cause = (message == null || message.isEmpty()) ? cause : new UnexpectedResultException(message, cause);
        }

        @Override
        public Status getStatus() {
            return cause.getStatus();
        }

        @Override
        public V getValue() {
            throw cause;
        }

        @Override
        @SuppressWarnings("unchecked")
        public <U> Unexpected<U> map(Function<V, U> mapper) {
            return (Unexpected<U>) this;
        }

        @Override
        @SuppressWarnings("unchecked")
        public <U> CompletableFuture<Result<U>> mapResultFuture(Function<V, CompletableFuture<Result<U>>> mapper) {
            return CompletableFuture.completedFuture((Unexpected<U>) this);
        }

        @Override
        public CompletableFuture<Status> mapStatusFuture(Function<V, CompletableFuture<Status>> mapper) {
            return CompletableFuture.completedFuture(cause.getStatus());
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            Unexpected<?> error = (Unexpected<?>) o;
            return Objects.equals(cause, error.cause);
        }

        @Override
        public int hashCode() {
            return Objects.hash(cause);
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder("Unexpected{message='").append(cause.getMessage()).append("'");
            if (cause.getCause() != null) {
                sb.append(", cause=").append(cause.getCause());
            }
            return sb.append("}").toString();
        }
    }

    /*
     * ERROR
     */
    final class Error<V> implements Result<V> {
        private static final Status ERROR = Status.of(StatusCode.CLIENT_INTERNAL_ERROR);
        private final String message;
        private final Status status;

        private Error(String message, Throwable cause) {
            this.message = message;
            this.status = ERROR.withCause(cause);
        }

        @Override
        public Status getStatus() {
            return status;
        }

        @Override
        public V getValue() {
            throw new UnexpectedResultException(message, status);
        }

        @Override
        @SuppressWarnings("unchecked")
        public <U> Error<U> map(Function<V, U> mapper) {
            return (Error<U>) this;
        }

        @Override
        @SuppressWarnings("unchecked")
        public <U> CompletableFuture<Result<U>> mapResultFuture(Function<V, CompletableFuture<Result<U>>> mapper) {
            CompletableFuture<Result<U>> future = new CompletableFuture<>();
            future.completeExceptionally(status.getCause());
            return future;
        }

        @Override
        public CompletableFuture<Status> mapStatusFuture(Function<V, CompletableFuture<Status>> mapper) {
            return CompletableFuture.completedFuture(status);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            Error<?> error = (Error<?>) o;
            return Objects.equals(message, error.message)
                    && Objects.equals(status, error.status);
        }

        @Override
        public int hashCode() {
            return Objects.hash(message, status);
        }

        @Override
        public String toString() {
            return "Error{message='" + message + "', cause=" + status.getCause() + '}';
        }
    }
}
