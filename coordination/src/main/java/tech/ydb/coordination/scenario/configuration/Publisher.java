package tech.ydb.coordination.scenario.configuration;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tech.ydb.coordination.CoordinationClient;
import tech.ydb.coordination.CoordinationSession;
import tech.ydb.core.Status;
import tech.ydb.core.StatusCode;

public class Publisher implements AutoCloseable {
    private static final Logger logger = LoggerFactory.getLogger(Publisher.class);
    private final String semaphoreName;
    private final CoordinationSession session;
    private CompletableFuture<Status> semaphoreFuture;

    private Publisher(CoordinationSession session, String semaphoreName) {
        this.session = session;
        this.semaphoreName = semaphoreName;
        this.semaphoreFuture = session.createSemaphore(semaphoreName, 1);
    }

    /**
     * Create new Publisher for Configuration service
     *
     * @param client        - Coordination client
     * @param fullPath      - full path to the coordination node
     * @param semaphoreName - name of Configuration service semaphore
     * @return Completable future with Publisher
     */
    public static CompletableFuture<Publisher> newPublisherAsync(CoordinationClient client, String fullPath,
                                                                 String semaphoreName) {
        return client.createSession(fullPath)
                .thenApply(session -> new Publisher(session, semaphoreName));
    }

    /**
     * {@link Publisher#newPublisherAsync(CoordinationClient, String, String)}
     */
    public static Publisher newPublisher(CoordinationClient client, String path, String semaphoreName) {
        return newPublisherAsync(client, path, semaphoreName).join();
    }

    /**
     * Change data on semaphore
     *
     * @param data - data which all Subscribers will see
     * @return Completable future with status of change data on semaphore
     */
    public synchronized CompletableFuture<Status> publishAsync(byte[] data) {
        return semaphoreFuture = semaphoreFuture.handleAsync((status, th) -> {
            if (status != null && th == null) {
                return session.updateSemaphore(semaphoreName, data);
            }
            logger.warn("Exception when publish data \"{}\": (status: {}, throwable: {}",
                    new String(data, StandardCharsets.UTF_8), status, th);
            return CompletableFuture.completedFuture(Status.of(StatusCode.UNUSED_STATUS));
        }).thenComposeAsync(Function.identity());
    }

    /**
     * {@link Publisher#publishAsync(byte[])}
     */
    public Status publish(byte[] data) {
        return publishAsync(data).join();
    }

    /**
     * Close Publisher with closing session resource
     */
    @Override
    public void close() {
        semaphoreFuture.complete(Status.of(StatusCode.ABORTED));
        session.close();
    }
}
