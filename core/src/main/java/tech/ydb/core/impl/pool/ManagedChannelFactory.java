package tech.ydb.core.impl.pool;

import io.grpc.ManagedChannel;

import tech.ydb.core.grpc.GrpcTransportBuilder;

/**
 * @author Nikolay Perfilov
 * @author Aleksandr Gorshenin
 */
public interface ManagedChannelFactory {
    interface Builder {
        ManagedChannelFactory buildFactory(GrpcTransportBuilder builder);
    }

    ManagedChannel newManagedChannel(String host, int port, String authority);

    long getConnectTimeoutMs();
}
