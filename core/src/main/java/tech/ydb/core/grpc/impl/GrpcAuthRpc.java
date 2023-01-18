package tech.ydb.core.grpc.impl;

import io.grpc.CallOptions;

import tech.ydb.core.grpc.GrpcTransport;

/**
 *
 * @author Aleksandr Gorshenin
 */
public class GrpcAuthRpc {
    private final EndpointRecord endpoint;
    private final BaseGrpcTrasnsport parent;
    private final ChannelFactory channelFactory;

    public GrpcAuthRpc(EndpointRecord endpoint, BaseGrpcTrasnsport parent, ChannelFactory channelFactory) {
        this.endpoint = endpoint;
        this.parent = parent;
        this.channelFactory = channelFactory;
    }

    public String getEndpoint() {
        return endpoint.toString();
    }

    public String getDatabase() {
        return parent.getDatabase();
    }

    public GrpcTransport createTransport() {
        // For auth provider we use transport without auth (with default CallOptions)
        return new SingleChannelTransport(
                CallOptions.DEFAULT,
                parent.scheduler(),
                parent.getDefaultReadTimeoutMillis(),
                parent.getDatabase(),
                endpoint,
                channelFactory
        );
    }

}
