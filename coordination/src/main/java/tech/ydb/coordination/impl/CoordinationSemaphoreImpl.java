package tech.ydb.coordination.impl;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

import tech.ydb.coordination.CoordinationSessionNew.CoordinationSemaphore;
import tech.ydb.core.Result;

public class CoordinationSemaphoreImpl implements CoordinationSemaphore {
    private final String name;
    private final CoordinationRetryableStreamImpl stream;
    AtomicInteger nextId;

    CoordinationSemaphoreImpl(CoordinationRetryableStreamImpl stream, String name, AtomicInteger nextId) {
        this.stream = stream;
        this.name = name;
        this.nextId = nextId;
    }

    @Override
    public CompletableFuture<Result<Boolean>> release() {
        final int id = nextId.getAndIncrement();
        return stream.sendReleaseSemaphore(name, id);
    }
}