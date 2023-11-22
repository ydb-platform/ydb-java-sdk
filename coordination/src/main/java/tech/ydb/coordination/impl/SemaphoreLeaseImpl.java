package tech.ydb.coordination.impl;

import java.util.concurrent.CompletableFuture;

import tech.ydb.coordination.CoordinationSession;
import tech.ydb.coordination.SemaphoreLease;
import tech.ydb.core.Status;

public class SemaphoreLeaseImpl implements SemaphoreLease {
    private final CoordinationSessionImpl session;
    private final String name;
    private final CompletableFuture<Status> statusFuture = new CompletableFuture<>();

    public SemaphoreLeaseImpl(CoordinationSessionImpl session, String name) {
        this.session = session;
        this.name = name;
    }

    @Override
    public CoordinationSession getSession() {
        return session;
    }

    @Override
    public String getSemaphoreName() {
        return name;
    }

    @Override
    public CompletableFuture<Void> release() {
        return session.releaseSemaphore(name).thenApply(r -> null);
    }

    void completeLease(Status status) {
        statusFuture.complete(status);
    }

    @Override
    public boolean isActive() {
        return statusFuture.isDone();
    }
}
