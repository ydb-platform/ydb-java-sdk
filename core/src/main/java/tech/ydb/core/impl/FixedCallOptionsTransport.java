package tech.ydb.core.impl;

import java.util.concurrent.ScheduledExecutorService;

import io.grpc.CallOptions;
import io.grpc.Status;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tech.ydb.core.grpc.GrpcRequestSettings;
import tech.ydb.core.impl.pool.EndpointRecord;
import tech.ydb.core.impl.pool.GrpcChannel;
import tech.ydb.core.impl.pool.ManagedChannelFactory;

/**
 *
 * @author Aleksandr Gorshenin
 */
public class FixedCallOptionsTransport extends BaseGrpcTrasnsport {
    private static final Logger logger = LoggerFactory.getLogger(FixedCallOptionsTransport.class);

    private final ScheduledExecutorService scheduler;
    private final CallOptions callOptions;
    private final String database;
    private final GrpcChannel channel;

    public FixedCallOptionsTransport(
            ScheduledExecutorService scheduler,
            CallOptions callOptions,
            String database,
            EndpointRecord endpoint,
            ManagedChannelFactory channelFactory) {
        this.scheduler = scheduler;
        this.callOptions = callOptions;
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
    public CallOptions getCallOptions() {
        return callOptions;
    }

    @Override
    GrpcChannel getChannel(GrpcRequestSettings settings) {
        return channel;
    }

    @Override
    void updateChannelStatus(GrpcChannel channel, Status status) {
        if (!status.isOk()) {
            logger.warn("grpc error {}[{}] on fixed channel {}",
                    status.getCode(),
                    status.getDescription(),
                    channel.getEndpoint());
        }
    }

}
