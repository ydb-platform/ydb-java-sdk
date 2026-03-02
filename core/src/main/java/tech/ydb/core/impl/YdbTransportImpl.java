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
import tech.ydb.core.tracing.Tracer;

/**
 * @author Nikolay Perfilov
 */
public class YdbTransportImpl extends BaseGrpcTransport {
    private static final Logger logger = LoggerFactory.getLogger(YdbTransportImpl.class);

    private final String database;
    private final ScheduledExecutorService scheduler;
    private final ManagedChannelFactory channelFactory;
    private final AuthCallOptions callOptions;
    private final EndpointPool endpointPool;
    private final GrpcChannelPool channelPool;
    private final YdbDiscovery discovery;
    private final Tracer tracer;

    public YdbTransportImpl(GrpcTransportBuilder builder) {
        super(builder);
        BalancingSettings balancingSettings = getBalancingSettings(builder);
        Duration discoveryTimeout = Duration.ofMillis(builder.getDiscoveryTimeoutMillis());

        this.database = Strings.nullToEmpty(builder.getDatabase());
        this.tracer = builder.getTracer();

        logger.info("Create YDB transport with endpoint {} and {}", serverEndpoint, balancingSettings);

        this.channelFactory = builder.getManagedChannelFactory();
        this.scheduler = builder.getSchedulerFactory().get();
        this.callOptions = new AuthCallOptions(
                scheduler,
                Collections.singletonList(serverEndpoint),
                channelFactory,
                builder
        );
        this.channelPool = new GrpcChannelPool(channelFactory, scheduler);
        this.endpointPool = new EndpointPool(balancingSettings);
        this.discovery = new YdbDiscovery(new DiscoveryHandler(), scheduler, database, discoveryTimeout);
    }

    public void start(GrpcTransportBuilder.InitMode mode) {
        if (mode == GrpcTransportBuilder.InitMode.ASYNC_FALLBACK) {
            endpointPool.setNewState(null, Collections.singletonList(serverEndpoint));
        }

        discovery.start();

        if (mode == GrpcTransportBuilder.InitMode.SYNC) {
            discovery.waitReady(-1);
        }
    }

    @Override
    public String toString() {
        return "YdbTransport{endpoint=" + serverEndpoint + ", database=" + database + "}";
    }

    @Deprecated
    public void startAsync(Runnable readyWatcher) {
        endpointPool.setNewState(null, Collections.singletonList(serverEndpoint));
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
    public Tracer getTracer() {
        return tracer;
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
            return new FixedCallOptionsTransport(scheduler, callOptions, database, serverEndpoint, channelFactory);
        }
    }
}
