package tech.ydb.core.impl;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.Clock;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;

import com.google.common.base.Strings;
import com.google.common.net.HostAndPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tech.ydb.core.grpc.BalancingSettings;
import tech.ydb.core.grpc.GrpcRequestSettings;
import tech.ydb.core.grpc.GrpcTransport;
import tech.ydb.core.grpc.GrpcTransportBuilder;
import tech.ydb.core.impl.auth.AuthCallOptions;
import tech.ydb.core.impl.pool.EndpointPool;
import tech.ydb.core.impl.pool.EndpointRecord;
import tech.ydb.core.impl.pool.GrpcChannel;
import tech.ydb.core.impl.pool.GrpcChannelPool;
import tech.ydb.core.impl.pool.ManagedChannelFactory;

/**
 * @author Nikolay Perfilov
 */
public class YdbTransportImpl extends BaseGrpcTransport {
    static final int DEFAULT_PORT = 2135;

    private static final Logger logger = LoggerFactory.getLogger(YdbTransportImpl.class);

    private final String database;
    private final ScheduledExecutorService scheduler;
    private final AuthCallOptions callOptions;
    private final EndpointPool endpointPool;
    private final GrpcChannelPool channelPool;
    private final YdbDiscoveryImpl discovery;

    public YdbTransportImpl(GrpcTransportBuilder builder) {
        this.database = Strings.nullToEmpty(builder.getDatabase());

        ManagedChannelFactory channelFactory = builder.getManagedChannelFactory();
        BalancingSettings balancingSettings = getBalancingSettings(builder);
        EndpointRecord discoveryEndpoint = getDiscoveryEndpoint(builder);
        Duration discoveryTimeout = Duration.ofMillis(builder.getDiscoveryTimeoutMillis());

        logger.info("Create YDB transport with endpoint {} and {}", discoveryEndpoint, balancingSettings);

        this.scheduler = builder.getSchedulerFactory().get();
        this.callOptions = new AuthCallOptions(scheduler,
                database,
                Collections.singletonList(discoveryEndpoint),
                channelFactory,
                builder
        );

        this.channelPool = new GrpcChannelPool(channelFactory, scheduler);
        this.endpointPool = new EndpointPool(balancingSettings);
        this.discovery = new YdbDiscoveryImpl(channelFactory, discoveryEndpoint, discoveryTimeout);
    }

    public void start(GrpcTransportBuilder.InitMode mode) {
        if (mode == GrpcTransportBuilder.InitMode.ASYNC_FALLBACK) {
            endpointPool.setNewState(null, Collections.singletonList(discovery.discoveryEndpoint));
        }

        discovery.start();

        if (mode == GrpcTransportBuilder.InitMode.SYNC) {
            discovery.waitReady();
        }
    }

    @Deprecated
    public void startAsync(Runnable readyWatcher) {
        discovery.start();
        if (readyWatcher != null) {
            scheduler.execute(() -> {
                discovery.waitReady();
                readyWatcher.run();
            });
        }
    }

    @Override
    protected void shutdown() {
        discovery.stop();
        channelPool.shutdown();
        callOptions.close();

        YdbSchedulerFactory.shutdownScheduler(scheduler);
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

        return new EndpointRecord(endpointURI.getHost(), endpointURI.getPort());
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
    public AuthCallOptions getAuthCallOptions() {
        return callOptions;
    }

    @Override
    protected GrpcChannel getChannel(GrpcRequestSettings settings) {
        EndpointRecord endpoint = endpointPool.getEndpoint(settings.getPreferredNodeID());
        while (endpoint == null) {
            discovery.waitReady();
            endpoint = endpointPool.getEndpoint(settings.getPreferredNodeID());
        }
        return channelPool.getChannel(endpoint);
    }

    @Override
    protected void updateChannelStatus(GrpcChannel channel, io.grpc.Status status) {
        // Usally CANCELLED is received when ClientCall is canceled on client side
        if (!status.isOk() && status.getCode() != io.grpc.Status.Code.CANCELLED) {
            endpointPool.pessimizeEndpoint(channel.getEndpoint());
        }
    }

    private class YdbDiscoveryImpl extends YdbDiscovery {
        private final ManagedChannelFactory channelFactory;
        private final EndpointRecord discoveryEndpoint;

        YdbDiscoveryImpl(ManagedChannelFactory channelFactory, EndpointRecord endpoint, Duration timeout) {
            super(Clock.systemUTC(), scheduler, database, timeout);
            this.channelFactory = channelFactory;
            this.discoveryEndpoint = endpoint;
        }

        @Override
        protected boolean forceDiscovery() {
            return endpointPool.needToRunDiscovery();
        }

        @Override
        protected void handleEndpoints(List<EndpointRecord> endpoints, String selfLocation) {
            List<EndpointRecord> removed = endpointPool.setNewState(selfLocation, endpoints);
            channelPool.removeChannels(removed);
        }

        @Override
        protected GrpcTransport createDiscoveryTransport() {
            return new FixedCallOptionsTransport(scheduler, callOptions, database, discoveryEndpoint, channelFactory);
        }
    }
}
