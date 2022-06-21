package tech.ydb.table;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;

import tech.ydb.core.Result;


/**
 * @author Aleksandr Gorshenin
 */
public interface SessionSupplier {
    /**
     * Create new session asynchronous
     *
     * @param duration - timeout of operation completion waiting
     * @return Return a new CompletableFuture that is completed with successful
     * Result when session created, and fail Result otherwise
     */
    CompletableFuture<Result<Session>> createSession(Duration duration);

}
