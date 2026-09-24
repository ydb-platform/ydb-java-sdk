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

    private final AtomicReference<State> state = new AtomicReference<>();
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
            newState.closeWithoutStart();
            return;
        }

        newState.start();

        if (isClosed) {
            close();
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
            onStreamStop(local.closeStream(), status, retryConfig.getStatusRetryPolicy(status));
        }
    }

    public void send(W msg) {
        State local = state.get();
        S stream = local != null ? local.stream.get() : null;
        if (stream == null) {
            logger.warn("[{}] send message before stream is ready", debugId);
            return;
        }
        stream.send(msg);
    }

    public boolean close() {
        isClosed = true;
        State local = state.getAndSet(null);
        if (local == null) {
            return false;
        }

        onStreamStop(local.closeStream(), Status.SUCCESS, null);
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
        private final AtomicReference<S> stream = new AtomicReference<>();

        State(CompletableFuture<Result<S>> future) {
            this.future = future;
        }

        public void start() {
            this.future.whenComplete((res, th) -> {
                if (res == null) {
                    if (state.compareAndSet(this, null)) {
                        Status wrapped = Status.of(StatusCode.CLIENT_INTERNAL_ERROR, th);
                        onStreamStop(null, wrapped, retryConfig.getThrowableRetryPolicy(th));
                    }
                    return;
                }

                if (!res.isSuccess()) {
                    if (state.compareAndSet(this, null)) {
                        onStreamStop(null, res.getStatus(), retryConfig.getStatusRetryPolicy(res.getStatus()));
                    }
                    return;
                }

                startStream(res.getValue());
            });
        }

        public void closeWithoutStart() {
            this.future.whenComplete((res, th) -> {
                if (res != null && res.isSuccess()) {
                    res.getValue().close();
                }
            });
        }

        private void startStream(S local) {
            if (state.get() != this) {
                local.close();
                return;
            }

            stream.set(local);
            local.start(msg -> onNext(local, msg)).whenComplete((status, th) -> {
                if (!state.compareAndSet(this, null)) {
                    return;
                }

                if (status != null) {
                    onStreamStop(local, status, retryConfig.getStatusRetryPolicy(status));
                } else {
                    Status wrapped = Status.of(StatusCode.CLIENT_INTERNAL_ERROR, th);
                    onStreamStop(local, wrapped, retryConfig.getThrowableRetryPolicy(th));
                }
            });

            if (state.get() != this) {
                closeStream();
            }
        }

        public S closeStream() {
            S local = stream.getAndSet(null);
            if (local != null) {
                local.close();
            }
            return local;
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
