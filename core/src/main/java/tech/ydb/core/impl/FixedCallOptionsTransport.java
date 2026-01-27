package tech.ydb.core.impl;

import java.util.concurrent.ScheduledExecutorService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tech.ydb.core.grpc.GrpcRequestSettings;
import tech.ydb.core.impl.auth.AuthCallOptions;
import tech.ydb.core.impl.pool.EndpointRecord;
import tech.ydb.core.impl.pool.GrpcChannel;
import tech.ydb.core.impl.pool.ManagedChannelFactory;

/**
 *
 * @author Aleksandr Gorshenin
 */
public class FixedCallOptionsTransport extends BaseGrpcTransport {
    private static final Logger logger = LoggerFactory.getLogger(FixedCallOptionsTransport.class);

    private final ScheduledExecutorService scheduler;
    private final AuthCallOptions callOptions;
    private final String database;
    private final GrpcChannel channel;

    public FixedCallOptionsTransport(
            ScheduledExecutorService scheduler,
            AuthCallOptions callOptions,
            String database,
            EndpointRecord endpoint,
            ManagedChannelFactory channelFactory) {
        this.scheduler = scheduler;
        this.callOptions = callOptions;
        this.database = database;
        this.channel = new GrpcChannel(endpoint, channelFactory);
    }

    @Override
    public ScheduledExecutorService getScheduler() {
        return scheduler;
    }

    @Override
    public String getDatabase() {
        return database;
    }

    @Override
    protected void shutdown() {
        channel.shutdown();
    }

    @Override
    public AuthCallOptions getAuthCallOptions() {
        return callOptions;
    }

    @Override
    protected GrpcChannel getChannel(GrpcRequestSettings settings) {
        return channel;
    }
}
