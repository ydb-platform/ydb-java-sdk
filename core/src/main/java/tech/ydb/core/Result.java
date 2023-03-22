package tech.ydb.core;

import java.util.Objects;
import java.util.function.Function;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;


/**
 * @author Sergey Polovko
 * @author Alexandr Gorshenin
 * @param <T> type of result value
 */
public interface Result<T> {
    @Nonnull
    Status getStatus();

    @Nullable
    T getValue() throws UnexpectedResultException;

    @Nonnull
    <U> Result<U> map(Function<T, U> mapper);

    default boolean isSuccess() {
        return getStatus().getCode() == StatusCode.SUCCESS;
    }

    static <V> Result<V> success(V value) {
        return new Success<>(value, Status.SUCCESS);
    }

    static <V> Result<V> success(V value, Status status) {
        return new Success<>(value, Objects.requireNonNull(status));
    }

    static <V> Result<V> fail(Status status) {
        return new Fail<>(Objects.requireNonNull(status));
    }

    static <V> Result<V> fail(UnexpectedResultException unexpected) {
        return new Unexpected<>(Objects.requireNonNull(unexpected));
    }

    static <V> Result<V> error(String message, Throwable throwable) {
        if (throwable instanceof UnexpectedResultException) {
            return new Unexpected<>(message, (UnexpectedResultException) throwable);
        }
        return new Error<>(message, throwable);
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
        private final Throwable cause;
        private final Status status;

        private Error(String message, Throwable cause) {
            this.message = message;
            this.status = ERROR;
            this.cause = cause;
        }

        @Override
        public Status getStatus() {
            return status;
        }

        @Override
        public V getValue() {
            throw new UnexpectedResultException(message, status, cause);
        }

        @Override
        @SuppressWarnings("unchecked")
        public <U> Error<U> map(Function<V, U> mapper) {
            return (Error<U>) this;
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
                    && Objects.equals(status, error.status)
                    && Objects.equals(cause, error.cause);
        }

        @Override
        public int hashCode() {
            return Objects.hash(message, status, cause);
        }

        @Override
        public String toString() {
            return "Error{message='" + message + "', cause=" + cause + '}';
        }
    }
}
