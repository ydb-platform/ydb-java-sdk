package tech.ydb.core.impl;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.ScheduledExecutorService;
import java.util.stream.Collectors;

import com.google.common.base.Strings;
import com.google.common.net.HostAndPort;
import io.grpc.Status;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tech.ydb.core.grpc.BalancingSettings;
import tech.ydb.core.grpc.GrpcRequestSettings;
import tech.ydb.core.grpc.GrpcTransportBuilder;
import tech.ydb.core.impl.auth.AuthCallOptions;
import tech.ydb.core.impl.pool.EndpointPool;
import tech.ydb.core.impl.pool.EndpointRecord;
import tech.ydb.core.impl.pool.GrpcChannel;
import tech.ydb.core.impl.pool.GrpcChannelPool;
import tech.ydb.core.impl.pool.ManagedChannelFactory;

/**
 *
 * @author Aleksandr Gorshenin
 */
@Deprecated
public class MultiChannelTransport extends BaseGrpcTransport {
    private static final Logger logger = LoggerFactory.getLogger(YdbTransportImpl.class);

    private final String database;
    private final AuthCallOptions callOptions;
    private final List<EndpointRecord> endpoints;
    private final EndpointPool endpointPool;
    private final GrpcChannelPool channelPool;
    private final ScheduledExecutorService scheduler;

    public MultiChannelTransport(GrpcTransportBuilder builder, List<HostAndPort> hosts) {
        ManagedChannelFactory channelFactory = builder.getManagedChannelFactory();

        logger.info("creating multi channel transport with hosts {}", Objects.requireNonNull(hosts));

        this.database = Strings.nullToEmpty(builder.getDatabase());
        this.scheduler = builder.getSchedulerFactory().get();

        this.endpoints = hosts.stream()
                .map(host -> new EndpointRecord(host.getHost(), host.getPortOrDefault(YdbTransportImpl.DEFAULT_PORT)))
                .collect(Collectors.toList());

        this.callOptions = new AuthCallOptions(scheduler, endpoints, channelFactory, builder);
        this.channelPool = new GrpcChannelPool(channelFactory, scheduler);
        this.endpointPool = new EndpointPool(BalancingSettings.defaultInstance());
        this.endpointPool.setNewState(null, endpoints);
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
        channelPool.shutdown();
        callOptions.close();
        YdbSchedulerFactory.shutdownScheduler(scheduler);
    }

    @Override
    public AuthCallOptions getAuthCallOptions() {
        return callOptions;
    }

    @Override
    protected GrpcChannel getChannel(GrpcRequestSettings settings) {
        EndpointRecord endpoint = endpointPool.getEndpoint(null);
        return channelPool.getChannel(endpoint);
    }

    @Override
    protected void updateChannelStatus(GrpcChannel channel, Status status) {
        if (!status.isOk()) {
            endpointPool.pessimizeEndpoint(channel.getEndpoint());

            if (endpointPool.needToRunDiscovery()) {
                endpointPool.setNewState(null, endpoints);
            }
        }
    }
}
