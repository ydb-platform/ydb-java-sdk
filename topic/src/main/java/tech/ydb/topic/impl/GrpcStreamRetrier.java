package tech.ydb.topic.impl;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

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
    protected final AtomicBoolean isReconnecting = new AtomicBoolean(false);
    protected final AtomicBoolean isStopped = new AtomicBoolean(false);
    private final ScheduledExecutorService scheduler;
    protected final AtomicInteger reconnectCounter = new AtomicInteger(0);

    protected GrpcStreamRetrier(ScheduledExecutorService scheduler) {
        this.scheduler = scheduler;
    }

    protected abstract Logger getLogger();
    protected abstract String getStreamName();
    protected abstract void onStreamReconnect();
    protected abstract void onStreamFinished();
    protected abstract void onShutdown(String reason);

    private void tryScheduleReconnect() {
        int currentReconnectCounter = reconnectCounter.get() + 1;
        if (MAX_RECONNECT_COUNT > 0 && currentReconnectCounter > MAX_RECONNECT_COUNT) {
            if (isStopped.compareAndSet(false, true)) {
                String errorMessage = "Maximum retry count (" + MAX_RECONNECT_COUNT + ") exceeded. Shutting down "
                        + getStreamName();
                getLogger().error(errorMessage);
                shutdownImpl(errorMessage);
                return;
            } else {
                getLogger().debug("Maximum retry count ({}}) exceeded. But {} is already shut down.",
                        MAX_RECONNECT_COUNT, getStreamName());
            }
        }
        if (isReconnecting.compareAndSet(false, true)) {
            reconnectCounter.set(currentReconnectCounter);
            int delayMs = currentReconnectCounter <= EXP_BACKOFF_MAX_POWER
                    ? EXP_BACKOFF_BASE_MS * (1 << currentReconnectCounter)
                    : EXP_BACKOFF_CEILING_MS;
            // Add jitter
            delayMs = delayMs + ThreadLocalRandom.current().nextInt(delayMs);
            getLogger().warn("Retry #{}. Scheduling {} reconnect in {}ms...", currentReconnectCounter, getStreamName(),
                    delayMs);
            try {
                scheduler.schedule(this::reconnect, delayMs, TimeUnit.MILLISECONDS);
            } catch (RejectedExecutionException exception) {
                String errorMessage = "Couldn't schedule reconnect: scheduler is already shut down. Shutting down "
                        + getStreamName();
                getLogger().error(errorMessage);
                shutdownImpl(errorMessage);
            }
        } else {
            getLogger().info("should reconnect {} stream, but reconnect is already in progress", getStreamName());
        }
    }

    void reconnect() {
        getLogger().info("{} reconnect #{} started", reconnectCounter.get(), getStreamName());
        if (!isReconnecting.compareAndSet(true, false)) {
            getLogger().warn("Couldn't reset reconnect flag. Shouldn't happen");
        }
        onStreamReconnect();
    }

    protected CompletableFuture<Void> shutdownImpl() {
        return shutdownImpl("");
    }

    protected CompletableFuture<Void> shutdownImpl(String reason) {
        getLogger().info("Shutting down {}" + (reason == null || reason.isEmpty() ? "" : " with reason: " + reason),
                getStreamName());
        isStopped.set(true);
        return CompletableFuture.runAsync(() -> {
            onShutdown(reason);
        });
    }

    protected void completeSession(Status status, Throwable th) {
        getLogger().info("CompleteSession called");
        onStreamFinished();

        if (th != null) {
            getLogger().error("Exception in {} stream session: ", getStreamName(), th);
        } else {
            if (status.isSuccess()) {
                if (isStopped.get()) {
                    getLogger().info("{} stream session closed successfully", getStreamName());
                    return;
                } else {
                    getLogger().warn("{} stream session was closed unexpectedly", getStreamName());
                }
            } else {
                getLogger().warn("Error in {} stream session: {}", getStreamName(), status);
            }
        }

        if (!isStopped.get()) {
            tryScheduleReconnect();
        } else  {
            getLogger().info(" {} is already stopped, no need to schedule reconnect", getStreamName());
        }
    }
}
