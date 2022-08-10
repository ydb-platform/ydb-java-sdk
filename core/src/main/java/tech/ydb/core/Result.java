package tech.ydb.core;

import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

import javax.annotation.Nullable;

import com.google.common.base.Strings;


/**
 * @author Sergey Polovko
 */
public abstract class Result<T> {

    public static <V> Result<V> success(V value) {
        return new Success<>(value, Issue.EMPTY_ARRAY, null);
    }

    public static <V> Result<V> success(V value, Double consumedRu, Issue... issues) {
        return new Success<>(value, issues, consumedRu);
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

    public static <V> Result<V> fail(UnexpectedResultException unexpected) {
        return new Fail<>(unexpected.getStatusCode(), unexpected.getIssues());
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

    public boolean hasCostInfo() {
        return getConsumedRu() != null;
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

    public abstract Double getConsumedRu();

    public abstract T expect(String message);

    public abstract Optional<T> ok();

    public abstract Optional<Throwable> error();

    public abstract <U> Result<U> map(Function<T, U> mapper);

    /**
     * SUCCESS
     */
    private static final class Success<V> extends Result<V> {
        @Nullable
        private final V value;
        private final Issue[] issues;
        private final Double consumedRu;

        Success(V value, Issue[] issues, Double consumedRu) {
            this.value = value;
            this.issues = issues;
            this.consumedRu = consumedRu;
        }

        @Override
        public StatusCode getCode() {
            return StatusCode.SUCCESS;
        }

        @Override
        public Issue[] getIssues() {
            return issues;
        }

        @Override
        public boolean hasCostInfo() {
            return consumedRu != null;
        }

        @Override
        public Double getConsumedRu() {
            return consumedRu;
        }

        @Override
        public V expect(String message) {
            return value;
        }

        @Override
        public Optional<V> ok() {
            return Optional.ofNullable(value);
        }

        @Override
        public Optional<Throwable> error() {
            return Optional.empty();
        }

        @Override
        public <U> Result<U> map(Function<V, U> mapper) {
            return new Success<>(mapper.apply(value), issues, consumedRu);
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
            if (!Objects.equals(value, success.value)) {
                return false;
            }
            return Arrays.equals(issues, success.issues);
        }

        @Override
        public int hashCode() {
            int result = value != null ? value.hashCode() : 1337;
            result = 31 * result + Arrays.hashCode(issues);
            return result;
        }

        @Override
        public String toString() {
            return "Success{" + value + ", issues=" + Arrays.toString(issues) + '}';
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
            this.code = Objects.requireNonNull(code, "code");
            this.issues = Objects.requireNonNull(issues, "issues");
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
        public Double getConsumedRu() {
            return null;
        }

        @Override
        public V expect(String message) {
            throw newException(message);
        }

        @Override
        public Optional<V> ok() {
            return Optional.empty();
        }

        @Override
        public Optional<Throwable> error() {
            return Optional.of(newException("error result"));
        }

        @SuppressWarnings("unchecked")
        @Override
        public <U> Result<U> map(Function<V, U> mapper) {
            return (Result<U>) this;
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
            if (code != fail.code) {
                return false;
            }
            return Arrays.equals(issues, fail.issues);
        }

        @Override
        public int hashCode() {
            int result = code.hashCode();
            result = 31 * result + Arrays.hashCode(issues);
            return result;
        }

        @Override
        public String toString() {
            return "Fail{code=" + code + ", issues=" + Arrays.toString(issues) + '}';
        }

        private UnexpectedResultException newException(String message) {
            return new UnexpectedResultException(message, code, issues);
        }
    }

    /**
     * ERROR
     */
    private static final class Error<V> extends Result<V> {
        private final String message;
        @Nullable
        private final Throwable cause;

        Error(String message, Throwable cause) {
            this.message = Strings.nullToEmpty(message);
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
        public Double getConsumedRu() {
            return null;
        }

        @Override
        public V expect(String message) {
            if (!this.message.isEmpty()) {
                message += ": " + this.message;
            }
            throw new UnexpectedResultException(message, getCode(), cause);
        }

        @Override
        public Optional<V> ok() {
            return Optional.empty();
        }

        @Override
        public Optional<Throwable> error() {
            return Optional.of(new UnexpectedResultException(message, getCode(), cause));
        }

        @SuppressWarnings("unchecked")
        @Override
        public <U> Result<U> map(Function<V, U> mapper) {
            return (Result<U>) this;
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
            if (!message.equals(error.message)) {
                return false;
            }
            return Objects.equals(cause, error.cause);
        }

        @Override
        public int hashCode() {
            return 31 * message.hashCode() + (cause != null ? cause.hashCode() : 0);
        }

        @Override
        public String toString() {
            return "Error{message=" + message + ", cause=" + cause + '}';
        }
    }
}
