package tech.ydb.core.grpc.impl;

import com.google.common.base.Strings;
import com.google.common.net.HostAndPort;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import tech.ydb.core.Result;
import tech.ydb.core.grpc.BalancingSettings;
import tech.ydb.core.grpc.EndpointInfo;
import tech.ydb.core.grpc.GrpcRequestSettings;
import tech.ydb.core.rpc.StreamControl;
import tech.ydb.core.rpc.StreamObserver;
import tech.ydb.core.utils.Async;
import io.grpc.Channel;
import io.grpc.ConnectivityState;
import io.grpc.MethodDescriptor;
import io.grpc.Status;
import io.netty.util.Timeout;
import io.netty.util.TimerTask;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.EnumSet;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.ydb.core.Issue;
import tech.ydb.core.StatusCode;
import tech.ydb.core.grpc.GrpcTransportBuilder;
import tech.ydb.discovery.DiscoveryProtos;

/**
 * @author Nikolay Perfilov
 */
public class YdbTransportImpl extends BaseGrpcTrasnsport {
    private static final int DEFAULT_PORT = 2135;
    private static final long DISCOVERY_TIMEOUT_SECONDS = 10;

    // Interval between discovery requests when everything is ok
    private static final long DISCOVERY_PERIOD_NORMAL_SECONDS = 60;
    // Interval between discovery requests when pessimization threshold is exceeded
    private static final long DISCOVERY_PERIOD_MIN_SECONDS = 5;
    // Maximum percent of endpoints pessimized by transport errors to start recheck
    private static final long DISCOVERY_PESSIMIZATION_THRESHOLD = 50;
    
    private static final Result<?> SHUTDOWN_RESULT =  Result.fail(tech.ydb.core.Status.of(
            StatusCode.CLIENT_CANCELLED,
            Issue.of("Request was not sent: transport is shutting down", Issue.Severity.ERROR)
    ));

    protected static final EnumSet<ConnectivityState> TEMPORARY_STATES = EnumSet.of(
        ConnectivityState.IDLE,
        ConnectivityState.CONNECTING
    );

    private static final Logger logger = LoggerFactory.getLogger(YdbTransportImpl.class);

    private final GrpcDiscoveryRpc discoveryRpc;
    private final EndpointPool endpointPool;
    private final GrpcChannelPool channelPool;
    private final PeriodicDiscoveryTask periodicDiscoveryTask = new PeriodicDiscoveryTask();
    private final String database;
    private volatile boolean shutdown = false;

    public YdbTransportImpl(GrpcTransportBuilder builder) {
        super(builder.getAuthProvider(), builder.getCallExecutor(), builder.getReadTimeoutMillis());
        this.database = Strings.nullToEmpty(builder.getDatabase());

        ChannelSettings channelSettings = ChannelSettings.fromBuilder(builder);
        BalancingSettings balancingSettings = getBalancingSettings(builder);
        logger.debug("creating YDB transport with {}", balancingSettings);

        this.discoveryRpc = createDiscoveryRpc(builder, channelSettings);

        this.endpointPool = new EndpointPool(this::listEndpoints, balancingSettings);
        this.periodicDiscoveryTask.start();
        
        this.channelPool = new GrpcChannelPool(channelSettings, endpointPool.getRecords());
    }
    
    private CompletableFuture<Result<DiscoveryProtos.ListEndpointsResult>> listEndpoints() {
        GrpcRequestSettings grpcSettings = GrpcRequestSettings.newBuilder()
                .withDeadlineAfter(System.nanoTime() + Duration.ofSeconds(DISCOVERY_TIMEOUT_SECONDS).toNanos())
                .build();

        return discoveryRpc.listEndpoints(database, grpcSettings);
    }
    
    private BalancingSettings getBalancingSettings(GrpcTransportBuilder builder) {
        BalancingSettings balancingSettings = builder.getBalancingSettings();
        if (balancingSettings != null) {
            return balancingSettings;
        }

        String localDc = builder.getLocalDc();
        if (localDc != null) {
            return BalancingSettings.fromLocation(builder.getLocalDc());
        }

        return new BalancingSettings();
    }

    private GrpcDiscoveryRpc createDiscoveryRpc(GrpcTransportBuilder builder, ChannelSettings channelSettings) {
        URI endpointURI = null;
        try {
            if (builder.getEndpoint() != null) {
                endpointURI = new URI(null, builder.getEndpoint(), null, null, null);
            }
            List<HostAndPort> hosts = builder.getHosts();
            if (hosts != null && !hosts.isEmpty()) {
                HostAndPort host = hosts.get(0);
                endpointURI = new URI(null, null, host.getHost(), host.getPortOrDefault(DEFAULT_PORT), null, null, null);
            }
        } catch (URISyntaxException ex) {
            logger.warn("endpoint parse problem", ex);
        }
        if (endpointURI == null) {
            throw new IllegalArgumentException("Can't create discovery rpc, unreadable "
                    + "endpoint " + builder.getEndpoint() + " and empty list of hosts");
        }
        EndpointRecord endpoint = new EndpointRecord(endpointURI.getHost(), endpointURI.getPort(), 0);
        SingleChannelTransport discoveryTransport = new SingleChannelTransport(
                builder.getAuthProvider(),
                builder.getCallExecutor(),
                builder.getReadTimeoutMillis(),
                endpoint,
                channelSettings);
        return new GrpcDiscoveryRpc(discoveryTransport);
    }
    
    @Override
    public String getEndpointByNodeId(int nodeId) {
        return endpointPool.getEndpointByNodeId(nodeId);
    }

    @Override
    public <ReqT, RespT> CompletableFuture<Result<RespT>> unaryCall(
            MethodDescriptor<ReqT, RespT> method,
            GrpcRequestSettings settings,
            ReqT request
    ) {
        if (shutdown) {
            return CompletableFuture.completedFuture(SHUTDOWN_RESULT.map(null));
        }
        return super.unaryCall(method, settings, request);
    }

    @Override
    public <ReqT, RespT> StreamControl serverStreamCall(
            MethodDescriptor<ReqT, RespT> method,
            GrpcRequestSettings settings,
            ReqT request,
            StreamObserver<RespT> observer
    ) {
        if (shutdown) {
            observer.onError(SHUTDOWN_RESULT.toStatus());
            return () -> {};
        }
        return super.serverStreamCall(method, settings, request, observer);
    }

    @Override
    public void close() {
        shutdown = true;
        periodicDiscoveryTask.stop();
        channelPool.shutdown();
    }

    @Override
    public String getDatabase() {
        return database;
    }

    @Override
    protected CheckableChannel getChannel(GrpcRequestSettings settings) {
        return new YdbChannel(settings);
    }

    private class YdbChannel implements CheckableChannel {
        private final GrpcChannel channel;

        public YdbChannel(GrpcRequestSettings settings) {
            EndpointInfo preferredEndpoint = settings.getPreferredEndpoint();
            EndpointRecord endpoint = endpointPool.getEndpoint(
                    preferredEndpoint != null ? preferredEndpoint.getEndpoint() : null);
            this.channel = channelPool.getChannel(endpoint);
        }

        @Override
        public Channel grpcChannel() {
            return channel.getGrpcChannel();
        }

        @Override
        public String endpoint() {
            return channel.getEndpoint();
        }

        @Override
        public void updateGrpcStatus(Status status) {
            if (!status.isOk()) {
                endpointPool.pessimizeEndpoint(channel.getEndpoint());
            }
        }
    }

    /**
     * PERIODIC DISCOVERY TASK
     */
    private final class PeriodicDiscoveryTask implements TimerTask {
        private volatile boolean stopped = false;
        private Timeout scheduledHandle = null;

        void stop() {
            logger.debug("stopping PeriodicDiscoveryTask");
            stopped = true;
            if (scheduledHandle != null) {
                scheduledHandle.cancel();
                scheduledHandle = null;
            }
        }

        void start() {
            CompletableFuture<EndpointPool.EndpointUpdateResultData> firstRunFuture = runDiscovery(true);
            if (firstRunFuture == null) {
                // TODO: Retry?
                throw new RuntimeException("Couldn't perform discovery on GrpcTransport start");
            }
            // Waiting for first discovery result...
            EndpointPool.EndpointUpdateResultData firstRunData = firstRunFuture.join();
            if (!firstRunData.discoveryStatus.isSuccess()) {
                throw new RuntimeException("Couldn't perform discovery on GrpcTransport start with status: "
                        + firstRunData.discoveryStatus);
            }
        }

        @Override
        public void run(Timeout timeout) {
            int pessimizationRatio = endpointPool.getPessimizationRatio();
            if (pessimizationRatio > DISCOVERY_PESSIMIZATION_THRESHOLD) {
                logger.info("launching discovery due to pessimization threshold is exceeded: {} is more than {}",
                        pessimizationRatio, DISCOVERY_PESSIMIZATION_THRESHOLD);
                runDiscovery(false);
            } else if (endpointPool.getTimeSinceLastUpdate().getSeconds() > DISCOVERY_PERIOD_NORMAL_SECONDS) {
                logger.debug("launching discovery in normal mode");
                runDiscovery(false);
            } else {
                scheduleNextDiscovery();
                logger.trace("no need to run discovery yet");
            }
        }

        private CompletableFuture<EndpointPool.EndpointUpdateResultData> runDiscovery(boolean firstRun) {
            if (stopped) {
                return null;
            }

            EndpointPool.EndpointUpdateResult updateResult = endpointPool.updateAsync();
            assert !firstRun || updateResult.discoveryWasPerformed;
            if (!updateResult.discoveryWasPerformed) {
                logger.debug("discovery was not performed: already in progress");
                scheduleNextDiscovery();
                return null;
            }

            logger.debug("discovery {} was requested (firstRun = {}), waiting for result...", updateResult.data, firstRun);
            return updateResult.data.handle((updateResultData, ex) -> {
                if (ex != null) {
                    logger.warn("couldn't perform discovery with exception", ex);
                    scheduleNextDiscovery();
                    return null;
                }

                if (updateResultData.discoveryStatus.isSuccess()) {
                    logger.debug("discovery was successfully performed");
                    if (channelPool != null) {
                        channelPool.removeChannels(updateResultData.removed);
                        logger.debug("channelPool.removeChannels executed successfully");
                    }
                } else {
                    logger.warn("couldn't perform discovery with status: {}", updateResultData.discoveryStatus);
                }
                scheduleNextDiscovery();
                return updateResultData;
            });
        }

        void scheduleNextDiscovery() {
            scheduledHandle = Async.runAfter(this, DISCOVERY_PERIOD_MIN_SECONDS, TimeUnit.SECONDS);
        }
    }
}
