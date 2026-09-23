package tech.ydb.topic.impl;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import javax.annotation.Nullable;

import com.google.protobuf.Message;
import org.slf4j.Logger;

import tech.ydb.common.retry.RetryConfig;
import tech.ydb.common.retry.RetryPolicy;
import tech.ydb.core.Result;
import tech.ydb.core.Status;
import tech.ydb.core.StatusCode;

public abstract class TopicRetryableStream<R extends Message, W extends Message, S extends TopicStream<R, W>> {
    protected final String debugId;
    private final Logger logger;
    private final RetryConfig retryConfig;
    private final ScheduledExecutorService scheduler;

    private final AtomicInteger streamCount = new AtomicInteger(0);
    private final RetryState state = new RetryState();

    private final AtomicReference<String> realStreamId = new AtomicReference<>();
    private volatile S realStream = null;

    private volatile boolean isClosed = false;

    public TopicRetryableStream(Logger logger, String debugId, RetryConfig config, ScheduledExecutorService scheduler) {
        this.debugId = debugId;
        this.logger = logger;
        this.retryConfig = config;
        this.scheduler = scheduler;
    }

    /**
     * Creates a new stream. Implementations must not block the calling thread: this method is invoked from the shared
     * scheduler on every reconnect, and blocking there stalls discovery, session pools and timeouts of the whole
     * transport.
     *
     * @param debugId identifier of the new stream for logging
     * @return future with the new stream result
     */
    protected abstract CompletableFuture<Result<S>> createNewStream(String debugId);

    protected abstract void onNext(S stream, R message);

    /**
     * @param stream the stopped stream, or {@code null} when stream creation itself failed
     * @param status status the stream stopped with
     */
    protected abstract void onRetry(@Nullable S stream, Status status);

    /**
     * @param stream the closed stream, or {@code null} when stream creation itself failed
     * @param status status the stream was closed with
     */
    protected abstract void onClose(@Nullable S stream, Status status);

    public void start() {
        if (isClosed) {
            logger.warn("[{}] double start of closed stream, ignored", debugId);
            return;
        }

        String streamID = debugId + '.' + streamCount.incrementAndGet();
        createNewStream(streamID).whenComplete((result, th) -> {
            tryStartStream(streamID, result, th);
        });
    }

    private void tryStartStream(String streamID, Result<S> result, Throwable th) {
        if (isClosed) {
            logger.info("[{}] stream was closed while it was creating, skipping", streamID);
            return;
        }

        if (!realStreamId.compareAndSet(null, streamID)) {
            logger.warn("[{}] double start of stream, skipping", streamID);
            return;
        }

        if (result != null && result.isSuccess()) {
            startStream(streamID, result.getValue());
            return;
        }

        if (!realStreamId.compareAndSet(streamID, null)) {
            return;
        }

        if (result == null) {
            logger.warn("[{}] cannot create stream with exeption", streamID, th);
            Status wrapped = Status.of(StatusCode.CLIENT_INTERNAL_ERROR, th);
            onStreamStop(null, wrapped, retryConfig.getThrowableRetryPolicy(th));
        } else {
            logger.warn("[{}] cannot create stream with status {}", streamID, result.getStatus());
            onStreamStop(null, result.getStatus(), retryConfig.getStatusRetryPolicy(result.getStatus()));
        }
    }

    private void startStream(String streamID, S stream) {
        realStream = stream;
        stream.start(msg -> onNext(stream, msg)).whenComplete((status, th) -> {
            if (!realStreamId.compareAndSet(streamID, null)) {
                return;
            }
            if (status != null) {
                onStreamStop(stream, status, retryConfig.getStatusRetryPolicy(status));
            }
            if (th != null) {
                Status wrapped = Status.of(StatusCode.CLIENT_INTERNAL_ERROR, th);
                onStreamStop(stream, wrapped, retryConfig.getThrowableRetryPolicy(th));
            }
        });

        if (isClosed) { // stream may be closed by other thread
            realStream = null;
            stream.close();
        }
    }

    protected void resetRetries() {
        state.reset();
    }

    public boolean isClosed() {
        return isClosed;
    }

    public void fail(Status status) {
        String streamId = realStreamId.getAndSet(null);
        if (streamId != null) {
            logger.warn("[{}] failed by application-side error {}", streamId, status);
            S local = realStream;
            realStream = null;
            if (local != null) {
                local.close();
            }
            onStreamStop(local, status, retryConfig.getStatusRetryPolicy(status));
        }
    }

    public void send(W msg) {
        S local = realStream;
        if (local == null) {
            logger.warn("[{}] send message before stream is ready", debugId);
            return;
        }
        local.send(msg);
    }

    public boolean close() {
        isClosed = true;
        String streamId = realStreamId.getAndSet(null);
        if (streamId == null) {
            return false;
        }

        S local = realStream;
        realStream = null;
        if (local != null) {
            local.close();
        }
        onStreamStop(local, Status.SUCCESS, null);
        return true;
    }

    private void onStreamStop(S closed, Status status, RetryPolicy policy) {
        if (isClosed) { // stream was already closed (usually with success)
            onClose(closed, status);
            return;
        }

        if (policy == null) {
            logger.warn("[{}] stopped by non-retryable status {}", debugId, status);
            isClosed = true;
            onClose(closed, status);
            return;
        }

        long nextRetryMs = state.nextRetryMs(policy);

        if (nextRetryMs < 0) {
            logger.warn("[{}] stopped after retry policy evaluation for status {}", debugId, status);
            isClosed = true;
            onClose(closed, status);
            return;
        }

        if (nextRetryMs == 0) { // retry immediately
            logger.warn("[{}] retry #{}. Retry immediately...", debugId, state.retryNumber());
            onRetry(closed, status);
            start();
            return;
        }

        // retry scheduling
        logger.warn("[{}] retry #{}. Scheduling reconnect in {}ms...", debugId, state.retryNumber(), nextRetryMs);
        onRetry(closed, status);

        try {
            scheduler.schedule(this::start, nextRetryMs, TimeUnit.MILLISECONDS);
        } catch (Exception ex) {
            logger.error("[{}] cannot schedule reconnect, stopping", debugId, ex);
            isClosed = true;
            onClose(closed, status);
        }
    }

    private static class RetryState {
        private final AtomicInteger count = new AtomicInteger();
        private volatile long startedAt = 0;

        public long nextRetryMs(RetryPolicy policy) {
            int retryNumber = count.getAndIncrement();
            if (retryNumber == 0) {
                startedAt = System.currentTimeMillis();
            }
            return policy.nextRetryMs(retryNumber, System.currentTimeMillis() - startedAt);
        }

        public int retryNumber() {
            return count.get();
        }

        public void reset() {
            count.set(0);
        }
    }
}
