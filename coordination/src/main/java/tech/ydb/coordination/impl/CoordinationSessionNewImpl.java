package tech.ydb.coordination.impl;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

import tech.ydb.coordination.CoordinationSession.Observer;
import tech.ydb.coordination.CoordinationSessionNew;
import tech.ydb.coordination.settings.DescribeSemaphoreChanged;
import tech.ydb.coordination.settings.SemaphoreDescription;
import tech.ydb.core.Result;
import tech.ydb.core.Status;
import tech.ydb.core.StatusCode;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CoordinationSessionNewImpl implements CoordinationSessionNew {
    // Add logs
    private static final Logger logger = LoggerFactory.getLogger(CoordinationSessionNew.class);
    private static final byte[] BYTE_ARRAY_STUB = new byte[0];
    private final CoordinationRetryableStreamImpl stream;
    private final AtomicBoolean isWorking = new AtomicBoolean(true);
    private final AtomicLong sessionId = new AtomicLong();
    private final AtomicInteger lastId = new AtomicInteger(1);
    private final Map<Integer, CompletableFuture<Status>> createSemaphoreFutures =
            new ConcurrentHashMap<>();
    private final Map<Integer, CompletableFuture<Status>> createEphemeralSemaphoreFutures = new ConcurrentHashMap<>();
    private final Map<Integer, CompletableFuture<Result<Boolean>>> acquireSemaphoreFutures = new ConcurrentHashMap<>();
    private final Map<Integer, CompletableFuture<Result<Boolean>>> releaseSemaphoreFutures = new ConcurrentHashMap<>();
    private final Map<Integer, CompletableFuture<Result<SemaphoreDescription>>> describeSemaphoreFutures =
            new ConcurrentHashMap<>();
    private final Map<Integer, CompletableFuture<Status>> deleteSemaphoreFutures = new ConcurrentHashMap<>();
    private final Map<Integer, Consumer<DescribeSemaphoreChanged>> updateWatchers = new ConcurrentHashMap<>();
    private final Map<Integer, CompletableFuture<Status>> updateSemaphoreFutures = new ConcurrentHashMap<>();

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
        return stream.start(timeout, new Observer() {
            @Override
            public void onAcquireSemaphoreResult(boolean acquired, Status status, int requestId) {
                final CompletableFuture<Status> acquireEphemeralSemaphore;
                // TODO: write tests with Ephemeral Semaphores
                if ((acquireEphemeralSemaphore = createEphemeralSemaphoreFutures.get(requestId)) != null) {
                    acquireEphemeralSemaphore.complete(status);
                } else {
                    acquireSemaphoreFutures.remove(requestId).complete(status.isSuccess() ? Result.success(acquired) :
                            Result.fail(status));
                }
            }

            @Override
            public void onDescribeSemaphoreResult(SemaphoreDescription description, Status status, int requestId) {
                describeSemaphoreFutures.remove(requestId).complete(status.isSuccess() ? Result.success(description) :
                        Result.fail(status));
            }

            @Override
            public void onDescribeSemaphoreChanged(boolean dataChanged, boolean ownersChanged, int requestId) {
                updateWatchers.get(requestId).accept(new DescribeSemaphoreChanged(dataChanged, ownersChanged));
            }

            @Override
            public void onDeleteSemaphoreResult(Status status, int requestId) {
                deleteSemaphoreFutures.remove(requestId).complete(status);
            }

            @Override
            public void onCreateSemaphoreResult(Status status, int requestId) {
                logger.debug("onCreateSemaphoreResult");
                createSemaphoreFutures.remove(requestId).complete(status);
            }

            @Override
            public void onReleaseSemaphoreResult(boolean released, Status status, int requestId) {
                releaseSemaphoreFutures.remove(requestId).complete(status.isSuccess() ? Result.success(released) :
                        Result.fail(status));
            }

            @Override
            public void onUpdateSemaphoreResult(Status status, int requestId) {
                updateSemaphoreFutures.remove(requestId).complete(status);
            }
        });
    }

    @Override
    public CompletableFuture<Result<CoordinationSemaphore>> createSemaphore(String semaphoreName, long limit,
                                                                            byte[] data) {
        if (data == null) {
            data = BYTE_ARRAY_STUB;
        }
        final int semaphoreId = lastId.getAndIncrement();
        final CompletableFuture<Status> createFuture = new CompletableFuture<>();
        createSemaphoreFutures.put(semaphoreId, createFuture);
        stream.sendCreateSemaphore(semaphoreName, limit, data, semaphoreId);

        return createFuture.thenApply(status -> (status.isSuccess() || status.getCode() == StatusCode.ALREADY_EXISTS) ?
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
        createEphemeralSemaphoreFutures.put(semaphoreCreateId, acquireFuture);
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
        private int lastDescribeId = -1;

        public CoordinationSemaphoreImpl(String name) {
            this.name = name;
        }

        @Override
        public CompletableFuture<Status> update(byte[] data) {
            final int id = lastId.getAndIncrement();
            checkDeleted();
            final CompletableFuture<Status> updateFuture = new CompletableFuture<>();
            updateSemaphoreFutures.put(id, updateFuture);
            stream.sendUpdateSemaphore(name, data, id);
            return updateFuture;
        }

        @Override
        public CompletableFuture<Result<Boolean>> acquire(long count, Duration timeout, byte[] data) {
            if (data == null) {
                data = BYTE_ARRAY_STUB;
            }
            final int id = lastId.getAndIncrement();
            checkDeleted();
            final CompletableFuture<Result<Boolean>> acquireFuture = new CompletableFuture<>();
            acquireSemaphoreFutures.put(id, acquireFuture);
            stream.sendAcquireSemaphore(name, count, timeout, false, data, id);
            return acquireFuture;
        }

        @Override
        public CompletableFuture<Result<Boolean>> release() {
            final int id = lastId.getAndIncrement();
            checkDeleted();
            final CompletableFuture<Result<Boolean>> releaseFuture = new CompletableFuture<>();
            releaseSemaphoreFutures.put(id, releaseFuture);
            stream.sendReleaseSemaphore(name, id);
            return releaseFuture;
        }

        @Override
        public CompletableFuture<Result<SemaphoreDescription>> describe(DescribeMode describeMode, WatchMode watchMode,
                                                                Consumer<DescribeSemaphoreChanged> updateWatcher) {
            final int id = lastId.getAndIncrement();
            checkDeleted();
            updateWatchers.remove(lastDescribeId);
            final CompletableFuture<Result<SemaphoreDescription>> describeFuture = new CompletableFuture<>();
            describeSemaphoreFutures.put(id, describeFuture);
            updateWatchers.put(id, updateWatcher);
            lastDescribeId = id;
            stream.sendDescribeSemaphore(name,
                    describeMode.includeOwners(),
                    describeMode.includeWaiters(),
                    watchMode.watchData(),
                    watchMode.watchOwners(),
                    id);
            return describeFuture;
        }

        @Override
        public CompletableFuture<Status> delete(boolean force) {
            final int id = lastId.getAndIncrement();
            checkDeleted();
            final CompletableFuture<Status> deleteFuture = new CompletableFuture<>();
            stream.sendDeleteSemaphore(name, force, id);
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
