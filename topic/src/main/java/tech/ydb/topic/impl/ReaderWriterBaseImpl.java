package tech.ydb.topic.impl;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
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
public abstract class ReaderWriterBaseImpl<SessionType extends Session> {
    // TODO: add retry policy
    private static final int MAX_RECONNECT_COUNT = 0; // Inf
    private static final int EXP_BACKOFF_BASE_MS = 256;
    private static final int EXP_BACKOFF_CEILING_MS = 40000; // 40 sec (max delays would be 40-80 sec)
    private static final int EXP_BACKOFF_MAX_POWER = 7;
    protected final AtomicBoolean isReconnecting = new AtomicBoolean(false);
    protected final AtomicBoolean isStopped = new AtomicBoolean(false);

    protected final String id;
    protected final AtomicInteger reconnectCounter = new AtomicInteger(0);
    protected String currentSessionId;
    protected SessionType session;
    private final ScheduledExecutorService scheduler;

    protected ReaderWriterBaseImpl(ScheduledExecutorService scheduler) {
        this.scheduler = scheduler;
        this.id = UUID.randomUUID().toString();
    }

    protected abstract Logger getLogger();
    protected abstract String getSessionType();
    protected abstract void onSessionStop();
    protected abstract void onReconnect();
    protected abstract void onShutdown(String reason);

    private void tryScheduleReconnect() {
        int currentReconnectCounter = reconnectCounter.get() + 1;
        if (MAX_RECONNECT_COUNT > 0 && currentReconnectCounter > MAX_RECONNECT_COUNT) {
            if (isStopped.compareAndSet(false, true)) {
                getLogger().error("[{}] Maximum retry count ({}}) exceeded. Shutting down {}.", id,
                        MAX_RECONNECT_COUNT, getSessionType());
                shutdownImpl("Maximum retry count (" + MAX_RECONNECT_COUNT
                        + ") exceeded. Shutting down " + getSessionType());
            } else {
                getLogger().debug("[{}] Maximum retry count ({}}) exceeded. But writer is already shut down.", id,
                        MAX_RECONNECT_COUNT);
            }
        }
        if (isReconnecting.compareAndSet(false, true)) {
            reconnectCounter.set(currentReconnectCounter);
            int delayMs = currentReconnectCounter <= EXP_BACKOFF_MAX_POWER
                    ? EXP_BACKOFF_BASE_MS * (1 << currentReconnectCounter)
                    : EXP_BACKOFF_CEILING_MS;
            // Add jitter
            delayMs = delayMs + ThreadLocalRandom.current().nextInt(delayMs);
            getLogger().warn("[{}] Retry #{}. Scheduling reconnect in {}ms...", id, currentReconnectCounter, delayMs);
            scheduler.schedule(this::reconnect, delayMs, TimeUnit.MILLISECONDS);
        } else {
            getLogger().info("[{}] Should reconnect, but reconnect is already in progress", id);
        }
    }
    void reconnect() {
        if (!isReconnecting.compareAndSet(true, false)) {
            getLogger().warn("[{}] Couldn't reset reconnect flag. Shouldn't happen", id);
        }
        onReconnect();
    }
    protected CompletableFuture<Void> shutdownImpl() {
        return shutdownImpl("");
    }

    private CompletableFuture<Void> shutdownImpl(String reason) {
        getLogger().info("[{}] Shutting down Topic {}"
                        + (reason == null || reason.isEmpty() ? "" : " with reason: " + reason), id, getSessionType());
        isStopped.set(true);
        return CompletableFuture.runAsync(() -> {
            session.shutdown();
            onShutdown(reason);
        });
    }

    protected void completeSession(Status status, Throwable th) {
        getLogger().info("[{}] CompleteSession called", id);
        // This session is not working anymore
        this.session.stop();
        onSessionStop();

        if (th != null) {
            getLogger().error("[{}] Exception in {} stream session {}: ", id, getSessionType(), currentSessionId, th);
        } else {
            if (status.isSuccess()) {
                if (isStopped.get()) {
                    getLogger().info("[{}] {} stream session {} closed successfully", id, getSessionType(),
                            currentSessionId);
                    return;
                } else {
                    getLogger().warn("[{}] {} stream session {} was closed unexpectedly", id, getSessionType(),
                            currentSessionId);
                }
            } else {
                getLogger().warn("[{}] Error in {} stream session {}: {}", id, getSessionType(), currentSessionId,
                        status);
            }
        }

        if (!isStopped.get()) {
            tryScheduleReconnect();
        } else  {
            getLogger().info("[{}] {} is already stopped, no need to schedule reconnect", id, getSessionType());
        }
    }

}
