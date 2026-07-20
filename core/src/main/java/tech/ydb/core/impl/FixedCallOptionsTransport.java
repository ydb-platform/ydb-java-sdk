package tech.ydb.core.impl;

import java.util.concurrent.ScheduledExecutorService;

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
    private final ScheduledExecutorService scheduler;
    private final AuthCallOptions callOptions;
    private final String database;
    private final GrpcChannel channel;

    public FixedCallOptionsTransport(
            ScheduledExecutorService scheduler,
            AuthCallOptions callOptions,
            String database,
            String buildInfo,
            EndpointRecord endpoint,
            ManagedChannelFactory channelFactory) {
        super(endpoint, buildInfo);
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
