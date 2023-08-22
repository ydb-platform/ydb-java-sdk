package tech.ydb.table;

import java.time.Duration;

import javax.annotation.WillNotClose;

import tech.ydb.core.grpc.GrpcTransport;
import tech.ydb.table.impl.PooledTableClient;
import tech.ydb.table.rpc.grpc.GrpcTableRpc;


/**
 * @author Sergey Polovko
 * @author Aleksandr Gorshenin
 *
 * TableClient is a main point for accepting and releasing sessions
 * It has factory method {@link newClient(GrpcTransport)} which
 * return instance of default implementation {@link PooledTableClient}. This
 * implementation contains session pool with fixed sizes. This is recommended way
 * to create implementation of SessionSupplier.
 * If you want to use implementation without session pool, you may use
 * {@link tech.ydb.table.impl.SimpleTableClient}
 */
public interface TableClient extends SessionSupplier, AutoCloseable {

    /**
     * Return TableClient builder used passed {@link GrpcTransport}
     * @param transport instance of grpc transport
     * @return {@link TableClient.Builder} for TableClient creating
     */
    static Builder newClient(@WillNotClose GrpcTransport transport) {
        return PooledTableClient.newClient(GrpcTableRpc.useTransport(transport));
    }

    SessionPoolStats sessionPoolStats();

    @Override
    void close();

    interface Builder {

        Builder keepQueryText(boolean keep);

        Builder sessionPoolSize(int minSize, int maxSize);

        Builder sessionKeepAliveTime(Duration duration);

        Builder sessionMaxIdleTime(Duration duration);

        TableClient build();
    }
}