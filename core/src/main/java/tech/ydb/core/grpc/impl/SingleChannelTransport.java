package tech.ydb.core.grpc.impl;

import io.grpc.Channel;
import io.grpc.Status;
import java.util.concurrent.Executor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.ydb.core.auth.AuthProvider;
import tech.ydb.core.grpc.GrpcRequestSettings;

/**
 *
 * @author Aleksandr Gorshenin
 */
public class SingleChannelTransport extends BaseGrpcTrasnsport {
    private final static Logger logger = LoggerFactory.getLogger(SingleChannelTransport.class);
    private final String database;
    private final GrpcChannel channel;
    
    public SingleChannelTransport(
            AuthProvider authProvider,
            Executor executor,
            long readTimeoutMillis,
            EndpointRecord endpoint,
            ChannelSettings channelSettings) {
        super(authProvider, executor, readTimeoutMillis);
        this.database = channelSettings.getDatabase();
        this.channel = new GrpcChannel(endpoint, channelSettings);
    }

    @Override
    public String getEndpointByNodeId(int nodeId) {
        return channel.getEndpoint();
    }

    @Override
    public String getDatabase() {
        return database;
    }

    @Override
    public void close() {
        channel.shutdown();
    }

    @Override
    protected CheckableChannel getChannel(GrpcRequestSettings settings) {
        return new CheckableChannel() {
            @Override
            public Channel grpcChannel() { return channel.getGrpcChannel(); }
            @Override
            public String endpoint() { return channel.getEndpoint(); }
            @Override
            public void updateGrpcStatus(Status status) {
                if (!status.isOk()) {
                    logger.warn("grpc error {}[{}] on single channel {}",
                            status.getCode(),
                            status.getDescription(),
                            channel.getEndpoint());
                }
            }
        };
    };
}
