package tech.ydb.topic.impl;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import com.google.protobuf.Message;
import org.slf4j.Logger;

import tech.ydb.common.retry.RetryConfig;
import tech.ydb.common.retry.RetryPolicy;
import tech.ydb.core.Status;
import tech.ydb.core.StatusCode;

public abstract class TopicRetryableStream<R extends Message, W extends Message> {
    protected final String debugId;
    private final Logger logger;
    private final RetryConfig retryConfig;
    private final ScheduledExecutorService scheduler;

    private final AtomicReference<TopicStream<R, W>> realStream = new AtomicReference<>();
    private final AtomicInteger streamCount = new AtomicInteger(0);
    private final RetryState state = new RetryState();

    private volatile boolean isClosed = false;

    public TopicRetryableStream(Logger logger, String debugId, RetryConfig config, ScheduledExecutorService scheduler) {
        this.debugId = debugId;
        this.logger = logger;
        this.retryConfig = config;
        this.scheduler = scheduler;
    }

    protected abstract TopicStream<R, W> createNewStream(String debugId);
    protected abstract W getInitRequest();

    protected abstract void onNext(R message);

    protected abstract void onRetry(Status status);
    protected abstract void onClose(Status status);

    public void start() {
        if (isClosed) {
            return;
        }

        String streamID = debugId + '.' + streamCount.incrementAndGet();
        TopicStream<R, W> stream = createNewStream(streamID);

        if (!realStream.compareAndSet(null, stream)) {
            logger.warn("[{}] double start of stream, skipping", debugId);
            return;
        }

        stream.start(getInitRequest(), this::onNext).whenComplete((status, th) -> {
            realStream.compareAndSet(stream, null);
            if (status != null) {
                onStreamStop(status, retryConfig.getStatusRetryPolicy(status));
            }
            if (th != null) {
                Status wrapped = Status.of(StatusCode.CLIENT_INTERNAL_ERROR, th);
                onStreamStop(wrapped, retryConfig.getThrowableRetryPolicy(th));
            }
        });
    }

    protected void resetRetries() {
        state.reset();
    }

    public void send(W msg) {
        TopicStream<R, W> stream = realStream.get();
        if (stream == null) {
            logger.warn("[{}] send message before stream is ready", debugId);
            return;
        }
        stream.send(msg);
    }

    public boolean close() {
        isClosed = true;
        TopicStream<R, W> stream = realStream.getAndSet(null);
        if (stream == null) {
            return false;
        }

        stream.close();
        return true;
    }

    private void onStreamStop(Status status, RetryPolicy policy) {
        if (isClosed) { // stream was already closed (usually with success)
            onClose(status);
            return;
        }

        if (policy == null) {
            logger.warn("[{}] stopped by non-retryable status {}", debugId, status);
            onClose(status);
            return;
        }

        long nextRetryMs = state.nextRetryMs(policy);

        if (nextRetryMs < 0) {
            logger.warn("[{}] stopped after retry policy evaluation for status {}", debugId, status);
            onClose(status);
            return;
        }

        if (nextRetryMs == 0) { // retry immediately
            logger.warn("[{}] retry #{}. Retry immediately...", debugId, state.retryNumber());
            onRetry(status);
            start();
            return;
        }

        // retry scheduling
        logger.warn("[{}] retry #{}. Scheduling reconnect in {}ms...", debugId, state.retryNumber(), nextRetryMs);
        onRetry(status);

        try {
            scheduler.schedule(this::start, nextRetryMs, TimeUnit.MILLISECONDS);
        } catch (Exception ex) {
            logger.error("[{}] cannot schedule reconnect, stopping", debugId, ex);
            onClose(status);
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
