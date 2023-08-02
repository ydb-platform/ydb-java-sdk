package tech.ydb.query;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;

import javax.annotation.WillNotClose;

import tech.ydb.core.Result;
import tech.ydb.core.grpc.GrpcTransport;
import tech.ydb.query.impl.QueryClientImpl;

/**
 *
 * @author Aleksandr Gorshenin
 */
public interface QueryClient extends AutoCloseable {
    static Builder newClient(@WillNotClose GrpcTransport transport) {
        return QueryClientImpl.newClient(transport);
    }

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
