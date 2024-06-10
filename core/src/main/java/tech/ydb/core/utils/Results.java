package tech.ydb.core.utils;

import javax.annotation.Nonnull;

import tech.ydb.core.Result;
import tech.ydb.core.UnexpectedResultException;

/**
 *
 * @author Aleksandr Gorshenin
 */
public class Results {
    private Results() { }

    @Nonnull
    public static <T> T getValueOrThrow(Result<T> result, String template)
            throws UnexpectedResultException {
        if (!result.isSuccess()) {
            result.getStatus().expectSuccess(template);
        }
        return result.getValue();
    }

    @Nonnull
    public static <T> T getValueOrThrow(Result<T> result, String template, Object... args)
            throws UnexpectedResultException {
        if (!result.isSuccess()) {
            result.getStatus().expectSuccess(String.format(template, args));
        }
        return result.getValue();
    }

    @Nonnull
    public static <T> T getValueOrThrow(Result<T> result, String template, int a1)
            throws UnexpectedResultException {
        if (!result.isSuccess()) {
            result.getStatus().expectSuccess(String.format(template, a1));
        }
        return result.getValue();
    }

    @Nonnull
    public static <T> T getValueOrThrow(Result<T> result, String template, long a1)
            throws UnexpectedResultException {
        if (!result.isSuccess()) {
            result.getStatus().expectSuccess(String.format(template, a1));
        }
        return result.getValue();
    }

    @Nonnull
    public static <T> T getValueOrThrow(Result<T> result, String template, boolean a1)
            throws UnexpectedResultException {
        if (!result.isSuccess()) {
            result.getStatus().expectSuccess(String.format(template, a1));
        }
        return result.getValue();
    }

    @Nonnull
    public static <T> T getValueOrThrow(Result<T> result, String template, Object a1)
            throws UnexpectedResultException {
        if (!result.isSuccess()) {
            result.getStatus().expectSuccess(String.format(template, a1));
        }
        return result.getValue();
    }

    @Nonnull
    public static <T> T getValueOrThrow(Result<T> result, String template, int a1, int a2)
            throws UnexpectedResultException {
        if (!result.isSuccess()) {
            result.getStatus().expectSuccess(String.format(template, a1, a2));
        }
        return result.getValue();
    }

    @Nonnull
    public static <T> T getValueOrThrow(Result<T> result, String template, long a1, int a2)
            throws UnexpectedResultException {
        if (!result.isSuccess()) {
            result.getStatus().expectSuccess(String.format(template, a1, a2));
        }
        return result.getValue();
    }

    @Nonnull
    public static <T> T getValueOrThrow(Result<T> result, String template, boolean a1, int a2)
            throws UnexpectedResultException {
        if (!result.isSuccess()) {
            result.getStatus().expectSuccess(String.format(template, a1, a2));
        }
        return result.getValue();
    }

    @Nonnull
    public static <T> T getValueOrThrow(Result<T> result, String template, Object a1, int a2)
            throws UnexpectedResultException {
        if (!result.isSuccess()) {
            result.getStatus().expectSuccess(String.format(template, a1, a2));
        }
        return result.getValue();
    }

    @Nonnull
    public static <T> T getValueOrThrow(Result<T> result, String template, int a1, long a2)
            throws UnexpectedResultException {
        if (!result.isSuccess()) {
            result.getStatus().expectSuccess(String.format(template, a1, a2));
        }
        return result.getValue();
    }

    @Nonnull
    public static <T> T getValueOrThrow(Result<T> result, String template, long a1, long a2)
            throws UnexpectedResultException {
        if (!result.isSuccess()) {
            result.getStatus().expectSuccess(String.format(template, a1, a2));
        }
        return result.getValue();
    }

    @Nonnull
    public static <T> T getValueOrThrow(Result<T> result, String template, boolean a1, long a2)
            throws UnexpectedResultException {
        if (!result.isSuccess()) {
            result.getStatus().expectSuccess(String.format(template, a1, a2));
        }
        return result.getValue();
    }

    @Nonnull
    public static <T> T getValueOrThrow(Result<T> result, String template, Object a1, long a2)
            throws UnexpectedResultException {
        if (!result.isSuccess()) {
            result.getStatus().expectSuccess(String.format(template, a1, a2));
        }
        return result.getValue();
    }

    @Nonnull
    public static <T> T getValueOrThrow(Result<T> result, String template, int a1, boolean a2)
            throws UnexpectedResultException {
        if (!result.isSuccess()) {
            result.getStatus().expectSuccess(String.format(template, a1, a2));
        }
        return result.getValue();
    }

    @Nonnull
    public static <T> T getValueOrThrow(Result<T> result, String template, long a1, boolean a2)
            throws UnexpectedResultException {
        if (!result.isSuccess()) {
            result.getStatus().expectSuccess(String.format(template, a1, a2));
        }
        return result.getValue();
    }

    @Nonnull
    public static <T> T getValueOrThrow(Result<T> result, String template, boolean a1, boolean a2)
            throws UnexpectedResultException {
        if (!result.isSuccess()) {
            result.getStatus().expectSuccess(String.format(template, a1, a2));
        }
        return result.getValue();
    }

    @Nonnull
    public static <T> T getValueOrThrow(Result<T> result, String template, Object a1, boolean a2)
            throws UnexpectedResultException {
        if (!result.isSuccess()) {
            result.getStatus().expectSuccess(String.format(template, a1, a2));
        }
        return result.getValue();
    }

    @Nonnull
    public static <T> T getValueOrThrow(Result<T> result, String template, int a1, Object a2)
            throws UnexpectedResultException {
        if (!result.isSuccess()) {
            result.getStatus().expectSuccess(String.format(template, a1, a2));
        }
        return result.getValue();
    }

    @Nonnull
    public static <T> T getValueOrThrow(Result<T> result, String template, long a1, Object a2)
            throws UnexpectedResultException {
        if (!result.isSuccess()) {
            result.getStatus().expectSuccess(String.format(template, a1, a2));
        }
        return result.getValue();
    }

    @Nonnull
    public static <T> T getValueOrThrow(Result<T> result, String template, boolean a1, Object a2)
            throws UnexpectedResultException {
        if (!result.isSuccess()) {
            result.getStatus().expectSuccess(String.format(template, a1, a2));
        }
        return result.getValue();
    }

    @Nonnull
    public static <T> T getValueOrThrow(Result<T> result, String template, Object a1, Object a2)
            throws UnexpectedResultException {
        if (!result.isSuccess()) {
            result.getStatus().expectSuccess(String.format(template, a1, a2));
        }
        return result.getValue();
    }
}
