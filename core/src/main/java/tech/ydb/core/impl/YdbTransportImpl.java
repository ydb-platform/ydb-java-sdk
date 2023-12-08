package tech.ydb.core.impl;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;

import com.google.common.base.Strings;
import com.google.common.net.HostAndPort;
import io.grpc.Status;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tech.ydb.core.grpc.BalancingSettings;
import tech.ydb.core.grpc.GrpcRequestSettings;
import tech.ydb.core.grpc.GrpcTransportBuilder;
import tech.ydb.core.impl.auth.AuthCallOptions;
import tech.ydb.core.impl.discovery.GrpcDiscoveryRpc;
import tech.ydb.core.impl.discovery.PeriodicDiscoveryTask;
import tech.ydb.core.impl.pool.EndpointPool;
import tech.ydb.core.impl.pool.EndpointRecord;
import tech.ydb.core.impl.pool.GrpcChannel;
import tech.ydb.core.impl.pool.GrpcChannelPool;
import tech.ydb.core.impl.pool.ManagedChannelFactory;
import tech.ydb.proto.discovery.DiscoveryProtos;

/**
 * @author Nikolay Perfilov
 */
public class YdbTransportImpl extends BaseGrpcTransport {
    static final int DEFAULT_PORT = 2135;

    private static final Logger logger = LoggerFactory.getLogger(YdbTransportImpl.class);

    private final AuthCallOptions callOptions;
    private final String database;
    private final EndpointPool endpointPool;
    private final GrpcChannelPool channelPool;
    private final PeriodicDiscoveryTask periodicDiscoveryTask;
    private final ScheduledExecutorService scheduler;

    public YdbTransportImpl(GrpcTransportBuilder builder) {
        this.database = Strings.nullToEmpty(builder.getDatabase());

        ManagedChannelFactory channelFactory = ManagedChannelFactory.fromBuilder(builder);
        BalancingSettings balancingSettings = getBalancingSettings(builder);
        EndpointRecord discoveryEndpoint = getDiscoveryEndpoint(builder);

        logger.info("Create YDB transport with endpoint {} and {}", discoveryEndpoint, balancingSettings);

        this.scheduler = builder.getSchedulerFactory().get();
        this.callOptions = new AuthCallOptions(scheduler,
                database,
                Collections.singletonList(discoveryEndpoint),
                channelFactory,
                builder
        );

        GrpcDiscoveryRpc discoveryRpc = new GrpcDiscoveryRpc(this,
                discoveryEndpoint,
                channelFactory,
                callOptions,
                Duration.ofMillis(builder.getDiscoveryTimeoutMillis()));

        this.channelPool = new GrpcChannelPool(channelFactory, scheduler);
        this.endpointPool = new EndpointPool(discoveryEndpoint, balancingSettings);

        this.periodicDiscoveryTask = new PeriodicDiscoveryTask(
                scheduler,
                discoveryRpc,
                new YdbDiscoveryHandler(),
                builder.getConnectTimeoutMillis() + builder.getDiscoveryTimeoutMillis()
        );
    }

    public void init() {
        periodicDiscoveryTask.start();
    }

    public void initAsync(Runnable readyWatcher) {
        periodicDiscoveryTask.startAsync(readyWatcher);
    }

    static EndpointRecord getDiscoveryEndpoint(GrpcTransportBuilder builder) {
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
        if (shutdown) {
            return;
        }
        super.close();

        periodicDiscoveryTask.stop();
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
