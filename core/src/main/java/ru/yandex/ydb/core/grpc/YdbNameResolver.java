package ru.yandex.ydb.core.grpc;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

import javax.annotation.Nullable;

import io.grpc.Attributes;
import io.grpc.EquivalentAddressGroup;
import io.grpc.NameResolver;
import io.grpc.Status;
import io.grpc.internal.GrpcUtil;

import ru.yandex.ydb.OperationProtos.Operation;
import ru.yandex.ydb.core.Operations;
import ru.yandex.ydb.core.auth.AuthProvider;
import ru.yandex.ydb.discovery.DiscoveryProtos.EndpointInfo;
import ru.yandex.ydb.discovery.DiscoveryProtos.ListEndpointsRequest;
import ru.yandex.ydb.discovery.DiscoveryProtos.ListEndpointsResponse;
import ru.yandex.ydb.discovery.DiscoveryProtos.ListEndpointsResult;
import ru.yandex.ydb.discovery.v1.DiscoveryServiceGrpc;


/**
 * @author Sergey Polovko
 */
public final class YdbNameResolver extends NameResolver {

    public static final String SCHEME = "ydb";
    private static final int DEFAULT_PORT = 2135;

    private final String database;
    private final String authority;
    private final GrpcTransport transport;

    private Listener listener;
    private volatile boolean shutdown = false;

    private YdbNameResolver(String hostname, int port, String database, AuthProvider authProvider) {
        this.database = database;
        this.authority = GrpcUtil.authorityFromHostAndPort(hostname, port);
        this.transport = GrpcTransportBuilder.singleHost(hostname, port)
            .withAuthProvider(authProvider)
            .build();
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
        resolve();
    }

    @Override
    public void shutdown() {
        shutdown = true;
    }

    private void resolve() {
        listEndpoints((response, status) -> {
            if (!status.isOk()) {
                listener.onError(status.augmentDescription("unable to resolve database " + database + ", network issue"));
                return;
            }

            try {
                Operation operation = response.getOperation();
                if (!operation.getReady()) {
                    // TODO: wait deferred operations
                    String msg = "unable to resolve database " + database +
                        ", got not ready operation, id: " + operation.getId() +
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

                List<EquivalentAddressGroup> groups = new ArrayList<>(endpointsCount);
                for (int i = 0; i < endpointsCount; i++) {
                    EndpointInfo e = result.getEndpoints(i);
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
            }
        });
    }

    // TODO: resolve name asynchronously
    private EquivalentAddressGroup createAddressGroup(EndpointInfo endpoint) throws UnknownHostException {
        InetAddress[] addresses = InetAddress.getAllByName(endpoint.getAddress());
        if (addresses.length == 1) {
            return new EquivalentAddressGroup(new InetSocketAddress(addresses[0], endpoint.getPort()));
        }

        List<SocketAddress> socketAddresses = new ArrayList<>(addresses.length);
        for (InetAddress address : addresses) {
            socketAddresses.add(new InetSocketAddress(address, endpoint.getPort()));
        }
        return new EquivalentAddressGroup(socketAddresses);
    }

    private void listEndpoints(BiConsumer<ListEndpointsResponse, Status> consumer) {
        if (shutdown) {
            return;
        }

        ListEndpointsRequest request = ListEndpointsRequest.newBuilder()
            .setDatabase(database)
            .build();
        transport.unaryCall(DiscoveryServiceGrpc.METHOD_LIST_ENDPOINTS, request, consumer);
    }

    public static Factory newFactory(AuthProvider authProvider) {
        return new Factory() {
            @Nullable
            @Override
            public NameResolver newNameResolver(URI targetUri, Attributes params) {
                if (!SCHEME.equals(targetUri.getScheme())) {
                    return null;
                }
                int port = targetUri.getPort();
                if (port == -1) {
                    port = DEFAULT_PORT;
                }
                return new YdbNameResolver(targetUri.getHost(), port, targetUri.getPath(), authProvider);
            }

            @Override
            public String getDefaultScheme() {
                return SCHEME;
            }
        };
    }
}
