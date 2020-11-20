package tech.ydb.core.grpc;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

import tech.ydb.OperationProtos.Operation;
import tech.ydb.StatusCodesProtos.StatusIds.StatusCode;
import tech.ydb.core.Operations;
import tech.ydb.core.auth.AuthProvider;
import tech.ydb.core.utils.Async;
import tech.ydb.discovery.DiscoveryProtos.EndpointInfo;
import tech.ydb.discovery.DiscoveryProtos.ListEndpointsRequest;
import tech.ydb.discovery.DiscoveryProtos.ListEndpointsResponse;
import tech.ydb.discovery.DiscoveryProtos.ListEndpointsResult;
import tech.ydb.discovery.v1.DiscoveryServiceGrpc;
import io.grpc.Attributes;
import io.grpc.EquivalentAddressGroup;
import io.grpc.NameResolver;
import io.grpc.Status;
import io.grpc.SynchronizationContext;
import io.grpc.internal.GrpcUtil;
import io.grpc.netty.NettyChannelBuilder;
import io.netty.util.Timeout;


/**
 * @author Sergey Polovko
 * @author Evgeniy Pshenitsin
 */
final class YdbNameResolver extends NameResolver {

    private static final Logger logger = Logger.getLogger(YdbNameResolver.class.getName());

    private static final String SCHEME = "ydb";
    public static final Attributes.Key<String> LOCATION_ATTR = Attributes.Key.create("loc");

    private final String database;
    private final String authority;
    private final GrpcTransport transport;

    private Listener listener;
    private volatile boolean shutdown = false;

    private final SynchronizationContext synchronizationContext;
    private Timeout scheduledHandle = null;
    private final Duration discoveryPeriod;

    private YdbNameResolver(
            String database,
            String authority,
            GrpcTransport transport,
            SynchronizationContext synchronizationContext,
            Duration discoveryPeriod) {
        this.database = database;
        this.authority = authority;
        this.transport = transport;
        this.synchronizationContext = synchronizationContext;
        if (discoveryPeriod.toMillis() < 5000) {
            discoveryPeriod = Duration.ofSeconds(5);
        }
        this.discoveryPeriod = discoveryPeriod;
    }

    static String makeTarget(String endpoint, String database) {
        StringBuilder sb = new StringBuilder();
        sb.append(SCHEME).append("://").append(endpoint);
        if (!database.startsWith("/")) {
            sb.append('/');
        }
        sb.append(database);
        return sb.toString();
    }

    @Override
    public String getServiceAuthority() {
        return authority;
    }

    @Override
    public void start(Listener listener) {
        this.listener = listener;
        resolve();
    }

    @Override
    public void refresh() {
        cancelScheduledRefresh();
        resolve();
    }

    @Override
    public void shutdown() {
        cancelScheduledRefresh();
        transport.close();
        shutdown = true;
    }

    private void cancelScheduledRefresh() {
        if (scheduledHandle != null) {
            scheduledHandle.cancel();
            scheduledHandle = null;
        }
    }

    private void resolve() {
        listEndpoints((response, status) -> {
            try {
                if (!status.isOk()) {
                    listener.onError(status.augmentDescription("unable to resolve database " + database + ", network issue"));
                    return;
                }

                logger.log(Level.FINE, String.format("response METHOD_LIST_ENDPOINTS - %s", response.toString()));

                Operation operation = response.getOperation();
                if (!operation.getReady()) {
                    // TODO: wait deferred operations
                    String msg = "unable to resolve database " + database +
                            ", got not ready operation, id: " + operation.getId() +
                            ", status: " + operation.getStatus();
                    listener.onError(Status.INTERNAL.withDescription(msg));
                    return;
                }

                if (operation.getStatus() != StatusCode.SUCCESS) {
                    String msg = "unable to resolve database " + database +
                            ", got non SUCCESS response, id: " + operation.getId() +
                            ", status: " + operation.getStatus();
                    listener.onError(Status.INTERNAL.withDescription(msg));
                    return;
                }

                ListEndpointsResult result = Operations.unpackResult(operation, ListEndpointsResult.class);

                int endpointsCount = result.getEndpointsCount();
                if (endpointsCount == 0) {
                    String msg = "unable to resolve database " + database + ", got empty list of endpoints";
                    listener.onError(Status.UNAVAILABLE.withDescription(msg));
                    return;
                }

                logger.fine(String.format("ListEndpointsResult - %s)",
                        result.getEndpointsList().stream()
                                .map(e -> String.format("{addr - %s, loc - %s}", e.getAddress(), e.getLocation()))
                                .collect(Collectors.joining(","))));

                List<EquivalentAddressGroup> groups = new ArrayList<>(endpointsCount);
                for (EndpointInfo e : result.getEndpointsList()) {
                    try {
                        groups.add(createAddressGroup(e));
                    } catch (UnknownHostException x) {
                        String msg = "unable to resolve database " + database +
                                ", got unknown hostname: " + e.getAddress();
                        listener.onError(Status.UNAVAILABLE.withDescription(msg).withCause(x));
                        return;
                    }
                }

                listener.onAddresses(groups, Attributes.EMPTY);
            } catch (Throwable t) {
                String msg = "unable to resolve database " + database + ", unhandled exception";
                listener.onError(Status.UNAVAILABLE.withDescription(msg).withCause(t));
            } finally {
                scheduleNextDiscovery();
            }
        });
    }

    private void scheduleNextDiscovery() {
        synchronizationContext.execute(() -> {
            cancelScheduledRefresh();
            scheduledHandle = Async.runAfter((timeout) -> {
                if (!timeout.isCancelled()) {
                    synchronizationContext.execute(this::refresh);
                }
            }, randomDelay(discoveryPeriod.toMillis()), TimeUnit.MILLISECONDS);
        });
    }

    private long randomDelay(long delayMillis) {
        long half = delayMillis / 2;
        return half + ThreadLocalRandom.current().nextLong(half);
    }

    // TODO: resolve name asynchronously
    private static EquivalentAddressGroup createAddressGroup(EndpointInfo endpoint) throws UnknownHostException {
        InetAddress[] addresses = InetAddress.getAllByName(endpoint.getAddress());
        if (addresses.length == 1) {
            return new EquivalentAddressGroup(new InetSocketAddress(addresses[0], endpoint.getPort()),
                    Attributes.newBuilder().set(LOCATION_ATTR, endpoint.getLocation()).build());
        }

        List<SocketAddress> socketAddresses = new ArrayList<>(addresses.length);
        for (InetAddress address : addresses) {
            socketAddresses.add(new InetSocketAddress(address, endpoint.getPort()));
        }
        return new EquivalentAddressGroup(socketAddresses,
                Attributes.newBuilder().set(LOCATION_ATTR, endpoint.getLocation()).build());
    }

    private void listEndpoints(BiConsumer<ListEndpointsResponse, Status> consumer) {
        if (shutdown) {
            return;
        }

        ListEndpointsRequest request = ListEndpointsRequest.newBuilder()
                .setDatabase(database)
                .build();
        transport.unaryCall(DiscoveryServiceGrpc.getListEndpointsMethod(), request, consumer, System.nanoTime() + discoveryPeriod.dividedBy(2).toNanos());
    }

    static Factory newFactory(
            AuthProvider authProvider,
            @Nullable byte[] cert,
            boolean useTLS,
            Duration discoveryPeriod,
            Executor executor,
            Consumer<NettyChannelBuilder> channelCustomizer) {
        return new Factory() {
            @Nullable
            @Override
            public NameResolver newNameResolver(URI targetUri, Helper helper) {
                if (!SCHEME.equals(targetUri.getScheme())) {
                    return null;
                }
                int port = targetUri.getPort();
                if (port == -1) {
                    port = GrpcTransport.DEFAULT_PORT;
                }

                String host = targetUri.getHost();
                String database = targetUri.getPath();
                String authority = GrpcUtil.authorityFromHostAndPort(host, port);
                GrpcTransport.Builder transportBuilder = GrpcTransport.forHost(host, port)
                        .withAuthProvider(authProvider)
                        .withCallExecutor(executor)
                        .withDataBase(database)
                        .withChannelInitializer(channelCustomizer);
                if (useTLS) {
                    if (cert != null) {
                        transportBuilder.withSecureConnection(cert);
                    } else {
                        transportBuilder.withSecureConnection();
                    }
                }
                GrpcTransport transport = transportBuilder.build();
                return new YdbNameResolver(database, authority, transport, helper.getSynchronizationContext(), discoveryPeriod);
            }

            @Override
            public String getDefaultScheme() {
                return SCHEME;
            }
        };
    }
}
