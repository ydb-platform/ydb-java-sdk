package tech.ydb.core.grpc.impl.ydb;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import com.google.common.net.HostAndPort;
import tech.ydb.core.grpc.BalancingSettings;
import tech.ydb.core.grpc.ChannelSettings;
import tech.ydb.core.grpc.DiscoveryMode;
import tech.ydb.core.grpc.GrpcDiscoveryRpc;
import tech.ydb.core.grpc.GrpcTransport;
import tech.ydb.core.grpc.TransportImplType;
import tech.ydb.core.utils.Async;
import io.grpc.Channel;
import io.netty.util.Timeout;
import io.netty.util.TimerTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Nikolay Perfilov
 */
public class YdbTransportImpl extends GrpcTransport {

    public static final long DISCOVERY_PERIOD_NORMAL_MS = 60000;

    private static final Logger logger = LoggerFactory.getLogger(YdbTransportImpl.class);

    private final GrpcDiscoveryRpc discoveryRpc;
    private final EndpointPool endpointPool;
    private final GrpcChannelPool channelPool;
    private final long discoveryPeriodMillis = DISCOVERY_PERIOD_NORMAL_MS;
    private final PeriodicDiscoveryTask periodicDiscoveryTask = new PeriodicDiscoveryTask();

    public YdbTransportImpl(GrpcTransport.Builder builder) {
        super(builder);

        this.discoveryRpc = createDiscoveryRpc(builder);

        BalancingSettings balancingSettings = builder.getBalancingSettings();
        if (balancingSettings == null) {
            if (builder.getLocalDc() == null) {
                balancingSettings = new BalancingSettings();
            } else {
                balancingSettings = BalancingSettings.fromLocation(builder.getLocalDc());
            }
        }
        logger.debug("creating YDB transport with {}", balancingSettings);

        this.endpointPool = new EndpointPool(
                () -> discoveryRpc.listEndpoints(
                        database,
                        System.nanoTime() + Duration.ofSeconds(DISCOVERY_TIMEOUT_SECONDS).toNanos()
                ),
                balancingSettings
        );

        periodicDiscoveryTask.start();

        channelPool = new GrpcChannelPool(ChannelSettings.fromBuilder(builder), endpointPool.getRecords());
    }

    private GrpcDiscoveryRpc createDiscoveryRpc(Builder builder) {
        String endpoint = builder.getEndpoint();
        if (endpoint == null || builder.getDatabase() == null) {
            throw new IllegalArgumentException(
                    "YDB transport implementation does not support multiple hosts settings (GrpcTransport.forHosts). " +
                            "Use GrpcTransport.forEndpoint instead." +
                            " Or use Grpc transport implementation (TransportImplType.GRPC_TRANSPORT_IMPL)");
        }
        HostAndPort hostAndPort = HostAndPort.fromString(endpoint);
        GrpcTransport.Builder transportBuilder = GrpcTransport
                .forHost(hostAndPort.getHost(), hostAndPort.getPortOrDefault(DEFAULT_PORT))
                .withAuthProvider(builder.getAuthProvider())
                .withCallExecutor(builder.getCallExecutor())
                .withDataBase(builder.getDatabase())
                .withChannelInitializer(builder.getChannelInitializer())
                .withTransportImplType(TransportImplType.GRPC_TRANSPORT_IMPL)
                .withDiscoveryMode(DiscoveryMode.SYNC);

        if (builder.getUseTls()) {
            if (builder.getCert() != null) {
                transportBuilder.withSecureConnection(builder.getCert());
            } else {
                transportBuilder.withSecureConnection();
            }
        }
        GrpcTransport transport = transportBuilder.build();
        return new GrpcDiscoveryRpc(transport);
    }

    @Override
    protected Channel getChannel() {
        return channelPool.getChannel(endpointPool.getEndpoint()).channel;
    }

    @Override
    public void close() {
        super.close();
        periodicDiscoveryTask.stop();
        channelPool.shutdown(WAIT_FOR_CLOSING_MS);
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
            runDiscovery(false);
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

            logger.debug("discovery was requested (firstRun = {}), waiting for result...", firstRun);
            return updateResult.data
                    .thenApply(updateResultData -> {
                        if (updateResultData.discoveryStatus.isSuccess()) {
                            logger.debug("discovery was successfully performed");
                            if (channelPool != null) {
                                channelPool.removeChannels(updateResultData.removed, WAIT_FOR_CLOSING_MS);
                                logger.debug("channelPool.removeChannels executed successfully");
                            }
                        } else {
                            logger.warn("couldn't perform discovery with status: {}", updateResultData.discoveryStatus);
                        }
                        scheduleNextDiscovery();
                        return updateResultData;
                    })
                    .exceptionally(e -> {
                        logger.warn("couldn't perform discovery with exception: {}", e.toString());
                        scheduleNextDiscovery();
                        return null;
                    });
        }

        void scheduleNextDiscovery() {
            logger.debug("scheduling next discovery in {}ms", discoveryPeriodMillis);
            scheduledHandle = Async.runAfter(this, discoveryPeriodMillis, TimeUnit.MILLISECONDS);
        }
    }
}
