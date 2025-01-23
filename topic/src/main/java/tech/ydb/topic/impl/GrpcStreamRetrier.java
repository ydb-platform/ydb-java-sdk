package tech.ydb.topic.impl;

import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

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

    private final Logger logger;
    private final ScheduledExecutorService scheduler;
    private final RetryMode retryMode;
    private final RetryPolicy retryPolicy = new DefaultRetryPolicy();

    private volatile boolean connected = false;
    private volatile int retryNumber = 0;

    protected GrpcStreamRetrier(Logger logger, RetryMode retryMode, ScheduledExecutorService scheduler) {
        this.logger = logger;
        this.retryMode = retryMode;
        this.scheduler = scheduler;
        this.id = generateRandomId(ID_LENGTH);
    }

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

    private void tryScheduleReconnect() {
        if (!isReconnecting.compareAndSet(false, true)) {
            logger.info("[{}] should reconnect {} stream, but reconnect is already in progress", id,
                    getStreamName());
            return;
        }

        long delay = retryPolicy.nextRetryMs(retryNumber, 0);
        logger.warn("[{}] Retry #{}. Scheduling {} reconnect in {}ms...", id, retryNumber, getStreamName(), delay);
        try {
            scheduler.schedule(this::reconnect, delay, TimeUnit.MILLISECONDS);
        } catch (RejectedExecutionException exception) {
            String errorMessage = "[" + id + "] Couldn't schedule reconnect: scheduler is already shut down. " +
                    "Shutting down " + getStreamName();
            logger.error(errorMessage);
            shutdownImpl(errorMessage);
        }
    }

    protected void resetRetries() {
        retryNumber = 0;
        connected = true;
    }

    void reconnect() {
        logger.info("[{}] {} reconnect #{} started", id, getStreamName(), retryNumber);
        if (!isReconnecting.compareAndSet(true, false)) {
            logger.warn("Couldn't reset reconnect flag. Shouldn't happen");
        }
        onStreamReconnect();
    }

    protected CompletableFuture<Void> shutdownImpl() {
        return shutdownImpl("");
    }

    protected CompletableFuture<Void> shutdownImpl(String reason) {
        logger.info("[{}] Shutting down {}"
                        + (reason == null || reason.isEmpty() ? "" : " with reason: " + reason), id, getStreamName());
        isStopped.set(true);
        return CompletableFuture.runAsync(() -> {
            onShutdown(reason);
        });
    }

    protected void onSessionClosed(Status status, Throwable th) {
        logger.info("[{}] onSessionClosed called", id);

        if (th != null) {
            logger.warn("[{}] Exception in {} stream session: ", id, getStreamName(), th);
        } else {
            if (status.isSuccess()) {
                if (isStopped.get()) {
                    logger.info("[{}] {} stream session closed successfully", id, getStreamName());
                    return;
                } else {
                    logger.warn("[{}] {} stream session was closed on working {}", id, getStreamName(),
                            getStreamName());
                }
            } else {
                logger.warn("[{}] Error in {} stream session: {}", id, getStreamName(), status);
            }
        }

        if (isStopped.get()) {
            logger.info("[{}] {} is already stopped, no need to schedule reconnect", id, getStreamName());
            return;
        }

        if (retryMode == RetryMode.ALWAYS || (retryMode == RetryMode.RECOVER && connected)) {
            retryNumber++;
            tryScheduleReconnect();
            return;
        }

        if (!isStopped.compareAndSet(false, true)) {
            logger.warn("[{}] Stopped by retry mode {} after {} retries. But {} is already shut down.", id,
                    retryMode, retryNumber, getStreamName());
            return;
        }

        String errorMessage = "[" + id + "] Stopped by retry mode " + retryMode + " after " + retryNumber +
                " retries. Shutting down " + getStreamName();
        logger.warn(errorMessage);
        shutdownImpl(errorMessage);
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
