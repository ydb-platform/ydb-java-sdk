package tech.ydb.topic.impl;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tech.ydb.common.retry.RetryConfig;
import tech.ydb.common.retry.RetryPolicy;
import tech.ydb.core.Status;
import tech.ydb.core.StatusCode;

public class TopicRetryableStream<R, W> {
    private static final Logger logger = LoggerFactory.getLogger(TopicRetryableStream.class);

    public interface Handler<R, W> {
        TopicStream<R, W> createNewStream(String id);
        W buildInitRequest();

        void onRetry(Status status);
        void onStop(Status status);

        void onNext(R message);
    }

    private static final int ID_LENGTH = 6;
    private static final char[] ID_ALPHABET = "abcdefghijklmnopqrstuvwxyzABSDEFGHIJKLMNOPQRSTUVWXYZ1234567890"
            .toCharArray();

    private final String id;
    private final Handler<R, W> handler;

    private final RetryConfig config;
    private final ScheduledExecutorService scheduler;

    private final AtomicReference<TopicStream<R, W>> streamRef = new AtomicReference<>();
    private final AtomicInteger streamCount = new AtomicInteger(0);
    private final RetryState state = new RetryState();

    public TopicRetryableStream(String id, RetryConfig retryConfig, ScheduledExecutorService scheduler,
            Handler<R, W> handler) {
        this.id = id == null ? generateRandomId(ID_LENGTH) : id;
        this.handler = handler;
        this.config = retryConfig;
        this.scheduler = scheduler;
    }

    public void start() {
        String streamID = id + '.' + streamCount.incrementAndGet();
        TopicStream<R, W> stream = handler.createNewStream(streamID);

        if (!streamRef.compareAndSet(null, stream)) {
            logger.warn("[{}] Double start of stream retrier, skippingdouble start", id);
            stream.close();
            return;
        }

        stream.start(handler.buildInitRequest(), handler::onNext).whenComplete((status, th) -> {
            if (status != null) {
                onStop(stream, status, config.getStatusRetryPolicy(status));
            }
            if (th != null) {
                onStop(stream, Status.of(StatusCode.CLIENT_INTERNAL_ERROR, th), config.getThrowableRetryPolicy(th));
            }
        });
    }

    public void resetRetries() {
        state.reset();
    }

    public void close() {
        TopicStream<R, W> stream = streamRef.getAndSet(null);
        if (stream != null) {
            stream.close();
        }
    }

    public void send(W msg) {
        TopicStream<R, W> stream = streamRef.get();
        if (stream == null) {
            logger.warn("[{}] send message before stream is ready", id);
            return;
        }
        stream.send(msg);
    }

    private void onStop(TopicStream<R, W> stream, Status status, RetryPolicy policy) {
        if (!streamRef.compareAndSet(stream, null)) { // stream was already closed
            return;
        }

        if (policy == null) {
            logger.warn("[{}] stream stopped by non-retryable status {}", id, status);
            handler.onStop(status);
            return;
        }

        long nextRetryMs = state.nextRetryMs(policy);

        if (nextRetryMs < 0) {
            logger.warn("[{}] stream stopped by retry policy {}", id, status);
            handler.onStop(status);
            return;
        }

        if (nextRetryMs == 0) { // retry immediatelly
            logger.warn("[{}] stream retry #{}. Retry immediatelly...", id, state.retryNumber());
            handler.onRetry(status);
            start();
            return;
        }

        // retry scheduling
        logger.warn("[{}] stream retry #{}. Scheduling reconnect in {}ms...", id, state.retryNumber(), nextRetryMs);
        handler.onRetry(status);

        try {
            scheduler.schedule(this::start, nextRetryMs, TimeUnit.MILLISECONDS);
        } catch (Exception ex) {
            logger.error("[{}] stream cannot schedule reconnect, stopping", id, ex);
            handler.onStop(status);
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

    private static String generateRandomId(int length) {
        return ThreadLocalRandom.current().ints(0, ID_ALPHABET.length)
                .limit(length)
                .map(charId -> ID_ALPHABET[charId])
                .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
                .toString();
    }
}
