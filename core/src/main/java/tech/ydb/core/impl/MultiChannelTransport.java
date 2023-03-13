package tech.ydb.core.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ScheduledExecutorService;

import com.google.common.base.Strings;
import com.google.common.net.HostAndPort;
import io.grpc.CallOptions;
import io.grpc.Status;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tech.ydb.core.grpc.BalancingSettings;
import tech.ydb.core.grpc.GrpcRequestSettings;
import tech.ydb.core.grpc.GrpcTransportBuilder;
import tech.ydb.core.impl.auth.CallOptionsFactory;
import tech.ydb.core.impl.pool.EndpointPool;
import tech.ydb.core.impl.pool.EndpointRecord;
import tech.ydb.core.impl.pool.GrpcChannel;
import tech.ydb.core.impl.pool.GrpcChannelPool;
import tech.ydb.core.impl.pool.ManagedChannelFactory;
import tech.ydb.discovery.DiscoveryProtos;

/**
 *
 * @author Aleksandr Gorshenin
 */
@Deprecated
public class MultiChannelTransport extends BaseGrpcTrasnsport {
    private static final Logger logger = LoggerFactory.getLogger(YdbTransportImpl.class);

    private final String database;
    private final CallOptionsFactory callOptionsFactory;
    private final DiscoveryProtos.ListEndpointsResult discoveryResult;
    private final CallOptions callOptions;
    private final EndpointPool endpointPool;
    private final GrpcChannelPool channelPool;
    private final ScheduledExecutorService scheduler;

    public MultiChannelTransport(GrpcTransportBuilder builder, List<HostAndPort> hosts) {
        ManagedChannelFactory channelFactory = ManagedChannelFactory.fromBuilder(builder);

        logger.info("creating multi channle transport with hosts {}", Objects.requireNonNull(hosts));

        this.database = Strings.nullToEmpty(builder.getDatabase());
        this.scheduler = YdbSchedulerFactory.createScheduler();

        List<EndpointRecord> endpoints = new ArrayList<>();
        DiscoveryProtos.ListEndpointsResult.Builder discoveryBuilder = DiscoveryProtos.ListEndpointsResult.newBuilder();
        hosts.forEach(host -> {
            endpoints.add(new EndpointRecord(host.getHost(), host.getPortOrDefault(YdbTransportImpl.DEFAULT_PORT), 0));
            discoveryBuilder.addEndpointsBuilder()
                    .setAddress(host.getHost())
                    .setPort(host.getPortOrDefault(YdbTransportImpl.DEFAULT_PORT))
                    .build();
        });

        this.discoveryResult = discoveryBuilder.build();
        this.callOptionsFactory = new CallOptionsFactory(this,
                endpoints,
                channelFactory,
                builder.getAuthProvider()
        );
        this.callOptions = callOptionsFactory.createCallOptions(
                builder.getReadTimeoutMillis(), builder.getCallExecutor()
        );

        this.channelPool = new GrpcChannelPool(channelFactory, scheduler);
        this.endpointPool = new EndpointPool(BalancingSettings.defaultInstance());
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
    public void close() {
        super.close();

        channelPool.shutdown();
        callOptionsFactory.close();

        YdbSchedulerFactory.shutdownScheduler(scheduler);
    }

    @Override
    public CallOptions getCallOptions() {
        return callOptions;
    }

    @Override
    GrpcChannel getChannel(GrpcRequestSettings settings) {
        EndpointRecord endpoint = endpointPool.getEndpoint(null);
        return channelPool.getChannel(endpoint);
    }

    @Override
    void updateChannelStatus(GrpcChannel channel, Status status) {
        if (!status.isOk()) {
            endpointPool.pessimizeEndpoint(channel.getEndpoint());

            if (endpointPool.needToRunDiscovery()) {
                endpointPool.setNewState(discoveryResult);
            }
        }
    }
}
