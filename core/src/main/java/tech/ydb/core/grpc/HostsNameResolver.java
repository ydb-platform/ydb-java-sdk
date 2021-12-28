package tech.ydb.core.grpc;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

import com.google.common.hash.Hasher;
import com.google.common.hash.Hashing;
import com.google.common.net.HostAndPort;
import io.grpc.Attributes;
import io.grpc.EquivalentAddressGroup;
import io.grpc.NameResolver;
import io.grpc.Status;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * @author Sergey Polovko
 */
final class HostsNameResolver extends NameResolver {
    private static final Logger logger = LoggerFactory.getLogger(YdbNameResolver.class);

    private static final String SCHEME = "ydb-hosts";

    private final List<HostAndPort> hosts;
    private final ConcurrentMap<HostAndPort, EquivalentAddressGroup> latestSuccessResolving = new ConcurrentHashMap<>();
    private final String authority;
    private Listener listener;
    private final Executor executor;
    private final AtomicBoolean running = new AtomicBoolean();

    private HostsNameResolver(List<HostAndPort> hosts, Executor executor) {
        this.hosts = hosts;
        this.authority = makeAuthority(hosts);
        this.executor = executor;
    }

    static String makeTarget(List<HostAndPort> hosts) {
        return SCHEME + "://" + join(hosts);
    }

    static Factory newFactory(List<HostAndPort> hosts, Executor executor) {
        return new HostsFactory(hosts, executor);
    }

    @Override
    public String getServiceAuthority() {
        return authority;
    }

    @Override
    public void start(Listener listener) {
        this.listener = listener;
        refresh();
    }

    @Override
    public void refresh() {
        if (running.compareAndSet(false, true)) {
            executor.execute(this::resolve);
        }
    }

    private void resolve() {
        try {
            List<EquivalentAddressGroup> groups = new ArrayList<>(hosts.size());
            UnknownHostException latestUnknownEx = null;
            for (HostAndPort host : hosts) {
                try {
                    EquivalentAddressGroup group = createAddressGroup(host);
                    latestSuccessResolving.put(host, group);
                    groups.add(group);
                } catch (UnknownHostException e) {
                    logger.warn(e.getMessage());
                    EquivalentAddressGroup prevResolved = latestSuccessResolving.get(host);
                    if (prevResolved != null) {
                        groups.add(prevResolved);
                    } else {
                        latestUnknownEx = e;
                    }
                }
            }

            if (groups.isEmpty() && latestUnknownEx != null) {
                onFailResolve(latestUnknownEx);
                return;
            }

            onSuccessResolve(groups);
        } catch (Throwable e) {
            onFailResolve(e);
        }
    }

    private void onSuccessResolve(List<EquivalentAddressGroup> groups) {
        running.set(false);
        listener.onAddresses(groups, Attributes.EMPTY);
    }

    private void onFailResolve(Throwable e) {
        running.set(false);
        listener.onError(Status.INTERNAL.withCause(e).withDescription("cannot resolve hosts"));
    }

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

    private static String makeAuthority(List<HostAndPort> hosts) {
        Hasher hasher = Hashing.sha256().newHasher();
        for (HostAndPort host : hosts) {
            hasher.putString(host.toString(), StandardCharsets.UTF_8);
        }
        return hasher.hash().toString();
    }

    /**
     * FACTORY
     */
    private static final class HostsFactory extends Factory {
        private final List<HostAndPort> hosts;
        private final Executor executor;

        HostsFactory(List<HostAndPort> hosts, Executor executor) {
            this.hosts = hosts;
            this.executor = executor;
        }

        @Nullable
        @Override
        public NameResolver newNameResolver(URI targetUri, Helper helper) {
            if (!SCHEME.equals(targetUri.getScheme())) {
                return null;
            }
            return new HostsNameResolver(hosts, executor);
        }

        @Override
        public String getDefaultScheme() {
            return SCHEME;
        }
    }
}
