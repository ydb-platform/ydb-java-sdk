package tech.ydb.coordination.impl;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import tech.ydb.coordination.CoordinationSessionNew.CoordinationSemaphore;
import tech.ydb.coordination.CoordinationSessionNew.DescribeMode;
import tech.ydb.coordination.CoordinationSessionNew.WatchMode;
import tech.ydb.coordination.settings.DescribeSemaphoreChanged;
import tech.ydb.coordination.settings.SemaphoreDescription;
import tech.ydb.core.Result;
import tech.ydb.core.Status;

public class CoordinationSemaphoreImpl implements CoordinationSemaphore {
    private static final byte[] BYTE_ARRAY_STUB = new byte[0];
    private final String name;
    private final AtomicBoolean isDeleted = new AtomicBoolean(false);
    private final int semaphoreId;
    private final CoordinationRetryableStreamImpl stream;
    AtomicInteger nextId;

    CoordinationSemaphoreImpl(CoordinationRetryableStreamImpl stream, int semaphoreId, String name,
                              AtomicInteger nextId) {
        this.stream = stream;
        this.name = name;
        this.semaphoreId = semaphoreId;
        this.nextId = nextId;
    }

    @Override
    public CompletableFuture<Status> update(byte[] data) {
        final int id = nextId.getAndIncrement();
        checkDeleted();
        return stream.sendUpdateSemaphore(name, data, id);
    }

    @Override
    public CompletableFuture<Result<Boolean>> acquire(long count, Duration timeout, byte[] data) {
        if (data == null) {
            data = BYTE_ARRAY_STUB;
        }
        final int id = nextId.getAndIncrement();
        checkDeleted();
        return stream.sendAcquireSemaphore(name, count, timeout, false, data, id);
    }

    @Override
    public CompletableFuture<Result<Boolean>> release() {
        final int id = nextId.getAndIncrement();
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
        final int id = nextId.getAndIncrement();
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
        stream.removeUpdateWatcher(semaphoreId);
        release();
    }

    private void checkDeleted() {
        if (isDeleted.get()) {
            throw new IllegalStateException("Semaphore has already deleted");
        }
    }
}

class CoordinationEphemeralSemaphoreImpl extends CoordinationSemaphoreImpl {

    CoordinationEphemeralSemaphoreImpl(CoordinationRetryableStreamImpl stream, int semaphoreId, String name,
                                       AtomicInteger nextId) {
        super(stream, semaphoreId, name, nextId);
    }

    @Override
    public CompletableFuture<Status> delete(boolean force) {
        throw new UnsupportedOperationException("Ephemeral semaphore couldn't be deleted.");
    }
}