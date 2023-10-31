package tech.ydb.coordination.scenario.service_discovery;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;

import tech.ydb.coordination.CoordinationSession;
import tech.ydb.coordination.SemaphoreLease;
import tech.ydb.core.UnexpectedResultException;

public class Worker {
    public static final String SEMAPHORE_NAME = "service-discovery-semaphore";
    private final SemaphoreLease semaphore;

    private Worker(SemaphoreLease semaphore) {
        this.semaphore = semaphore;
    }

    public static CompletableFuture<Worker> newWorker(CoordinationSession session, String endpoint,
                                                      Duration maxAttemptTimeout) {
        byte[] data = endpoint.getBytes(StandardCharsets.UTF_8);
        return session.acquireSemaphore(SEMAPHORE_NAME, 1, data, maxAttemptTimeout).thenApply(lease -> {
            if (lease.isValid()) {
                return new Worker(lease);
            } else {
                throw new UnexpectedResultException("The semaphore for Worker wasn't acquired.",
                        lease.getStatusFuture().join());
            }
        });
    }

    public CompletableFuture<Boolean> stop() {
        return semaphore.release();
    }
}
