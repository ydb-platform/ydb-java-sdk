package tech.ydb.table;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;

import javax.annotation.WillClose;

import tech.ydb.core.Result;
import tech.ydb.table.impl.TableClientBuilderImpl;
import tech.ydb.table.rpc.TableRpc;
import tech.ydb.table.settings.CreateSessionSettings;
import tech.ydb.table.stats.SessionPoolStats;


/**
 * @author Sergey Polovko
 */
public interface TableClient extends SessionSupplier, AutoCloseable {

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

    SessionPoolStats getSessionPoolStats();

    @Override
    void close();

    /**
     * BUILDER
     */
    interface Builder {

        Builder queryCacheSize(int size);

        Builder keepQueryText(boolean keep);

        Builder sessionPoolSize(int minSize, int maxSize);

        Builder sessionKeepAliveTime(Duration duration);

        Builder sessionMaxIdleTime(Duration duration);

        Builder sessionCreationMaxRetries(int maxRetries);

        TableClient build();
    }
}
