package tech.ydb.core.impl.auth;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicInteger;

import tech.ydb.core.grpc.GrpcTransport;
import tech.ydb.core.impl.BaseGrpcTransport;
import tech.ydb.core.impl.FixedCallOptionsTransport;
import tech.ydb.core.impl.pool.EndpointRecord;
import tech.ydb.core.impl.pool.ManagedChannelFactory;

/**
 *
 * @author Aleksandr Gorshenin
 */
public class GrpcAuthRpc {
    private final List<EndpointRecord> endpoints;
    private final BaseGrpcTransport parent;
    private final ManagedChannelFactory channelFactory;
    private final AtomicInteger endpointIdx = new AtomicInteger();

    public GrpcAuthRpc(
            List<EndpointRecord> endpoints,
            BaseGrpcTransport parent,
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
        while (true) {
            int cur = endpointIdx.get();
            int next = (cur + 1) % endpoints.size();

            if (endpointIdx.compareAndSet(cur, next)) {
                break;
            }
        }
    }

    public GrpcTransport createTransport() {
        // For auth provider we use transport without auth (with default CallOptions)
        return new FixedCallOptionsTransport(
                parent.getScheduler(),
                new AuthCallOptions(),
                parent.getDatabase(),
                endpoints.get(endpointIdx.get()),
                channelFactory
        );
    }

}
