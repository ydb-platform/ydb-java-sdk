package tech.ydb.core.grpc;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

import com.google.common.net.HostAndPort;
import io.grpc.Attributes;
import io.grpc.EquivalentAddressGroup;
import io.grpc.NameResolver;
import io.grpc.Status;


/**
 * @author Sergey Polovko
 */
final class HostsNameResolver extends NameResolver {

    private static final String SCHEME = "ydb-hosts";

    private final List<HostAndPort> hosts;
    private final String authority;
    private Listener listener;

    private HostsNameResolver(List<HostAndPort> hosts) {
        this.hosts = hosts;
        this.authority = join(hosts);
    }

    static String makeTarget(List<HostAndPort> hosts) {
        return SCHEME + "://" + join(hosts);
    }

    static Factory newFactory(List<HostAndPort> hosts) {
        return new HostsFactory(hosts);
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

    private void resolve() {
        try {
            List<EquivalentAddressGroup> groups = new ArrayList<>(hosts.size());
            for (HostAndPort host : hosts) {
                groups.add(createAddressGroup(host));
            }
            listener.onAddresses(groups, Attributes.EMPTY);
        } catch (UnknownHostException e) {
            listener.onError(Status.INTERNAL.withCause(e).withDescription("cannot resolve hosts"));
        }
    }

    // TODO: resolve name asynchronously
    private static EquivalentAddressGroup createAddressGroup(HostAndPort host) throws UnknownHostException {
        int port = host.getPortOrDefault(GrpcTransport.DEFAULT_PORT);
        InetAddress[] addresses = InetAddress.getAllByName(host.getHost());
        if (addresses.length == 1) {
            return new EquivalentAddressGroup(new InetSocketAddress(addresses[0], port));
        }

        List<SocketAddress> socketAddresses = new ArrayList<>(addresses.length);
        for (InetAddress address : addresses) {
            socketAddresses.add(new InetSocketAddress(address, port));
        }
        return new EquivalentAddressGroup(socketAddresses);
    }

    @Override
    public void shutdown() {
    }

    private static String join(List<HostAndPort> hosts) {
        return hosts.stream()
            .map(HostAndPort::toString)
            .collect(Collectors.joining(","));
    }

    /**
     * FACTORY
     */
    private static final class HostsFactory extends Factory {
        private final List<HostAndPort> hosts;

        HostsFactory(List<HostAndPort> hosts) {
            this.hosts = hosts;
        }

        @Nullable
        @Override
        public NameResolver newNameResolver(URI targetUri, Helper helper) {
            if (!SCHEME.equals(targetUri.getScheme())) {
                return null;
            }
            return new HostsNameResolver(hosts);
        }

        @Override
        public String getDefaultScheme() {
            return SCHEME;
        }
    }
}
