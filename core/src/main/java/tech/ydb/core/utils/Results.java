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

    /**
     * Extract a value of the result or throws an exception if the result is unsuccessful.
     *
     * @param <T> type of the value
     * @param result a result
     * @param template the exception message to use if the result is unsuccessful
     * @return value of the result
     * @throws UnexpectedResultException if {@code result} is unsuccessful
     */
    @Nonnull
    public static <T> T getValueOrThrow(Result<T> result, String template)
            throws UnexpectedResultException {
        if (!result.isSuccess()) {
            result.getStatus().expectSuccess(template);
        }
        return result.getValue();
    }

    /**
     * Extract a value of the result or throws an exception if the result is unsuccessful.
     *
     * @param <T> type of the value
     * @param result a result
     * @param template a template for the exception message to use if the result is unsuccessful;
     * will be converted to a string using {@link String#format(java.lang.String, java.lang.Object...)}
     * @param args the arguments to be substituted into the message template.
     * @return value of the successful result
     * @throws UnexpectedResultException if {@code result} is unsuccessful
     */
    @Nonnull
    public static <T> T getValueOrThrow(Result<T> result, String template, Object... args)
            throws UnexpectedResultException {
        if (!result.isSuccess()) {
            result.getStatus().expectSuccess(
                    String.format(template, args)
            );
        }
        return result.getValue();
    }

    /**
     * Extract a value of the result or throws an exception if the result is unsuccessful.
     *
     * @param <T> type of the value
     * @param result a result
     * @param template a template for the exception message to use if the result is unsuccessful;
     * will be converted to a string using {@link String#format(java.lang.String, java.lang.Object...)}
     * @param a1 the argument to be substituted into the message template.
     * @return value of the successful result
     * @throws UnexpectedResultException if {@code result} is unsuccessful
     */
    @Nonnull
    public static <T> T getValueOrThrow(Result<T> result, String template, int a1)
            throws UnexpectedResultException {
        if (!result.isSuccess()) {
            result.getStatus().expectSuccess(
                    String.format(template, a1)
            );
        }
        return result.getValue();
    }

    /**
     * Extract a value of the result or throws an exception if the result is unsuccessful.
     *
     * @param <T> type of the value
     * @param result a result
     * @param template a template for the exception message to use if the result is unsuccessful;
     * will be converted to a string using {@link String#format(java.lang.String, java.lang.Object...)}
     * @param a1 the argument to be substituted into the message template.
     * @return value of the successful result
     * @throws UnexpectedResultException if {@code result} is unsuccessful
     */
    @Nonnull
    public static <T> T getValueOrThrow(Result<T> result, String template, long a1)
            throws UnexpectedResultException {
        if (!result.isSuccess()) {
            result.getStatus().expectSuccess(
                    String.format(template, a1)
            );
        }
        return result.getValue();
    }

    /**
     * Extract a value of the result or throws an exception if the result is unsuccessful.
     *
     * @param <T> type of the value
     * @param result a result
     * @param template a template for the exception message to use if the result is unsuccessful;
     * will be converted to a string using {@link String#format(java.lang.String, java.lang.Object...)}
     * @param a1 the argument to be substituted into the message template.
     * @return value of the successful result
     * @throws UnexpectedResultException if {@code result} is unsuccessful
     */
    @Nonnull
    public static <T> T getValueOrThrow(Result<T> result, String template, boolean a1)
            throws UnexpectedResultException {
        if (!result.isSuccess()) {
            result.getStatus().expectSuccess(
                    String.format(template, a1)
            );
        }
        return result.getValue();
    }

    /**
     * Extract a value of the result or throws an exception if the result is unsuccessful.
     *
     * @param <T> type of the value
     * @param result a result
     * @param template a template for the exception message to use if the result is unsuccessful;
     * will be converted to a string using {@link String#format(java.lang.String, java.lang.Object...)}
     * @param a1 the argument to be substituted into the message template.
     * @return value of the successful result
     * @throws UnexpectedResultException if {@code result} is unsuccessful
     */
    @Nonnull
    public static <T> T getValueOrThrow(Result<T> result, String template, Object a1)
            throws UnexpectedResultException {
        if (!result.isSuccess()) {
            result.getStatus().expectSuccess(
                    String.format(template, a1)
            );
        }
        return result.getValue();
    }

    /**
     * Extract a value of the result or throws an exception if the result is unsuccessful.
     *
     * @param <T> type of the value
     * @param result a result
     * @param template a template for the exception message to use if the result is unsuccessful;
     * will be converted to a string using {@link String#format(java.lang.String, java.lang.Object...)}
     * @param a1 the argument to be substituted into the message template.
     * @param a2 the argument to be substituted into the message template.
     * @return value of the successful result
     * @throws UnexpectedResultException if {@code result} is unsuccessful
     */
    @Nonnull
    public static <T> T getValueOrThrow(Result<T> result, String template, int a1, int a2)
            throws UnexpectedResultException {
        if (!result.isSuccess()) {
            result.getStatus().expectSuccess(
                    String.format(template, a1, a2)
            );
        }
        return result.getValue();
    }

    /**
     * Extract a value of the result or throws an exception if the result is unsuccessful.
     *
     * @param <T> type of the value
     * @param result a result
     * @param template a template for the exception message to use if the result is unsuccessful;
     * will be converted to a string using {@link String#format(java.lang.String, java.lang.Object...)}
     * @param a1 the argument to be substituted into the message template.
     * @param a2 the argument to be substituted into the message template.
     * @return value of the successful result
     * @throws UnexpectedResultException if {@code result} is unsuccessful
     */
    @Nonnull
    public static <T> T getValueOrThrow(Result<T> result, String template, long a1, int a2)
            throws UnexpectedResultException {
        if (!result.isSuccess()) {
            result.getStatus().expectSuccess(
                    String.format(template, a1, a2)
            );
        }
        return result.getValue();
    }

    /**
     * Extract a value of the result or throws an exception if the result is unsuccessful.
     *
     * @param <T> type of the value
     * @param result a result
     * @param template a template for the exception message to use if the result is unsuccessful;
     * will be converted to a string using {@link String#format(java.lang.String, java.lang.Object...)}
     * @param a1 the argument to be substituted into the message template.
     * @param a2 the argument to be substituted into the message template.
     * @return value of the successful result
     * @throws UnexpectedResultException if {@code result} is unsuccessful
     */
    @Nonnull
    public static <T> T getValueOrThrow(Result<T> result, String template, boolean a1, int a2)
            throws UnexpectedResultException {
        if (!result.isSuccess()) {
            result.getStatus().expectSuccess(
                    String.format(template, a1, a2)
            );
        }
        return result.getValue();
    }

    /**
     * Extract a value of the result or throws an exception if the result is unsuccessful.
     *
     * @param <T> type of the value
     * @param result a result
     * @param template a template for the exception message to use if the result is unsuccessful;
     * will be converted to a string using {@link String#format(java.lang.String, java.lang.Object...)}
     * @param a1 the argument to be substituted into the message template.
     * @param a2 the argument to be substituted into the message template.
     * @return value of the successful result
     * @throws UnexpectedResultException if {@code result} is unsuccessful
     */
    @Nonnull
    public static <T> T getValueOrThrow(Result<T> result, String template, Object a1, int a2)
            throws UnexpectedResultException {
        if (!result.isSuccess()) {
            result.getStatus().expectSuccess(
                    String.format(template, a1, a2)
            );
        }
        return result.getValue();
    }

    /**
     * Extract a value of the result or throws an exception if the result is unsuccessful.
     *
     * @param <T> type of the value
     * @param result a result
     * @param template a template for the exception message to use if the result is unsuccessful;
     * will be converted to a string using {@link String#format(java.lang.String, java.lang.Object...)}
     * @param a1 the argument to be substituted into the message template.
     * @param a2 the argument to be substituted into the message template.
     * @return value of the successful result
     * @throws UnexpectedResultException if {@code result} is unsuccessful
     */
    @Nonnull
    public static <T> T getValueOrThrow(Result<T> result, String template, int a1, long a2)
            throws UnexpectedResultException {
        if (!result.isSuccess()) {
            result.getStatus().expectSuccess(
                    String.format(template, a1, a2)
            );
        }
        return result.getValue();
    }

    /**
     * Extract a value of the result or throws an exception if the result is unsuccessful.
     *
     * @param <T> type of the value
     * @param result a result
     * @param template a template for the exception message to use if the result is unsuccessful;
     * will be converted to a string using {@link String#format(java.lang.String, java.lang.Object...)}
     * @param a1 the argument to be substituted into the message template.
     * @param a2 the argument to be substituted into the message template.
     * @return value of the successful result
     * @throws UnexpectedResultException if {@code result} is unsuccessful
     */
    @Nonnull
    public static <T> T getValueOrThrow(Result<T> result, String template, long a1, long a2)
            throws UnexpectedResultException {
        if (!result.isSuccess()) {
            result.getStatus().expectSuccess(
                    String.format(template, a1, a2)
            );
        }
        return result.getValue();
    }

    /**
     * Extract a value of the result or throws an exception if the result is unsuccessful.
     *
     * @param <T> type of the value
     * @param result a result
     * @param template a template for the exception message to use if the result is unsuccessful;
     * will be converted to a string using {@link String#format(java.lang.String, java.lang.Object...)}
     * @param a1 the argument to be substituted into the message template.
     * @param a2 the argument to be substituted into the message template.
     * @return value of the successful result
     * @throws UnexpectedResultException if {@code result} is unsuccessful
     */
    @Nonnull
    public static <T> T getValueOrThrow(Result<T> result, String template, boolean a1, long a2)
            throws UnexpectedResultException {
        if (!result.isSuccess()) {
            result.getStatus().expectSuccess(
                    String.format(template, a1, a2)
            );
        }
        return result.getValue();
    }

    /**
     * Extract a value of the result or throws an exception if the result is unsuccessful.
     *
     * @param <T> type of the value
     * @param result a result
     * @param template a template for the exception message to use if the result is unsuccessful;
     * will be converted to a string using {@link String#format(java.lang.String, java.lang.Object...)}
     * @param a1 the argument to be substituted into the message template.
     * @param a2 the argument to be substituted into the message template.
     * @return value of the successful result
     * @throws UnexpectedResultException if {@code result} is unsuccessful
     */
    @Nonnull
    public static <T> T getValueOrThrow(Result<T> result, String template, Object a1, long a2)
            throws UnexpectedResultException {
        if (!result.isSuccess()) {
            result.getStatus().expectSuccess(
                    String.format(template, a1, a2)
            );
        }
        return result.getValue();
    }

    /**
     * Extract a value of the result or throws an exception if the result is unsuccessful.
     *
     * @param <T> type of the value
     * @param result a result
     * @param template a template for the exception message to use if the result is unsuccessful;
     * will be converted to a string using {@link String#format(java.lang.String, java.lang.Object...)}
     * @param a1 the argument to be substituted into the message template.
     * @param a2 the argument to be substituted into the message template.
     * @return value of the successful result
     * @throws UnexpectedResultException if {@code result} is unsuccessful
     */
    @Nonnull
    public static <T> T getValueOrThrow(Result<T> result, String template, int a1, boolean a2)
            throws UnexpectedResultException {
        if (!result.isSuccess()) {
            result.getStatus().expectSuccess(
                    String.format(template, a1, a2)
            );
        }
        return result.getValue();
    }

    /**
     * Extract a value of the result or throws an exception if the result is unsuccessful.
     *
     * @param <T> type of the value
     * @param result a result
     * @param template a template for the exception message to use if the result is unsuccessful;
     * will be converted to a string using {@link String#format(java.lang.String, java.lang.Object...)}
     * @param a1 the argument to be substituted into the message template.
     * @param a2 the argument to be substituted into the message template.
     * @return value of the successful result
     * @throws UnexpectedResultException if {@code result} is unsuccessful
     */
    @Nonnull
    public static <T> T getValueOrThrow(Result<T> result, String template, long a1, boolean a2)
            throws UnexpectedResultException {
        if (!result.isSuccess()) {
            result.getStatus().expectSuccess(
                    String.format(template, a1, a2)
            );
        }
        return result.getValue();
    }

    /**
     * Extract a value of the result or throws an exception if the result is unsuccessful.
     *
     * @param <T> type of the value
     * @param result a result
     * @param template a template for the exception message to use if the result is unsuccessful;
     * will be converted to a string using {@link String#format(java.lang.String, java.lang.Object...)}
     * @param a1 the argument to be substituted into the message template.
     * @param a2 the argument to be substituted into the message template.
     * @return value of the successful result
     * @throws UnexpectedResultException if {@code result} is unsuccessful
     */
    @Nonnull
    public static <T> T getValueOrThrow(Result<T> result, String template, boolean a1, boolean a2)
            throws UnexpectedResultException {
        if (!result.isSuccess()) {
            result.getStatus().expectSuccess(
                    String.format(template, a1, a2)
            );
        }
        return result.getValue();
    }

    /**
     * Extract a value of the result or throws an exception if the result is unsuccessful.
     *
     * @param <T> type of the value
     * @param result a result
     * @param template a template for the exception message to use if the result is unsuccessful;
     * will be converted to a string using {@link String#format(java.lang.String, java.lang.Object...)}
     * @param a1 the argument to be substituted into the message template.
     * @param a2 the argument to be substituted into the message template.
     * @return value of the successful result
     * @throws UnexpectedResultException if {@code result} is unsuccessful
     */
    @Nonnull
    public static <T> T getValueOrThrow(Result<T> result, String template, Object a1, boolean a2)
            throws UnexpectedResultException {
        if (!result.isSuccess()) {
            result.getStatus().expectSuccess(
                    String.format(template, a1, a2)
            );
        }
        return result.getValue();
    }

    /**
     * Extract a value of the result or throws an exception if the result is unsuccessful.
     *
     * @param <T> type of the value
     * @param result a result
     * @param template a template for the exception message to use if the result is unsuccessful;
     * will be converted to a string using {@link String#format(java.lang.String, java.lang.Object...)}
     * @param a1 the argument to be substituted into the message template.
     * @param a2 the argument to be substituted into the message template.
     * @return value of the successful result
     * @throws UnexpectedResultException if {@code result} is unsuccessful
     */
    @Nonnull
    public static <T> T getValueOrThrow(Result<T> result, String template, int a1, Object a2)
            throws UnexpectedResultException {
        if (!result.isSuccess()) {
            result.getStatus().expectSuccess(
                    String.format(template, a1, a2)
            );
        }
        return result.getValue();
    }

    /**
     * Extract a value of the result or throws an exception if the result is unsuccessful.
     *
     * @param <T> type of the value
     * @param result a result
     * @param template a template for the exception message to use if the result is unsuccessful;
     * will be converted to a string using {@link String#format(java.lang.String, java.lang.Object...)}
     * @param a1 the argument to be substituted into the message template.
     * @param a2 the argument to be substituted into the message template.
     * @return value of the successful result
     * @throws UnexpectedResultException if {@code result} is unsuccessful
     */
    @Nonnull
    public static <T> T getValueOrThrow(Result<T> result, String template, long a1, Object a2)
            throws UnexpectedResultException {
        if (!result.isSuccess()) {
            result.getStatus().expectSuccess(
                    String.format(template, a1, a2)
            );
        }
        return result.getValue();
    }

    /**
     * Extract a value of the result or throws an exception if the result is unsuccessful.
     *
     * @param <T> type of the value
     * @param result a result
     * @param template a template for the exception message to use if the result is unsuccessful;
     * will be converted to a string using {@link String#format(java.lang.String, java.lang.Object...)}
     * @param a1 the argument to be substituted into the message template.
     * @param a2 the argument to be substituted into the message template.
     * @return value of the successful result
     * @throws UnexpectedResultException if {@code result} is unsuccessful
     */
    @Nonnull
    public static <T> T getValueOrThrow(Result<T> result, String template, boolean a1, Object a2)
            throws UnexpectedResultException {
        if (!result.isSuccess()) {
            result.getStatus().expectSuccess(
                    String.format(template, a1, a2)
            );
        }
        return result.getValue();
    }

    /**
     * Extract a value of the result or throws an exception if the result is unsuccessful.
     * @param <T> type of the value
     * @param result a result
     * @param template a template for the exception message to use if the result is unsuccessful;
     * will be converted to a string using {@link String#format(java.lang.String, java.lang.Object...)}
     * @param a1 the argument to be substituted into the message template.
     * @param a2 the argument to be substituted into the message template.
     * @return value of the successful result
     * @throws UnexpectedResultException if {@code result} is unsuccessful
     */
    @Nonnull
    public static <T> T getValueOrThrow(Result<T> result, String template, Object a1, Object a2)
            throws UnexpectedResultException {
        if (!result.isSuccess()) {
            result.getStatus().expectSuccess(
                    String.format(template, a1, a2)
            );
        }
        return result.getValue();
    }
}
