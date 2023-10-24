package tech.ydb.coordination.impl;

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tech.ydb.coordination.CoordinationSessionNew;
import tech.ydb.coordination.settings.DescribeSemaphoreChanged;
import tech.ydb.coordination.settings.SemaphoreDescription;
import tech.ydb.core.Issue;
import tech.ydb.core.Issue.Severity;
import tech.ydb.core.Result;
import tech.ydb.core.Status;
import tech.ydb.core.StatusCode;

public class CoordinationSessionNewImpl implements CoordinationSessionNew {
    private static final Logger logger = LoggerFactory.getLogger(CoordinationSessionNew.class);
    private static final byte[] BYTE_ARRAY_STUB = new byte[0];
    private final CoordinationRetryableStreamImpl stream;
    private final AtomicBoolean isWorking = new AtomicBoolean(true);
    private final AtomicLong sessionId = new AtomicLong();
    private final AtomicInteger lastId = new AtomicInteger(ThreadLocalRandom.current().nextInt());

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
    public CompletableFuture<Status> createSemaphore(String semaphoreName, long limit,
                                                     byte[] data) {
        if (data == null) {
            data = BYTE_ARRAY_STUB;
        }
        final int semaphoreId = lastId.getAndIncrement();
        logger.trace("Send createSemaphore {} with limit {}", semaphoreName, limit);
        return stream.sendCreateSemaphore(semaphoreName, limit, data, semaphoreId);
    }

    @Override
    public CompletableFuture<Result<CoordinationSemaphore>> acquireSemaphore(
            String semaphoreName, long count, boolean ephemeral, Duration timeout, byte[] data) {
        if (data == null) {
            data = BYTE_ARRAY_STUB;
        }
        final int semaphoreCreateId = lastId.getAndIncrement();
        logger.trace("Send acquireSemaphore {} with count {}", semaphoreName, count);
        return stream.sendAcquireSemaphore(semaphoreName, count, timeout, ephemeral, data, semaphoreCreateId)
                .thenApply(result -> (result.isSuccess() && result.getValue()) ?
                        Result.success(new CoordinationSemaphoreImpl(
                                stream, semaphoreName, lastId)
                        ) :
                        Result.fail(result.isSuccess() ?
                                Status.of(
                                        StatusCode.BAD_REQUEST,
                                        (double) 0, Issue.of("Semaphore has already existed.", Severity.WARNING)) :
                                result.getStatus())
                );
    }

    @Override
    public CompletableFuture<Status> updateSemaphore(String semaphoreName, byte[] data) {
        if (data == null) {
            data = BYTE_ARRAY_STUB;
        }
        return stream.sendUpdateSemaphore(semaphoreName, data, lastId.getAndIncrement());
    }

    @Override
    public CompletableFuture<Result<SemaphoreDescription>> describeSemaphore(String semaphoreName,
                                                                             DescribeMode describeMode,
                                                                             WatchMode watchMode,
                                                                             Consumer<DescribeSemaphoreChanged> updateWatcher) {
        return stream.sendDescribeSemaphore(semaphoreName, describeMode.includeOwners(), describeMode.includeWaiters(),
                watchMode.watchData(), watchMode.watchData(), updateWatcher);
    }

    @Override
    public CompletableFuture<Result<SemaphoreDescription>> describeSemaphore(String semaphoreName, DescribeMode mode) {
        return stream.sendDescribeSemaphore(semaphoreName, mode.includeOwners(), mode.includeWaiters());
    }

    @Override
    public CompletableFuture<Status> deleteSemaphore(String semaphoreName, boolean force) {
        return stream.sendDeleteSemaphore(semaphoreName, force, lastId.getAndIncrement());
    }

    @Override
    public long getId() {
        return sessionId.get();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof CoordinationSessionNewImpl)) {
            return false;
        }
        CoordinationSessionNewImpl that = (CoordinationSessionNewImpl) o;
        return Objects.equals(sessionId, that.sessionId);
    }

    @Override
    public int hashCode() {
        return Math.toIntExact(sessionId.get());
    }

    @Override
    public void close() {
        logger.trace("Close session with id={}", sessionId.get());
        if (isWorking.compareAndSet(true, false)) {
            stream.stop();
        }
    }
}
