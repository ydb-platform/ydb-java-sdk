package tech.ydb.coordination.impl;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import tech.ydb.coordination.CoordinationSessionNew;
import tech.ydb.core.Issue;
import tech.ydb.core.Issue.Severity;
import tech.ydb.core.Result;
import tech.ydb.core.Status;
import tech.ydb.core.StatusCode;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CoordinationSessionNewImpl implements CoordinationSessionNew {
    private static final Logger logger = LoggerFactory.getLogger(CoordinationSessionNew.class);
    private static final byte[] BYTE_ARRAY_STUB = new byte[0];
    private final CoordinationRetryableStreamImpl stream;
    private final AtomicBoolean isWorking = new AtomicBoolean(true);
    private final AtomicLong sessionId = new AtomicLong();
    private final AtomicInteger lastId = new AtomicInteger(1);

    protected CoordinationSessionNewImpl(CoordinationRetryableStreamImpl stream) {
        this.stream = stream;
    }

    public static CompletableFuture<CoordinationSessionNew> newSession(final CoordinationRetryableStreamImpl stream,
                                                                       Duration timeout) {
        final CoordinationSessionNewImpl session = new CoordinationSessionNewImpl(stream);
        final CompletableFuture<CoordinationSessionNew> sessionStartFuture = CompletableFuture.completedFuture(session);
        return session.start(timeout)
                .thenAccept(session.sessionId::set)
                .thenCompose(ignored -> sessionStartFuture);
    }

    private CompletableFuture<Long> start(Duration timeout) {
        return stream.start(timeout);
    }

    @Override
    public CompletableFuture<Result<CoordinationSemaphore>> createSemaphore(String semaphoreName, long limit,
                                                                            byte[] data) {
        if (data == null) {
            data = BYTE_ARRAY_STUB;
        }
        final int semaphoreId = lastId.getAndIncrement();
        logger.trace("Send createSemaphore {} with limit {}", semaphoreName, limit);
        return stream.sendCreateSemaphore(semaphoreName, limit, data, semaphoreId)
                .thenApply(status -> (status.isSuccess() || status.getCode() == StatusCode.ALREADY_EXISTS) ?
                        Result.success(new CoordinationSemaphoreImpl(stream, lastId.getAndIncrement(), semaphoreName,
                                lastId)) :
                        Result.fail(status));
    }

    @Override
    public CompletableFuture<Result<CoordinationSemaphore>> acquireEphemeralSemaphore(
            String semaphoreName, long count, Duration timeout, byte[] data) {
        if (data == null) {
            data = BYTE_ARRAY_STUB;
        }
        final int semaphoreCreateId = lastId.getAndIncrement();
        logger.trace("Send acquireEphemeralSemaphore {} with count {}", semaphoreName, count);
        return stream.sendAcquireSemaphore(semaphoreName, count, timeout, true, data, semaphoreCreateId)
                .thenApply(result -> result.isSuccess() && result.getValue() ?
                        Result.success(new CoordinationEphemeralSemaphoreImpl(
                                stream, lastId.getAndIncrement(), semaphoreName, lastId)
                        ) :
                        Result.fail(result.isSuccess() ?
                                Status.of(
                                        StatusCode.BAD_REQUEST,
                                        (double) 0, Issue.of("Semaphore has already existed.", Severity.WARNING)) :
                                result.getStatus())
                );
    }

    @Override
    public void close() {
        logger.trace("Closed");
        if (isWorking.compareAndSet(true, false)) {
            stream.stop();
        }
    }
}
