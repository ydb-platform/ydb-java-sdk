package tech.ydb.coordination.impl;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

import tech.ydb.coordination.CoordinationSessionNew;
import tech.ydb.coordination.settings.DescribeSemaphoreChanged;
import tech.ydb.coordination.settings.SemaphoreDescription;
import tech.ydb.core.Result;
import tech.ydb.core.Status;
import tech.ydb.core.StatusCode;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CoordinationSessionNewImpl implements CoordinationSessionNew {
    // TODO: Add logs
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
        return stream.sendCreateSemaphore(semaphoreName, limit, data, semaphoreId)
                .thenApply(status -> (status.isSuccess() || status.getCode() == StatusCode.ALREADY_EXISTS) ?
                        Result.success(new CoordinationSemaphoreImpl(semaphoreName)) :
                        Result.fail(status));
    }

    @Override
    public CompletableFuture<Result<CoordinationSemaphore>> acquireEphemeralSemaphore(
            String semaphoreName, long count, Duration timeout, byte[] data) {
        if (data == null) {
            data = BYTE_ARRAY_STUB;
        }
        final int semaphoreCreateId = lastId.getAndIncrement();
        final CompletableFuture<Status> acquireFuture = new CompletableFuture<>();
        stream.sendAcquireSemaphore(semaphoreName, count, timeout, true, data, semaphoreCreateId);

        return acquireFuture.thenApply(status -> status.isSuccess() ?
                Result.success(new CoordinationEphemeralSemaphoreImpl(semaphoreName, semaphoreCreateId)) :
                Result.fail(status)
        );
    }

    @Override
    public void close() {
        if (isWorking.compareAndSet(true, false)) {
            stream.stop();
        }
    }

    public class CoordinationSemaphoreImpl implements CoordinationSemaphore {
        private final String name;
        private final AtomicBoolean isDeleted = new AtomicBoolean(false);
        private final int semaphoreId;

        public CoordinationSemaphoreImpl(String name) {
            this.name = name;
            semaphoreId = lastId.getAndIncrement();
        }

        @Override
        public CompletableFuture<Status> update(byte[] data) {
            final int id = lastId.getAndIncrement();
            checkDeleted();
            return stream.sendUpdateSemaphore(name, data, id);
        }

        @Override
        public CompletableFuture<Result<Boolean>> acquire(long count, Duration timeout, byte[] data) {
            if (data == null) {
                data = BYTE_ARRAY_STUB;
            }
            final int id = lastId.getAndIncrement();
            checkDeleted();
            return stream.sendAcquireSemaphore(name, count, timeout, false, data, id);
        }

        @Override
        public CompletableFuture<Result<Boolean>> release() {
            final int id = lastId.getAndIncrement();
            checkDeleted();
            return stream.sendReleaseSemaphore(name, id);
        }

        @Override
        public CompletableFuture<Result<SemaphoreDescription>> describe(DescribeMode describeMode, WatchMode watchMode,
                                                                Consumer<DescribeSemaphoreChanged> updateWatcher) {
            checkDeleted();
            return stream.sendDescribeSemaphore(name,
                    describeMode.includeOwners(),
                    describeMode.includeWaiters(),
                    watchMode.watchData(),
                    watchMode.watchOwners(),
                    updateWatcher, semaphoreId);
        }

        @Override
        public CompletableFuture<Status> delete(boolean force) {
            final int id = lastId.getAndIncrement();
            checkDeleted();
            final CompletableFuture<Status> deleteFuture = stream.sendDeleteSemaphore(name, force, id);
            deleteFuture.whenComplete((status, th) -> {
                if (th == null && status.isSuccess()) {
                    isDeleted.set(true);
                }
            });
            return deleteFuture;
        }

        @Override
        public void close() {
            release();
        }

        private void checkDeleted() {
            if (isDeleted.get()) {
                throw new IllegalStateException("Semaphore has already deleted");
            }
        }
    }

    public class CoordinationEphemeralSemaphoreImpl extends CoordinationSemaphoreImpl {

        public CoordinationEphemeralSemaphoreImpl(String name, int id) {
            super(name);
        }

        @Override
        public CompletableFuture<Status> delete(boolean force) {
            throw new UnsupportedOperationException("Ephemeral semaphore couldn't be deleted.");
        }
    }
}
