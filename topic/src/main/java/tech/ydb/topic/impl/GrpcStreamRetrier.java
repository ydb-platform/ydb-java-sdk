package tech.ydb.topic.impl;

import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;

import tech.ydb.common.retry.ExponentialBackoffRetry;
import tech.ydb.common.retry.RetryPolicy;
import tech.ydb.core.Status;
import tech.ydb.topic.settings.RetryMode;

/**
 * @author Nikolay Perfilov
 */
public abstract class GrpcStreamRetrier {
    private static final int ID_LENGTH = 6;
    private static final char[] ID_ALPHABET = "abcdefghijklmnopqrstuvwxyzABSDEFGHIJKLMNOPQRSTUVWXYZ1234567890"
            .toCharArray();

    protected final String id;
    protected final AtomicBoolean isReconnecting = new AtomicBoolean(false);
    protected final AtomicBoolean isStopped = new AtomicBoolean(false);

    private final ScheduledExecutorService scheduler;
    private final RetryMode retryMode;
    private final RetryPolicy retryPolicy = new DefaultRetryPolicy();
    private final AtomicInteger retry = new AtomicInteger(-1);

    protected GrpcStreamRetrier(RetryMode retryMode, ScheduledExecutorService scheduler) {
        this.retryMode = retryMode;
        this.scheduler = scheduler;
        this.id = generateRandomId(ID_LENGTH);
    }

    protected abstract Logger getLogger();
    protected abstract String getStreamName();
    protected abstract void onStreamReconnect();
    protected abstract void onShutdown(String reason);

    protected static String generateRandomId(int length) {
        return new Random().ints(0, ID_ALPHABET.length)
                .limit(length)
                .map(charId -> ID_ALPHABET[charId])
                .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
                .toString();
    }

    private void tryScheduleReconnect(int retryNumber) {
        if (!isReconnecting.compareAndSet(false, true)) {
            getLogger().info("[{}] should reconnect {} stream, but reconnect is already in progress", id,
                    getStreamName());
            return;
        }

        retry.set(retryNumber);
        long delay = retryPolicy.nextRetryMs(retryNumber, 0);
        getLogger().warn("[{}] Retry #{}. Scheduling {} reconnect in {}ms...", id, retryNumber, getStreamName(), delay);
        try {
            scheduler.schedule(this::reconnect, delay, TimeUnit.MILLISECONDS);
        } catch (RejectedExecutionException exception) {
            String errorMessage = "[" + id + "] Couldn't schedule reconnect: scheduler is already shut down. " +
                    "Shutting down " + getStreamName();
            getLogger().error(errorMessage);
            shutdownImpl(errorMessage);
        }
    }

    protected void resetRetries() {
        retry.set(0);
    }

    void reconnect() {
        getLogger().info("[{}] {} reconnect #{} started", id, getStreamName(), retry.get());
        if (!isReconnecting.compareAndSet(true, false)) {
            getLogger().warn("Couldn't reset reconnect flag. Shouldn't happen");
        }
        onStreamReconnect();
    }

    protected CompletableFuture<Void> shutdownImpl() {
        return shutdownImpl("");
    }

    protected CompletableFuture<Void> shutdownImpl(String reason) {
        getLogger().info("[{}] Shutting down {}"
                        + (reason == null || reason.isEmpty() ? "" : " with reason: " + reason), id, getStreamName());
        isStopped.set(true);
        return CompletableFuture.runAsync(() -> {
            onShutdown(reason);
        });
    }

    protected void onSessionClosed(Status status, Throwable th) {
        getLogger().info("[{}] onSessionClosed called", id);

        if (th != null) {
            getLogger().error("[{}] Exception in {} stream session: ", id, getStreamName(), th);
        } else {
            if (status.isSuccess()) {
                if (isStopped.get()) {
                    getLogger().info("[{}] {} stream session closed successfully", id, getStreamName());
                    return;
                } else {
                    getLogger().warn("[{}] {} stream session was closed on working {}", id, getStreamName(),
                            getStreamName());
                }
            } else {
                getLogger().warn("[{}] Error in {} stream session: {}", id, getStreamName(), status);
            }
        }

        if (isStopped.get()) {
            getLogger().info("[{}] {} is already stopped, no need to schedule reconnect", id, getStreamName());
            return;
        }

        int currentRetry = nextRetryNumber();
        if (currentRetry > 0) {
            tryScheduleReconnect(currentRetry);
            return;
        }

        if (!isStopped.compareAndSet(false, true)) {
            getLogger().warn("[{}] Stopped by retry mode {} after {} retries. But {} is already shut down.", id,
                    retryMode, currentRetry, getStreamName());
            return;
        }

        String errorMessage = "[" + id + "] Stopped by retry mode " + retryMode + " after " + currentRetry +
                " retries. Shutting down " + getStreamName();
        getLogger().error(errorMessage);
        shutdownImpl(errorMessage);
    }

    private int nextRetryNumber() {
        int next = retry.get() + 1;
        switch (retryMode) {
            case RECOVER: return next;
            case ALWAYS: return Math.max(1, next);
            case NONE:
            default:
                return 0;
        }
    }

    private static class DefaultRetryPolicy extends ExponentialBackoffRetry {

        private static final int EXP_BACKOFF_BASE_MS = 256;
        private static final int EXP_BACKOFF_MAX_POWER = 7;

        DefaultRetryPolicy() {
            super(EXP_BACKOFF_BASE_MS, EXP_BACKOFF_MAX_POWER);
        }

        @Override
        public long nextRetryMs(int retryCount, long elapsedTimeMs /* ignored */) {
            return backoffTimeMillis(retryCount);
        }
    }
}
