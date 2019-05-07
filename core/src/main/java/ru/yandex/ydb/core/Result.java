package ru.yandex.ydb.core;

import java.util.Arrays;
import java.util.Optional;
import java.util.function.Function;


/**
 * @author Sergey Polovko
 */
public abstract class Result<T> {

    public static <V> Result<V> success(V value) {
        return new Success<>(value);
    }

    public static <V> Result<V> fail(Status status) {
        return new Fail<>(status.getCode(), status.getIssues());
    }

    public static <V> Result<V> fail(StatusCode code) {
        return new Fail<>(code, Issue.EMPTY_ARRAY);
    }

    public static <V> Result<V> fail(StatusCode code, Issue... issues) {
        return new Fail<>(code, issues);
    }

    public static <V> Result<V> error(Throwable throwable) {
        return new Error<>("", throwable);
    }

    public static <V> Result<V> error(String message, Throwable throwable) {
        return new Error<>(message, throwable);
    }

    public boolean isSuccess() {
        return getCode() == StatusCode.SUCCESS;
    }

    public Status toStatus() {
        return Status.of(getCode(), getIssues());
    }

    public <U> Result<U> cast() {
        @SuppressWarnings("unchecked")
        Result<U> u = (Result<U>) this;
        return u;
    }

    public abstract StatusCode getCode();

    public abstract Issue[] getIssues();

    public abstract T expect(String message);

    public abstract Optional<T> toOptional();

    public abstract <U> Result<U> map(Function<T, U> mapper);

    /**
     * SUCCESS
     */
    private static final class Success<V> extends Result<V> {
        private final V value;

        Success(V value) {
            this.value = value;
        }

        @Override
        public StatusCode getCode() {
            return StatusCode.SUCCESS;
        }

        @Override
        public Issue[] getIssues() {
            return Issue.EMPTY_ARRAY;
        }

        @Override
        public V expect(String message) {
            return value;
        }

        @Override
        public Optional<V> toOptional() {
            return Optional.of(value);
        }

        @Override
        public <U> Result<U> map(Function<V, U> mapper) {
            return new Success<>(mapper.apply(value));
        }

        @Override
        public String toString() {
            return "Success{" + value + '}';
        }
    }

    /**
     * FAIL
     */
    private static final class Fail<V> extends Result<V> {
        private final StatusCode code;
        private final Issue[] issues;

        Fail(StatusCode code, Issue[] issues) {
            assert code != StatusCode.SUCCESS;
            this.code = code;
            this.issues = issues;
        }

        @Override
        public StatusCode getCode() {
            return code;
        }

        @Override
        public Issue[] getIssues() {
            return issues;
        }

        @Override
        public V expect(String message) {
            throw new UnexpectedResultException(message, code, issues);
        }

        @Override
        public Optional<V> toOptional() {
            return Optional.empty();
        }

        @SuppressWarnings("unchecked")
        @Override
        public <U> Result<U> map(Function<V, U> mapper) {
            return (Result<U>) this;
        }

        @Override
        public String toString() {
            return "Fail{code=" + code + ", issues=" + Arrays.toString(issues) + '}';
        }
    }

    /**
     * ERROR
     */
    private static final class Error<V> extends Result<V> {
        private final String message;
        private final Throwable cause;

        Error(String message, Throwable cause) {
            this.message = message;
            this.cause = cause;
        }

        @Override
        public StatusCode getCode() {
            return StatusCode.CLIENT_INTERNAL_ERROR;
        }

        @Override
        public Issue[] getIssues() {
            return Issue.EMPTY_ARRAY;
        }

        @Override
        public V expect(String message) {
            if (this.message.isEmpty()) {
                throw new UnexpectedResultException(message, cause);
            }
            throw new UnexpectedResultException(message + ": " + this.message, cause);
        }

        @Override
        public Optional<V> toOptional() {
            return Optional.empty();
        }

        @SuppressWarnings("unchecked")
        @Override
        public <U> Result<U> map(Function<V, U> mapper) {
            return (Result<U>) this;
        }

        @Override
        public String toString() {
            return "Error{message=" + message + ", cause=" + cause + '}';
        }
    }
}
