package tech.ydb.table;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;

import javax.annotation.WillClose;
import javax.annotation.WillNotClose;

import tech.ydb.core.Result;
import tech.ydb.core.grpc.GrpcTransport;
import tech.ydb.table.impl.TableClientBuilderImpl;
import tech.ydb.table.rpc.TableRpc;
import tech.ydb.table.rpc.grpc.GrpcTableRpc;
import tech.ydb.table.settings.CreateSessionSettings;
import tech.ydb.table.stats.SessionPoolStats;


/**
 * @author Sergey Polovko
 */
public interface TableClient extends SessionSupplier, AutoCloseable {

    @Deprecated
    static Builder newClient(@WillClose TableRpc tableRpc) {
        return new TableClientBuilderImpl(tableRpc);
    }
    
    /** 
     * Return TableClient builder used passed {@link GrpcTransport}
     * @param transport - instance of grpc transport
     * @return ${@link TableClient.Builder} for TableClient creating
     */
    static Builder newClient(@WillNotClose GrpcTransport transport) {
        return new TableClientBuilderImpl(GrpcTableRpc.useTransport(transport));
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

        Builder keepQueryText(boolean keep);

        Builder sessionPoolSize(int minSize, int maxSize);

        Builder sessionKeepAliveTime(Duration duration);

        Builder sessionMaxIdleTime(Duration duration);

        Builder sessionCreationMaxRetries(int maxRetries);

        TableClient build();
    }
}
