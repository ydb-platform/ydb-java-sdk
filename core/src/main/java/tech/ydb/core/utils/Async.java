package tech.ydb.core.utils;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.TimeUnit;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

import io.netty.util.HashedWheelTimer;
import io.netty.util.Timeout;
import io.netty.util.Timer;
import io.netty.util.TimerTask;
import io.netty.util.internal.SystemPropertyUtil;


/**
 * @author Sergey Polovko
 */
public class Async {
    protected Async() {
    }

    private static final boolean DEFAULT_TIMER_THREAD_DAEMON =
        SystemPropertyUtil.getBoolean("tech.ydb.table.async.daemon", true);

    private static final Timer DEFAULT_TIMER = new HashedWheelTimer(
        r -> {
            Thread t = new Thread(r);
            t.setDaemon(DEFAULT_TIMER_THREAD_DAEMON);
            t.setName("YdbAsyncTimer");
            return t;
        },
        100, TimeUnit.MILLISECONDS);


    public static <T> CompletableFuture<T> failedFuture(Throwable t) {
        CompletableFuture<T> f = new CompletableFuture<>();
        f.completeExceptionally(t);
        return f;
    }

    public static <R> CompletableFuture<R> safeCall(Supplier<CompletableFuture<R>> fn) {
        try {
            return fn.get();
        } catch (Throwable ex) {
            return failedFuture(ex);
        }
    }

    public static <T, R> CompletableFuture<R> safeCall(T t, Function<T, CompletableFuture<R>> fn) {
        try {
            return fn.apply(t);
        } catch (Throwable ex) {
            return failedFuture(ex);
        }
    }

    public static <T, U, R> CompletableFuture<R> safeCall(T t, U u, BiFunction<T, U, CompletableFuture<R>> fn) {
        try {
            return fn.apply(t, u);
        } catch (Throwable ex) {
            return failedFuture(ex);
        }
    }

    public static Timeout runAfter(TimerTask task, long delay, TimeUnit unit) {
        return DEFAULT_TIMER.newTimeout(task, delay, unit);
    }

    public static Throwable unwrapCompletionException(Throwable throwable) {
        Throwable cause = throwable;
        while (cause instanceof CompletionException && cause.getCause() != null) {
            cause = cause.getCause();
        }
        return cause;
    }
}
