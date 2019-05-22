package tech.ydb.table;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import javax.annotation.WillClose;

import tech.ydb.core.Result;
import tech.ydb.core.Status;
import tech.ydb.table.impl.TableClientBuilderImpl;
import tech.ydb.table.rpc.TableRpc;
import tech.ydb.table.settings.CreateSessionSettings;


/**
 * @author Sergey Polovko
 */
public interface TableClient extends AutoCloseable {

    static Builder newClient(@WillClose TableRpc tableRpc) {
        return new TableClientBuilderImpl(tableRpc);
    }

    /**
     * Create new session.
     */
    CompletableFuture<Result<Session>> createSession(CreateSessionSettings settings);

    default CompletableFuture<Result<Session>> createSession() {
        return createSession(new CreateSessionSettings());
    }

    /**
     * Returns session from session pool, if all sessions are occupied new session will be created.
     */
    CompletableFuture<Result<Session>> getOrCreateSession(CreateSessionSettings settings);

    default CompletableFuture<Result<Session>> getOrCreateSession() {
        return getOrCreateSession(new CreateSessionSettings());
    }

    /**
     * Release session back to this client.
     */
    CompletableFuture<Status> releaseSession(Session session);

    @Override
    void close();

    /**
     * BUILDER
     */
    interface Builder {

        Builder queryCacheSize(int size);

        Builder sessionPoolSize(int minSize, int maxSize);

        Builder sessionKeepAliveTime(long time, TimeUnit timeUnit);

        Builder sessionMaxIdleTime(long time, TimeUnit timeUnit);

        Builder sessionCreationMaxRetries(int maxRetries);

        TableClient build();
    }
}
