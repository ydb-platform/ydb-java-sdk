package tech.ydb.core.utils;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

/**
 *
 * @author Aleksandr Gorshenin
 */
public class FutureTools {
    private FutureTools() { }

    public static Throwable unwrapCompletionException(Throwable throwable) {
        Throwable cause = throwable;
        while (cause instanceof CompletionException && cause.getCause() != null) {
            cause = cause.getCause();
        }
        return cause;
    }

    public static <T> CompletableFuture<T> failedFuture(Throwable t) {
        CompletableFuture<T> f = new CompletableFuture<>();
        f.completeExceptionally(t);
        return f;
    }
}
