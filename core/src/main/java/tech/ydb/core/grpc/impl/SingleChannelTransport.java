package tech.ydb.core.grpc.impl;

import java.util.concurrent.ScheduledExecutorService;

import io.grpc.CallOptions;
import io.grpc.Channel;
import io.grpc.Status;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tech.ydb.core.grpc.GrpcRequestSettings;

/**
 *
 * @author Aleksandr Gorshenin
 */
public class SingleChannelTransport extends BaseGrpcTrasnsport {
    private static final Logger logger = LoggerFactory.getLogger(SingleChannelTransport.class);

    private final CallOptions callOptions;
    private final ScheduledExecutorService scheduler;
    private final String database;
    private final GrpcChannel channel;

    public SingleChannelTransport(
            CallOptions callOptions,
            ScheduledExecutorService scheduler,
            long readTimeoutMillis,
            String database,
            EndpointRecord endpoint,
            ChannelFactory channelFactory) {
        super(readTimeoutMillis);
        this.callOptions = callOptions;
        this.scheduler = scheduler;
        this.database = database;
        this.channel = new GrpcChannel(endpoint, channelFactory, true);
    }

    @Override
    public ScheduledExecutorService scheduler() {
        return scheduler;
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
    protected CallOptions getCallOptions() {
        return callOptions;
    }

    @Override
    protected CheckableChannel getChannel(GrpcRequestSettings settings) {
        return new CheckableChannel() {
            @Override
            public Channel grpcChannel() {
                return channel.getReadyChannel();
            }

            @Override
            public String endpoint() {
                return channel.getEndpoint();
            }

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
