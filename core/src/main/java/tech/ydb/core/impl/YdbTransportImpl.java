package tech.ydb.core.impl;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.List;
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
import tech.ydb.core.impl.discovery.GrpcDiscoveryRpc;
import tech.ydb.core.impl.discovery.PeriodicDiscoveryTask;
import tech.ydb.core.impl.pool.EndpointPool;
import tech.ydb.core.impl.pool.EndpointRecord;
import tech.ydb.core.impl.pool.GrpcChannel;
import tech.ydb.core.impl.pool.GrpcChannelPool;
import tech.ydb.core.impl.pool.ManagedChannelFactory;
import tech.ydb.discovery.DiscoveryProtos;

/**
 * @author Nikolay Perfilov
 */
public class YdbTransportImpl extends BaseGrpcTrasnsport {
    static final int DEFAULT_PORT = 2135;

    private static final Logger logger = LoggerFactory.getLogger(YdbTransportImpl.class);

    private final GrpcDiscoveryRpc discoveryRpc;
    private final CallOptionsFactory callOptionsFactory;
    private final String database;
    private final CallOptions callOptions;
    private final EndpointPool endpointPool;
    private final GrpcChannelPool channelPool;
    private final PeriodicDiscoveryTask periodicDiscoveryTask;
    private final ScheduledExecutorService scheduler;

    public YdbTransportImpl(GrpcTransportBuilder builder) {
        ManagedChannelFactory channelFactory = ManagedChannelFactory.fromBuilder(builder);
        BalancingSettings balancingSettings = getBalancingSettings(builder);
        EndpointRecord discoveryEndpoint = getDiscoverytEndpoint(builder);

        logger.info("creating YDB transport with {}", balancingSettings);

        this.database = Strings.nullToEmpty(builder.getDatabase());
        this.discoveryRpc = new GrpcDiscoveryRpc(this, discoveryEndpoint, channelFactory);

        this.callOptionsFactory = new CallOptionsFactory(this,
                Arrays.asList(discoveryEndpoint),
                channelFactory,
                builder.getAuthProvider()
        );
        this.callOptions = callOptionsFactory.createCallOptions(
                builder.getReadTimeoutMillis(), builder.getCallExecutor()
        );

        this.scheduler = YdbSchedulerFactory.createScheduler();
        this.channelPool = new GrpcChannelPool(channelFactory, scheduler);
        this.endpointPool = new EndpointPool(balancingSettings);

        this.periodicDiscoveryTask = new PeriodicDiscoveryTask(scheduler, discoveryRpc, new YdbDiscoveryHandler());
    }

    public void init() {
        periodicDiscoveryTask.start();
    }

    static EndpointRecord getDiscoverytEndpoint(GrpcTransportBuilder builder) {
        URI endpointURI = null;
        try {
            String endpoint = builder.getEndpoint();
            if (endpoint != null) {
                if (endpoint.startsWith("grpc://") || endpoint.startsWith("grpcs://")) {
                    endpointURI = new URI(endpoint);
                } else {
                    endpointURI = new URI(null, endpoint, null, null, null);
                }
            }
            HostAndPort host = builder.getHost();
            if (host != null) {
                endpointURI = new URI(null, null, host.getHost(),
                        host.getPortOrDefault(DEFAULT_PORT), null, null, null);
            }
        } catch (URISyntaxException ex) {
            logger.warn("endpoint parse problem", ex);
        }
        if (endpointURI == null) {
            throw new IllegalArgumentException("Can't create discovery rpc, unreadable "
                    + "endpoint " + builder.getEndpoint() + " and empty host " + builder.getHost());
        }

        return new EndpointRecord(endpointURI.getHost(), endpointURI.getPort(), 0);
    }

    private static BalancingSettings getBalancingSettings(GrpcTransportBuilder builder) {
        BalancingSettings balancingSettings = builder.getBalancingSettings();
        if (balancingSettings != null) {
            return balancingSettings;
        }

        String localDc = builder.getLocalDc();
        if (localDc != null) {
            return BalancingSettings.fromLocation(builder.getLocalDc());
        }

        return BalancingSettings.defaultInstance();
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

        periodicDiscoveryTask.stop();
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
        EndpointRecord endpoint = endpointPool.getEndpoint(settings.getPreferredNodeID());
        return channelPool.getChannel(endpoint);
    }

    @Override
    void updateChannelStatus(GrpcChannel channel, Status status) {
        if (!status.isOk()) {
            endpointPool.pessimizeEndpoint(channel.getEndpoint());
        }
    }

    private class YdbDiscoveryHandler implements PeriodicDiscoveryTask.DiscoveryHandler {
        @Override
        public boolean useMinDiscoveryPeriod() {
            return endpointPool.needToRunDiscovery();
        }

        @Override
        public void handleDiscoveryResult(DiscoveryProtos.ListEndpointsResult result) {
            List<EndpointRecord> removed = endpointPool.setNewState(result);
            channelPool.removeChannels(removed);
        }
    }
}
