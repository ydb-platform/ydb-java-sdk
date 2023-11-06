package tech.ydb.coordination.scenario.configuration;

import java.util.concurrent.CompletableFuture;

import tech.ydb.coordination.CoordinationClient;
import tech.ydb.coordination.CoordinationSession;
import tech.ydb.core.Status;
import tech.ydb.core.StatusCode;

public class Publisher implements AutoCloseable {
    static final String SEMAPHORE_PREFIX = "configuration-";
    private final String semaphoreName;
    private final CoordinationSession session;
    private CompletableFuture<Status> semaphoreFuture;

    private Publisher(CoordinationSession session, String semaphoreName) {
        this.session = session;
        this.semaphoreName = semaphoreName;
        this.semaphoreFuture = session.createSemaphore(semaphoreName, 1);
    }

    public static CompletableFuture<Publisher> newPublisher(CoordinationClient client, String path, long token) {
        return client.createSession(path)
                .thenApply(session -> new Publisher(session, SEMAPHORE_PREFIX + token));
    }

    public synchronized CompletableFuture<Status> publish(byte[] data) {
        if (semaphoreFuture.isDone()) {
            semaphoreFuture = session.updateSemaphore(semaphoreName, data);
        } else {
            semaphoreFuture = semaphoreFuture.thenCompose(status -> session.updateSemaphore(semaphoreName, data));
        }
        return semaphoreFuture;
    }

    @Override
    public void close() {
        semaphoreFuture.complete(Status.of(StatusCode.ABORTED));
        session.close();
    }
}
