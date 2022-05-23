package tech.ydb.core.grpc.impl.grpc;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import com.google.common.net.HostAndPort;
import tech.ydb.core.grpc.ChannelSettings;
import tech.ydb.core.grpc.GrpcTransport;
import io.grpc.Channel;
import io.grpc.ConnectivityState;
import io.grpc.LoadBalancer;
import io.grpc.LoadBalancerProvider;
import io.grpc.LoadBalancerRegistry;
import io.grpc.ManagedChannel;
import io.grpc.netty.NettyChannelBuilder;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.ChannelOption;
import io.netty.util.internal.StringUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Sergey Polovko
 * @author Nikolay Perfilov
 */
public class GrpcTransportImpl extends GrpcTransport {
    private static final Logger logger = LoggerFactory.getLogger(GrpcTransportImpl.class);

    private final ManagedChannel realChannel;
    private final Channel channel;

    public GrpcTransportImpl(GrpcTransport.Builder builder) {
        super(builder);

        ChannelSettings channelSettings = ChannelSettings.fromBuilder(builder);
        this.realChannel = createChannel(builder, channelSettings);
        this.channel = interceptChannel(realChannel, channelSettings);
        init();
    }

    private void init() {
        switch (discoveryMode) {
            case SYNC:
                try {
                    Instant start = Instant.now();
                    tryToConnect().get(WAIT_FOR_CONNECTION_MS, TimeUnit.MILLISECONDS);
                    logger.info("GrpcTransport sync initialization took {} ms",
                            Duration.between(start, Instant.now()).toMillis());
                } catch (TimeoutException ignore) {
                    logger.warn("Couldn't establish YDB transport connection in {} ms", WAIT_FOR_CONNECTION_MS);
                    // Keep going
                    // Use ASYNC discovery mode and tryToConnect() method to add actions in case of connection timeout
                } catch (InterruptedException | ExecutionException e) {
                    logger.error("Exception thrown while establishing YDB transport connection: " + e);
                    throw new RuntimeException("Exception thrown while establishing YDB transport connection", e);
                }
                break;
            case ASYNC:
            default:
                break;
        }
    }

    /**
     * Establish connection for grpc channel(s) if its currently IDLE
     * Returns a future to a first {@link ConnectivityState} that is not IDLE or CONNECTING
     */
    public CompletableFuture<ConnectivityState> tryToConnect() {
        CompletableFuture<ConnectivityState> promise = new CompletableFuture<>();
        ConnectivityState initialState = realChannel.getState(true);
        logger.debug("GrpcTransport channel initial state: {}", initialState);
        if (!TEMPORARY_STATES.contains(initialState)) {
            promise.complete(initialState);
        } else {
            realChannel.notifyWhenStateChanged(
                    initialState,
                    new Runnable() {
                        @Override
                        public void run() {
                            ConnectivityState currState = realChannel.getState(false);
                            logger.debug("GrpcTransport channel new state: {}", currState);
                            if (TEMPORARY_STATES.contains(currState)) {
                                realChannel.notifyWhenStateChanged(currState, this);
                            } else {
                                promise.complete(currState);
                            }
                        }
                    }
            );
        }
        return promise;
    }

    @Override
    protected Channel getChannel() {
        return channel;
    }

    private static ManagedChannel createChannel(GrpcTransport.Builder builder, ChannelSettings channelSettings) {
        String endpoint = builder.getEndpoint();
        String database = builder.getDatabase();
        List<HostAndPort> hosts = builder.getHosts();
        assert endpoint == null || database != null;
        assert (endpoint == null) != (hosts == null);

        final String localDc = builder.getLocalDc();

        String defaultPolicy;

        if (!StringUtil.isNullOrEmpty(localDc)) {
            defaultPolicy = "prefer_nearest";
            registerPreferNearestLB(localDc);
        } else {
            defaultPolicy = "random_choice";
            registerRandomChoiceLB();
        }

        final NettyChannelBuilder channelBuilder;
        if (endpoint != null) {
            channelBuilder = NettyChannelBuilder.forTarget(YdbNameResolver.makeTarget(endpoint, database))
                    .nameResolverFactory(YdbNameResolver.newFactory(
                            builder.getAuthProvider(),
                            builder.getCert(),
                            builder.getUseTls(),
                            builder.getEndpointsDiscoveryPeriod(),
                            builder.getCallExecutor(),
                            builder.getChannelInitializer()))
                    .defaultLoadBalancingPolicy(defaultPolicy);
        } else if (hosts.size() > 1) {
            channelBuilder = NettyChannelBuilder.forTarget(HostsNameResolver.makeTarget(hosts))
                    .nameResolverFactory(HostsNameResolver.newFactory(hosts, builder.getCallExecutor()))
                    .defaultLoadBalancingPolicy(defaultPolicy);
        } else {
            channelBuilder = NettyChannelBuilder.forAddress(
                    hosts.get(0).getHost(),
                    hosts.get(0).getPortOrDefault(DEFAULT_PORT));
        }

        channelSettings.configureSecureConnection(channelBuilder);

        channelBuilder
                .maxInboundMessageSize(64 << 20) // 64 MiB
                .withOption(ChannelOption.ALLOCATOR, ByteBufAllocator.DEFAULT);

        builder.getChannelInitializer().accept(channelBuilder);
        return channelBuilder.build();
    }

    private static void registerPreferNearestLB(String localDc) {
        final LoadBalancerRegistry lbr = LoadBalancerRegistry.getDefaultRegistry();

        lbr.register(new LoadBalancerProvider() {
            @Override
            public boolean isAvailable() {
                return true;
            }

            @Override
            public int getPriority() {
                return 10;
            }

            @Override
            public String getPolicyName() {
                return "prefer_nearest";
            }

            @Override
            public LoadBalancer newLoadBalancer(LoadBalancer.Helper helper) {
                return new PreferNearestLoadBalancer(helper, localDc);
            }
        });
    }

    private static void registerRandomChoiceLB() {
        final LoadBalancerRegistry lbr = LoadBalancerRegistry.getDefaultRegistry();

        lbr.register(new LoadBalancerProvider() {
            @Override
            public boolean isAvailable() {
                return true;
            }

            @Override
            public int getPriority() {
                return 5;
            }

            @Override
            public String getPolicyName() {
                return "random_choice";
            }

            @Override
            public LoadBalancer newLoadBalancer(LoadBalancer.Helper helper) {
                return new RandomChoiceLoadBalancer(helper);
            }
        });
    }

    @Override
    public void close() {
        super.close();
        try {
            boolean closed = realChannel.shutdown()
                    .awaitTermination(WAIT_FOR_CLOSING_MS, TimeUnit.MILLISECONDS);
            if (!closed) {
                logger.warn("closing transport timeout exceeded, terminate");
                closed = realChannel.shutdownNow()
                        .awaitTermination(WAIT_FOR_CLOSING_MS, TimeUnit.MILLISECONDS);
                if (!closed) {
                    logger.warn("closing transport problem");
                }
            }
        } catch (InterruptedException e) {
            logger.error("transport shutdown interrupted", e);
            Thread.currentThread().interrupt();
        }
    }
}
