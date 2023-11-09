package tech.ydb.coordination.scenario.configuration;

import java.util.concurrent.CompletableFuture;

import tech.ydb.coordination.CoordinationClient;
import tech.ydb.coordination.CoordinationSession;
import tech.ydb.core.Status;
import tech.ydb.core.StatusCode;

public class Publisher implements AutoCloseable {
    private final String semaphoreName;
    private final CoordinationSession session;
    private CompletableFuture<Status> semaphoreFuture;

    private Publisher(CoordinationSession session, String semaphoreName) {
        this.session = session;
        this.semaphoreName = semaphoreName;
        this.semaphoreFuture = session.createSemaphore(semaphoreName, 1);
    }

    public static CompletableFuture<Publisher> newPublisherAsync(CoordinationClient client, String path,
                                                                 String semaphoreName) {
        return client.createSession(path)
                .thenApply(session -> new Publisher(session, semaphoreName));
    }

    public static Publisher newPublisher(CoordinationClient client, String path, String semaphoreName) {
        return newPublisherAsync(client, path, semaphoreName).join();
    }

    public synchronized CompletableFuture<Status> publishAsync(byte[] data) {
        if (semaphoreFuture.isDone()) {
            semaphoreFuture = session.updateSemaphore(semaphoreName, data);
        } else {
            semaphoreFuture = semaphoreFuture.thenCompose(status -> session.updateSemaphore(semaphoreName, data));
        }
        return semaphoreFuture;
    }

    public synchronized Status publish(byte[] data) {
        return publishAsync(data).join();
    }

    @Override
    public void close() {
        semaphoreFuture.complete(Status.of(StatusCode.ABORTED));
        session.close();
    }
}
