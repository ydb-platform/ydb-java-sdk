package tech.ydb.core.impl;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
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
    private final EndpointRecord discoveryEndpoint;
    private final ScheduledExecutorService scheduler;
    private final ManagedChannelFactory channelFactory;
    private final AuthCallOptions callOptions;
    private final EndpointPool endpointPool;
    private final GrpcChannelPool channelPool;
    private final YdbDiscovery discovery;

    public YdbTransportImpl(GrpcTransportBuilder builder) {
        BalancingSettings balancingSettings = getBalancingSettings(builder);
        Duration discoveryTimeout = Duration.ofMillis(builder.getDiscoveryTimeoutMillis());

        this.database = Strings.nullToEmpty(builder.getDatabase());
        this.discoveryEndpoint = getDiscoveryEndpoint(builder);

        logger.info("Create YDB transport with endpoint {} and {}", discoveryEndpoint, balancingSettings);

        this.channelFactory = builder.getManagedChannelFactory();
        this.scheduler = builder.getSchedulerFactory().get();
        this.callOptions = new AuthCallOptions(scheduler, Collections.singletonList(discoveryEndpoint),
                channelFactory, builder);
        this.channelPool = new GrpcChannelPool(channelFactory, scheduler);
        this.endpointPool = new EndpointPool(balancingSettings);
        this.discovery = new YdbDiscovery(new DiscoveryHandler(), scheduler, database, discoveryTimeout);
    }

    public void start(GrpcTransportBuilder.InitMode mode) {
        if (mode == GrpcTransportBuilder.InitMode.ASYNC_FALLBACK) {
            endpointPool.setNewState(null, Collections.singletonList(discoveryEndpoint));
        }

        discovery.start();

        if (mode == GrpcTransportBuilder.InitMode.SYNC) {
            discovery.waitReady(-1);
        }
    }

    @Override
    public String toString() {
        return "YdbTransport{endpoint=" + discoveryEndpoint + ", database=" + database + "}";
    }

    @Deprecated
    public void startAsync(Runnable readyWatcher) {
        endpointPool.setNewState(null, Collections.singletonList(discoveryEndpoint));
        discovery.start();
        if (readyWatcher != null) {
            scheduler.execute(() -> {
                discovery.waitReady(-1);
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

        if (endpointURI.getPort() < 0) {
            throw new IllegalArgumentException("Can't create discovery rpc, port is not specified for "
                    + "endpoint " + builder.getEndpoint());
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
        EndpointRecord endpoint = endpointPool.getEndpoint(channelPool.getReadyEndpoints(), settings);
        if (endpoint == null) {
            long timeout = -1;
            if (settings.getDeadlineAfter() != 0) {
                timeout = settings.getDeadlineAfter() - System.nanoTime();
            }
            discovery.waitReady(timeout);
            endpoint = endpointPool.getEndpoint(Collections.emptySet(), settings);
        }
        return channelPool.getChannel(endpoint);
    }

    @Override
    protected void pessimizeEndpoint(EndpointRecord endpoint, String reason) {
        endpointPool.pessimizeEndpoint(endpoint, reason);
    }

    private class DiscoveryHandler implements YdbDiscovery.Handler {
        @Override
        public Instant instant() {
            return Instant.now();
        }

        @Override
        public boolean needToForceDiscovery() {
            return endpointPool.needToRunDiscovery();
        }

        @Override
        public CompletableFuture<Boolean> handleEndpoints(List<EndpointRecord> endpoints, String selfLocation) {
            List<EndpointRecord> removed = endpointPool.setNewState(selfLocation, endpoints);
            return channelPool.removeChannels(removed);
        }

        @Override
        public GrpcTransport createDiscoveryTransport() {
            return new FixedCallOptionsTransport(scheduler, callOptions, database, discoveryEndpoint, channelFactory);
        }
    }
}
