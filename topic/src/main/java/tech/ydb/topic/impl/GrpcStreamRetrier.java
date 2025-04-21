package tech.ydb.topic.impl;

import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;

import org.slf4j.Logger;

import tech.ydb.core.Status;

/**
 * @author Nikolay Perfilov
 */
public abstract class GrpcStreamRetrier {
    // TODO: add retry policy
    private static final int MAX_RECONNECT_COUNT = 0; // Inf
    private static final int EXP_BACKOFF_BASE_MS = 256;
    private static final int EXP_BACKOFF_CEILING_MS = 40000; // 40 sec (max delays would be 40-80 sec)
    private static final int EXP_BACKOFF_MAX_POWER = 7;
    private static final int ID_LENGTH = 6;
    private static final char[] ID_ALPHABET = "abcdefghijklmnopqrstuvwxyzABSDEFGHIJKLMNOPQRSTUVWXYZ1234567890"
            .toCharArray();

    protected final String id;
    protected final AtomicBoolean isReconnecting = new AtomicBoolean(false);
    protected final AtomicBoolean isStopped = new AtomicBoolean(false);
    protected final AtomicInteger reconnectCounter = new AtomicInteger(0);

    private final ScheduledExecutorService scheduler;
    private final BiConsumer<Status, Throwable> errorsHandler;

    protected GrpcStreamRetrier(ScheduledExecutorService scheduler, BiConsumer<Status, Throwable> errorsHandler) {
        this.scheduler = scheduler;
        this.id = generateRandomId(ID_LENGTH);
        this.errorsHandler = errorsHandler;
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

    private void tryScheduleReconnect() {
        int currentReconnectCounter = reconnectCounter.get() + 1;
        if (MAX_RECONNECT_COUNT > 0 && currentReconnectCounter > MAX_RECONNECT_COUNT) {
            if (isStopped.compareAndSet(false, true)) {
                String errorMessage = "[" + id + "] Maximum retry count (" + MAX_RECONNECT_COUNT
                        + ") exceeded. Shutting down " + getStreamName();
                getLogger().error(errorMessage);
                shutdownImpl(errorMessage);
                return;
            } else {
                getLogger().info("[{}] Maximum retry count ({}}) exceeded. Need to shutdown {} but it's already " +
                                "shut down.", id, MAX_RECONNECT_COUNT, getStreamName());
            }
        }
        if (isReconnecting.compareAndSet(false, true)) {
            reconnectCounter.set(currentReconnectCounter);
            int delayMs = currentReconnectCounter <= EXP_BACKOFF_MAX_POWER
                    ? EXP_BACKOFF_BASE_MS * (1 << currentReconnectCounter)
                    : EXP_BACKOFF_CEILING_MS;
            // Add jitter
            delayMs = delayMs + ThreadLocalRandom.current().nextInt(delayMs);
            getLogger().warn("[{}] Retry #{}. Scheduling {} reconnect in {}ms...", id, currentReconnectCounter,
                    getStreamName(), delayMs);
            try {
                scheduler.schedule(this::reconnect, delayMs, TimeUnit.MILLISECONDS);
            } catch (RejectedExecutionException exception) {
                String errorMessage = "[" + id + "] Couldn't schedule reconnect: scheduler is already shut down. " +
                        "Shutting down " + getStreamName();
                getLogger().error(errorMessage);
                shutdownImpl(errorMessage);
            }
        } else {
            getLogger().info("[{}] should reconnect {} stream, but reconnect is already in progress", id,
                    getStreamName());
        }
    }

    void reconnect() {
        if (isStopped.get()) {
            getLogger().info("[{}] {} is already stopped, no need to reconnect", id, getStreamName());
            return;
        }

        getLogger().info("[{}] {} reconnect #{} started", id, getStreamName(), reconnectCounter.get());
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

        if (errorsHandler != null) {
            errorsHandler.accept(status, th);
        }

        if (!isStopped.get()) {
            tryScheduleReconnect();
        } else  {
            getLogger().info("[{}] {} is already stopped, no need to schedule reconnect", id, getStreamName());
        }
    }
}
