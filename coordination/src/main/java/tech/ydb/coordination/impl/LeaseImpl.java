package tech.ydb.coordination.impl;

import java.util.concurrent.CompletableFuture;

import tech.ydb.coordination.CoordinationSession;
import tech.ydb.coordination.SemaphoreLease;

/**
 *
 * @author Aleksandr Gorshenin
 */
class LeaseImpl implements SemaphoreLease {
    private final SessionImpl session;
    private final String name;

    LeaseImpl(SessionImpl session, String name) {
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
}
