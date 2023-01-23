package tech.ydb.core.impl.auth;

import java.util.concurrent.ExecutorService;

import tech.ydb.core.grpc.GrpcTransport;
import tech.ydb.core.impl.AnonimousTransport;
import tech.ydb.core.impl.BaseGrpcTrasnsport;
import tech.ydb.core.impl.pool.EndpointRecord;
import tech.ydb.core.impl.pool.ManagedChannelFactory;

/**
 *
 * @author Aleksandr Gorshenin
 */
public class GrpcAuthRpc {
    private final EndpointRecord endpoint;
    private final BaseGrpcTrasnsport parent;
    private final ManagedChannelFactory channelFactory;

    public GrpcAuthRpc(EndpointRecord endpoint, BaseGrpcTrasnsport parent, ManagedChannelFactory channelFactory) {
        this.endpoint = endpoint;
        this.parent = parent;
        this.channelFactory = channelFactory;
    }

    public ExecutorService getExecutor() {
        return parent.scheduler();
    }

    public String getEndpoint() {
        return endpoint.toString();
    }

    public String getDatabase() {
        return parent.getDatabase();
    }

    public GrpcTransport createTransport() {
        // For auth provider we use transport without auth (with default CallOptions)
        return new AnonimousTransport(
                parent.scheduler(),
                parent.getDatabase(),
                endpoint,
                channelFactory
        );
    }

}
