package tech.ydb.query;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;

import tech.ydb.core.Result;

/**
 *
 * @author Aleksandr Gorshenin
 */
public interface QueryClient extends AutoCloseable {
    /**
     * Create new session asynchronous
     *
     * @param duration - timeout of operation completion waiting
     * @return Return a new CompletableFuture that is completed with successful
     * Result when session created, and fail Result otherwise
     */
    CompletableFuture<Result<QuerySession>> createSession(Duration duration);

    @Override
    void close();

    interface Builder {
        Builder sessionPoolMinSize(int minSize);
        Builder sessionPoolMaxSize(int maxSize);

        Builder sessionMaxIdleTime(Duration duration);

        QueryClient build();
    }
}
