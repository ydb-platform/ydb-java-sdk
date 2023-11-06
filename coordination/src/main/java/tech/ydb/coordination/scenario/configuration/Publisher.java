package tech.ydb.coordination.scenario.configuration;

import java.util.concurrent.CompletableFuture;

import tech.ydb.coordination.CoordinationClient;
import tech.ydb.coordination.CoordinationSession;
import tech.ydb.core.Status;

public class Publisher {
    static final String SEMAPHORE_PREFIX = "configuration-";

    private final String semaphoreName;
    private final CoordinationSession session;
    private final CompletableFuture<Status>[] semaphoreFuture;

    private Publisher(CoordinationSession session, String semaphoreName, CompletableFuture<Status>[] semaphoreFuture) {
        this.session = session;
        this.semaphoreName = semaphoreName;
        this.semaphoreFuture = semaphoreFuture;
    }

    public static CompletableFuture<Publisher> newPublisher(CoordinationClient client, String path, long token) {
        return client.createSession(path)
                .thenApply(session -> {
                    final CompletableFuture<Status>[] isSemaphoreCreated = new CompletableFuture[1];
                    final CompletableFuture<Status> createFuture = new CompletableFuture<>();
                    isSemaphoreCreated[0] = createFuture;
                    session.createSemaphore(SEMAPHORE_PREFIX + token, 1).whenComplete(
                            (status, throwable) -> createFuture.complete(status));
                    return new Publisher(session, SEMAPHORE_PREFIX + token, isSemaphoreCreated);
                });
    }

    public synchronized CompletableFuture<Status> publish(byte[] data) {
        if (semaphoreFuture[0].isDone()) {
            semaphoreFuture[0] = session.updateSemaphore(semaphoreName, data);
        } else {
            semaphoreFuture[0] = semaphoreFuture[0].thenCompose(status -> session.updateSemaphore(semaphoreName, data));
        }
        return semaphoreFuture[0];
    }
}
