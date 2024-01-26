package tech.ydb.coordination.impl;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tech.ydb.coordination.CoordinationSession;
import tech.ydb.coordination.SemaphoreLease;
import tech.ydb.coordination.description.SemaphoreChangedEvent;
import tech.ydb.coordination.description.SemaphoreDescription;
import tech.ydb.coordination.description.SemaphoreWatcher;
import tech.ydb.coordination.rpc.CoordinationRpc;
import tech.ydb.coordination.settings.CoordinationSessionSettings;
import tech.ydb.coordination.settings.DescribeSemaphoreMode;
import tech.ydb.coordination.settings.WatchSemaphoreMode;
import tech.ydb.core.Result;
import tech.ydb.core.Status;

public class CoordinationSessionImpl implements CoordinationSession {
    private static final Logger logger = LoggerFactory.getLogger(CoordinationSession.class);
    private static final byte[] BYTE_ARRAY_STUB = new byte[0];

    private final Duration connectTimeout;
    private final CoordinationRetryableStreamImpl stream;
    private final AtomicBoolean isWorking = new AtomicBoolean(true);
    private final AtomicInteger lastId = new AtomicInteger(ThreadLocalRandom.current().nextInt());

    protected CoordinationSessionImpl(CoordinationRpc rpc, String nodePath,
            CoordinationSessionSettings settings) {
        Executor executor = settings.getExecutor();
        if (executor == null) {
            executor = ForkJoinPool.commonPool();
        }

        this.connectTimeout = settings.getConnectTimeout();
        this.stream = new CoordinationRetryableStreamImpl(rpc, executor, nodePath);
    }

    @Override
    public CompletableFuture<Status> connect() {
        return this.stream.start(connectTimeout);
    }

    @Override
    public Long getId() {
        return this.stream.getId();
    }

    @Override
    public State getState() {
        return stream.getState();
    }

    @Override
    public void addStateListener(Consumer<State> listener) {
        stream.addStateListener(listener);
    }

    @Override
    public void removeStateListener(Consumer<State> listener) {
        stream.removeStateListener(listener);
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
    public CompletableFuture<Result<SemaphoreLease>> acquireSemaphore(String name, long count, byte[] data,
            Duration timeout) {
        byte[] sepamhoreData = data != null ? data : BYTE_ARRAY_STUB;
        final int reqId = lastId.getAndIncrement();
        logger.trace("Send acquireSemaphore {} with count {}", name, count);
        return stream.sendAcquireSemaphore(name, count, timeout, false, sepamhoreData, reqId)
                .thenApply(r -> r.map(v -> new SemaphoreLeaseImpl(this, name)));
    }

    @Override
    public CompletableFuture<Result<SemaphoreLease>> acquireEphemeralSemaphore(String name, boolean exclusive,
            byte[] data, Duration timeout) {
        byte[] sepamhoreData = data != null ? data : BYTE_ARRAY_STUB;
        final int reqId = lastId.getAndIncrement();
        logger.trace("Send acquireEphemeralSemaphore {}", name);
        long limit = exclusive ? -1L : 1L;
        return stream.sendAcquireSemaphore(name, limit, timeout, true, sepamhoreData, reqId)
                .thenApply(r -> r.map(v -> new SemaphoreLeaseImpl(this, name)));
    }

    CompletableFuture<Boolean> releaseSemaphore(String name) {
        final int semaphoreReleaseId = lastId.getAndIncrement();
        logger.trace("Send releaseSemaphore {}", name);
        return stream.sendReleaseSemaphore(name, semaphoreReleaseId).thenApply(Result::getValue);
    }

    @Override
    public CompletableFuture<Status> updateSemaphore(String semaphoreName, byte[] data) {
        if (data == null) {
            data = BYTE_ARRAY_STUB;
        }
        return stream.sendUpdateSemaphore(semaphoreName, data, lastId.getAndIncrement());
    }

    @Override
    public CompletableFuture<Result<SemaphoreDescription>> describeSemaphore(String name, DescribeSemaphoreMode mode) {
        return stream.sendDescribeSemaphore(name, mode.includeOwners(), mode.includeWaiters());
    }

    @Override
    public CompletableFuture<Result<SemaphoreWatcher>> describeAndWatchSemaphore(String name,
            DescribeSemaphoreMode describeMode, WatchSemaphoreMode watchMode) {
        final CompletableFuture<SemaphoreChangedEvent> changeFuture = new CompletableFuture<>();
        return stream.sendDescribeSemaphore(name,
                describeMode.includeOwners(), describeMode.includeWaiters(),
                watchMode.watchData(), watchMode.watchOwners(),
                changeFuture::complete
        ).thenApply(r -> r.map(desc -> new SemaphoreWatcher(desc, changeFuture)));
    }

    @Override
    public CompletableFuture<Status> deleteSemaphore(String semaphoreName, boolean force) {
        return stream.sendDeleteSemaphore(semaphoreName, force, lastId.getAndIncrement());
    }

    @Override
    public void close() {
        logger.trace("Close session with id={}", stream.getId());
        if (isWorking.compareAndSet(true, false)) {
            stream.stop();
        }
    }
}
