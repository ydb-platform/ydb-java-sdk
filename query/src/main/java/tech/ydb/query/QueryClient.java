package tech.ydb.query;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledExecutorService;

import javax.annotation.WillNotClose;

import io.grpc.ExperimentalApi;

import tech.ydb.core.Result;
import tech.ydb.core.grpc.GrpcTransport;
import tech.ydb.query.impl.QueryClientImpl;

/**
 *
 * @author Aleksandr Gorshenin
 */
@ExperimentalApi("QueryService is experimental and API may change without notice")
public interface QueryClient extends AutoCloseable {
    static Builder newClient(@WillNotClose GrpcTransport transport) {
        return QueryClientImpl.newClient(transport);
    }

    /**
     * Return a future with {@link QuerySession} for further work. The session will be taken from the session pool if
     * it has any idle session or will be created on the fly
     * @param duration timeout of operation completion waiting
     * @return Return a new CompletableFuture that is completed with successful
     * Result when session created, and fail Result otherwise
     */
    CompletableFuture<Result<QuerySession>> createSession(Duration duration);

    ScheduledExecutorService getScheduler();

    @Override
    void close();

    interface Builder {
        Builder sessionPoolMinSize(int minSize);
        Builder sessionPoolMaxSize(int maxSize);

        Builder sessionMaxIdleTime(Duration duration);

        QueryClient build();
    }
}
