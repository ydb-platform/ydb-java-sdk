package tech.ydb.core.impl.auth;

import java.util.List;
import java.util.concurrent.ExecutorService;

import tech.ydb.core.grpc.GrpcTransport;
import tech.ydb.core.impl.BaseGrpcTrasnsport;
import tech.ydb.core.impl.FixedCallOptionsTransport;
import tech.ydb.core.impl.pool.EndpointRecord;
import tech.ydb.core.impl.pool.ManagedChannelFactory;

/**
 *
 * @author Aleksandr Gorshenin
 */
public class GrpcAuthRpc {
    private final List<EndpointRecord> endpoints;
    private final BaseGrpcTrasnsport parent;
    private final ManagedChannelFactory channelFactory;
    private volatile int endpointIdx = 0;

    public GrpcAuthRpc(
            List<EndpointRecord> endpoints,
            BaseGrpcTrasnsport parent,
            ManagedChannelFactory channelFactory) {
        if (endpoints == null || endpoints.isEmpty()) {
            throw new IllegalStateException("Empty endpoints list for auth rpc");
        }
        this.endpoints = endpoints;
        this.parent = parent;
        this.channelFactory = channelFactory;
    }

    public ExecutorService getExecutor() {
        return parent.getScheduler();
    }

    public String getDatabase() {
        return parent.getDatabase();
    }

    public void changeEndpoint() {
        endpointIdx = (endpointIdx + 1) % endpoints.size();
    }

    public GrpcTransport createTransport() {
        // For auth provider we use transport without auth (with default CallOptions)
        return new FixedCallOptionsTransport(
                parent.getScheduler(),
                new AuthCallOptions(),
                parent.getDatabase(),
                endpoints.get(endpointIdx),
                channelFactory
        );
    }

}
