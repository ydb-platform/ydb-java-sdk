package tech.ydb.topic.impl;

import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;

import tech.ydb.common.retry.RetryConfig;
import tech.ydb.common.retry.RetryPolicy;
import tech.ydb.core.Status;

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
    private final RetryConfig retryConfig;
    private volatile int retryCount;
    private volatile long retryStartedAt;

    protected GrpcStreamRetrier(RetryConfig retryConfig, ScheduledExecutorService scheduler) {
        this.retryConfig = retryConfig;
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

    private void tryReconnect(long delay) {
        if (!isReconnecting.compareAndSet(false, true)) {
            getLogger().info("[{}] should reconnect {} stream, but reconnect is already in progress", id,
                    getStreamName());
            return;
        }

        getLogger().warn("[{}] Retry #{}. Scheduling {} reconnect in {}ms...", id, retryCount, getStreamName(), delay);
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
        retryStartedAt = -1;
        retryCount = 0;
    }

    void reconnect() {
        getLogger().info("[{}] {} reconnect #{} started", id, getStreamName(), retryCount);
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

        RetryPolicy retryPolicy = null;
        if (th != null) {
            getLogger().error("[{}] Exception in {} stream session: ", id, getStreamName(), th);
            retryPolicy = retryConfig.isThrowableRetryable(th);
        } else {
            if (status.isSuccess()) {
                if (isStopped.get()) {
                    getLogger().info("[{}] {} stream session closed successfully", id, getStreamName());
                    return;
                } else {
                    getLogger().warn("[{}] {} stream session was closed on working {}", id, getStreamName());
                }
            } else {
                getLogger().warn("[{}] Error in {} stream session: {}", id, getStreamName(), status);
                retryPolicy = retryConfig.isStatusRetryable(status.getCode());
            }
        }

        if (isStopped.get()) {
            getLogger().info("[{}] {} is already stopped, no need to schedule reconnect", id, getStreamName());
            return;
        }

        if (retryPolicy != null) {
            if (retryCount < 1) {
                retryStartedAt = System.currentTimeMillis();
            }
            long delay = retryPolicy.nextRetryMs(retryCount + 1, System.currentTimeMillis() - retryStartedAt);
            if (delay >= 0) {
                retryCount++;
                tryReconnect(delay);
                return;
            }
        }

        long elapsedMs = retryStartedAt > 0 ? System.currentTimeMillis() - retryStartedAt : 0;
        if (!isStopped.compareAndSet(false, true)) {
            getLogger().warn("[{}] Stopped after {} retries and {} ms elapsed. But {} is already shut down.",
                    id, retryCount, elapsedMs, getStreamName());
            return;
        }

        String errorMessage = "[" + id + "] Stopped after " + retryCount + " retries and " + elapsedMs +
                " ms elapsed. Shutting down " + getStreamName();
        getLogger().error(errorMessage);
        shutdownImpl(errorMessage);
    }


}
