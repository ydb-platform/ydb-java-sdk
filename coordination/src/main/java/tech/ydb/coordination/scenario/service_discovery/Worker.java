package tech.ydb.coordination.scenario.service_discovery;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;

import tech.ydb.coordination.CoordinationSession;
import tech.ydb.coordination.CoordinationSession.CoordinationSemaphore;
import tech.ydb.core.Result;
import tech.ydb.core.UnexpectedResultException;

public class Worker {
    public static final String SEMAPHORE_NAME = "service-discovery-semaphore";
    private final CoordinationSemaphore semaphore;

    private Worker(CoordinationSemaphore semaphore) {
        this.semaphore = semaphore;
    }

    public static CompletableFuture<Worker> newWorker(CoordinationSession session, String endpoint,
                                                      Duration maxAttemptTimeout) {
        return session.acquireSemaphore(SEMAPHORE_NAME, 1, true, maxAttemptTimeout,
                endpoint.getBytes(StandardCharsets.UTF_8)).thenApply(semaphoreResult -> {
            if (semaphoreResult.isSuccess()) {
                return new Worker(semaphoreResult.getValue());
            } else {
                throw new UnexpectedResultException("The semaphore for Worker wasn't acquired.",
                        semaphoreResult.getStatus());
            }
        });
    }

    public CompletableFuture<Result<Boolean>> stop() {
        return semaphore.release();
    }
}
