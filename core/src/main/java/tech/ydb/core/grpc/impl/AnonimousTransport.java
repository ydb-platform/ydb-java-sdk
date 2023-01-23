package tech.ydb.core.grpc.impl;

import java.util.concurrent.ScheduledExecutorService;

import io.grpc.CallOptions;
import io.grpc.Status;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tech.ydb.core.grpc.GrpcRequestSettings;

/**
 *
 * @author Aleksandr Gorshenin
 */
public class AnonimousTransport extends BaseGrpcTrasnsport {
    private static final Logger logger = LoggerFactory.getLogger(AnonimousTransport.class);

    private final ScheduledExecutorService scheduler;
    private final String database;
    private final GrpcChannel channel;

    public AnonimousTransport(
            ScheduledExecutorService scheduler,
            String database,
            EndpointRecord endpoint,
            ManagedChannelFactory channelFactory) {
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
    CallOptions getCallOptions() {
        return CallOptions.DEFAULT;
    }

    @Override
    GrpcChannel getChannel(GrpcRequestSettings settings) {
        return channel;
    }

    @Override
    void updateChannelStatus(GrpcChannel channel, Status status) {
        if (!status.isOk()) {
            logger.warn("grpc error {}[{}] on auth channel {}",
                    status.getCode(),
                    status.getDescription(),
                    channel.getEndpoint());
        }
    }

}
