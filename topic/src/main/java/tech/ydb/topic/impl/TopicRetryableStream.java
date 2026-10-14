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

    private final AtomicReference<State> state = new AtomicReference<>(null);
    private final AtomicInteger streamCount = new AtomicInteger(0);
    private final RetryState retryState = new RetryState();

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
        State newState = new State(createNewStream(streamID));

        if (!state.compareAndSet(null, newState)) {
            logger.warn("[{}] double start of stream, skipping", debugId);
            newState.close();
            return;
        }

        newState.start();
        if (isClosed) {
            newState.close();
        }
    }

    protected void resetRetries() {
        retryState.reset();
    }

    public boolean isClosed() {
        return isClosed;
    }

    public void fail(Status status) {
        State local = state.getAndSet(null);
        if (local != null) {
            logger.warn("[{}] failed by application-side error {}", debugId, status);
            local.close();
            onStreamStop(local.stream, status, retryConfig.getStatusRetryPolicy(status));
        }
    }

    public void send(W msg) {
        State local = state.get();
        if (isClosed || local == null) {
            logger.warn("[{}] send message before starting or after closing", debugId);
            return;
        }
        if (local.stream == null) {
            logger.warn("[{}] send message before stream is ready", debugId);
            return;
        }

        local.stream.send(msg);
    }

    public boolean close() {
        isClosed = true;
        State local = state.get();
        if (local != null) {
            local.close();
        }
        return local != null;
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

        long nextRetryMs = retryState.nextRetryMs(policy);

        if (nextRetryMs < 0) {
            logger.warn("[{}] stopped after retry policy evaluation for status {}", debugId, status);
            isClosed = true;
            onClose(closed, status);
            return;
        }

        if (nextRetryMs == 0) { // retry immediately
            logger.warn("[{}] retry #{}. Retry immediately...", debugId, retryState.retryNumber());
            onRetry(closed, status);
            start();
            return;
        }

        // retry scheduling
        logger.warn("[{}] retry #{}. Scheduling reconnect in {}ms...", debugId, retryState.retryNumber(), nextRetryMs);
        onRetry(closed, status);

        try {
            scheduler.schedule(this::start, nextRetryMs, TimeUnit.MILLISECONDS);
        } catch (Exception ex) {
            logger.error("[{}] cannot schedule reconnect, stopping", debugId, ex);
            isClosed = true;
            onClose(closed, status);
        }
    }

    private class State {
        private final CompletableFuture<Result<S>> future;
        private final S stream;

        State(CompletableFuture<Result<S>> future) {
            this(future, null);
        }

        State(CompletableFuture<Result<S>> future, S stream) {
            this.future = future;
            this.stream = stream;
        }

        public void start() {
            this.future.whenComplete((res, th) -> {
                if (res == null || !res.isSuccess()) {
                    if (!state.compareAndSet(this, null)) {
                        return;
                    }

                    if (res != null) {
                        onStreamStop(null, res.getStatus(), retryConfig.getStatusRetryPolicy(res.getStatus()));
                    } else {
                        Status wrapped = Status.of(StatusCode.CLIENT_INTERNAL_ERROR, th);
                        onStreamStop(null, wrapped, retryConfig.getThrowableRetryPolicy(th));
                    }
                    return;
                }

                State next = new State(future, res.getValue());
                if (state.compareAndSet(this, next)) {
                    next.startStream();
                }
            });
        }

        public boolean close() {
            if (stream != null) {
                stream.close();
                return true;
            }

            this.future.whenComplete((res, th) -> {
                if (res != null && res.isSuccess()) {
                    res.getValue().close();
                }
            });

            if (!state.compareAndSet(this, null)) {
                return false;
            }

            onStreamStop(null, Status.SUCCESS, retryConfig.getStatusRetryPolicy(Status.SUCCESS));
            return true;
        }

        private void startStream() {
            stream.start(msg -> onNext(stream, msg)).whenComplete((status, th) -> {
                if (!state.compareAndSet(this, null)) {
                    return;
                }

                if (status != null) {
                    onStreamStop(stream, status, retryConfig.getStatusRetryPolicy(status));
                } else {
                    Status wrapped = Status.of(StatusCode.CLIENT_INTERNAL_ERROR, th);
                    onStreamStop(stream, wrapped, retryConfig.getThrowableRetryPolicy(th));
                }
            });
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
