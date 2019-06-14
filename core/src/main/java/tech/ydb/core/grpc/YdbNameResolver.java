package tech.ydb.core.grpc;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

import javax.annotation.Nullable;

import tech.ydb.OperationProtos.Operation;
import tech.ydb.StatusCodesProtos.StatusIds.StatusCode;
import tech.ydb.core.Operations;
import tech.ydb.core.auth.AuthProvider;
import tech.ydb.discovery.DiscoveryProtos.EndpointInfo;
import tech.ydb.discovery.DiscoveryProtos.ListEndpointsRequest;
import tech.ydb.discovery.DiscoveryProtos.ListEndpointsResponse;
import tech.ydb.discovery.DiscoveryProtos.ListEndpointsResult;
import tech.ydb.discovery.v1.DiscoveryServiceGrpc;
import io.grpc.Attributes;
import io.grpc.EquivalentAddressGroup;
import io.grpc.NameResolver;
import io.grpc.Status;
import io.grpc.internal.GrpcUtil;


/**
 * @author Sergey Polovko
 */
final class YdbNameResolver extends NameResolver {

    private static final String SCHEME = "ydb";

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
    private static EquivalentAddressGroup createAddressGroup(EndpointInfo endpoint) throws UnknownHostException {
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
        transport.unaryCall(DiscoveryServiceGrpc.METHOD_LIST_ENDPOINTS, request, consumer, 0);
    }

    static Factory newFactory(AuthProvider authProvider) {
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
                return new YdbNameResolver(targetUri.getHost(), port, targetUri.getPath(), authProvider);
            }

            @Override
            public String getDefaultScheme() {
                return SCHEME;
            }
        };
    }
}
