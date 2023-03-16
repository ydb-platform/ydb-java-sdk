package tech.ydb.core.impl.pool;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Ticker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.ydb.core.grpc.BalancingSettings;
import tech.ydb.discovery.DiscoveryProtos;

/**
 * @author Kirill Kurdyukov
 */
public class EndpointPriorityFactory {

    private static final Logger logger = LoggerFactory
            .getLogger(EndpointPriorityFactory.class);
    private static final int LOCALITY_SHIFT = 1000;
    private static final int NODE_SIZE = 5;
    private static final int TCP_PING_TIMEOUT_MS = 5000;

    private final Map<String, Long> locationDCToPriority;
    private final Ticker ticker;

    public EndpointPriorityFactory(
            BalancingSettings settings,
            DiscoveryProtos.ListEndpointsResult endpointsResult
    ) {
        this(settings, endpointsResult, Ticker.systemTicker());
    }

    @VisibleForTesting
    public EndpointPriorityFactory(
            BalancingSettings settings,
            DiscoveryProtos.ListEndpointsResult endpointsResult,
            Ticker ticker
    ) {
        this.ticker = ticker;

        Stream<DiscoveryProtos.EndpointInfo> endpointInfoStream = endpointsResult
                .getEndpointsList()
                .stream();

        switch (settings.getPolicy()) {
            case USE_ALL_NODES:
                locationDCToPriority = endpointInfoStream
                        .collect(Collectors.toMap(
                                        DiscoveryProtos.EndpointInfo::getLocation,
                                        e -> 0L,
                                        (a, b) -> a
                                )
                        );
                break;
            case USE_PREFERABLE_LOCATION:
                String preferred = settings.getPreferableLocation();

                if (preferred == null || preferred.isEmpty()) {
                    preferred = endpointsResult.getSelfLocation();
                }

                final String preferredDC = preferred;
                locationDCToPriority = endpointInfoStream
                        .collect(Collectors.toMap(
                                        DiscoveryProtos.EndpointInfo::getLocation,
                                        endpointInfo -> preferredDC
                                                .equalsIgnoreCase(endpointInfo.getLocation())
                                                ? 0L : LOCALITY_SHIFT,
                                        (a, b) -> a
                                )
                        );
                break;
            case USE_DETECT_LOCAL_DC:
                locationDCToPriority = detectLocalDC(endpointInfoStream);
                break;
            default:
                throw new RuntimeException("Not implemented balancing policy: "
                        + settings.getPolicy().name());
        }
    }

    public EndpointPool.PriorityEndpoint createEndpoint(
            DiscoveryProtos.EndpointInfo endpointInfo
    ) {
        return new EndpointPool.PriorityEndpoint(
                endpointInfo,
                locationDCToPriority.getOrDefault(
                        endpointInfo.getLocation(),
                        Long.MAX_VALUE
                )
        );
    }

    private Map<String, Long> detectLocalDC(Stream<DiscoveryProtos.EndpointInfo> endpointInfoStream) {
        Map<String, List<DiscoveryProtos.EndpointInfo>> dcLocationToNodes = endpointInfoStream
                .collect(Collectors
                        .groupingBy(DiscoveryProtos.EndpointInfo::getLocation)
                );

        HashMap<String, Long> dcLocationToTcpPing = new HashMap<>();
        long minPing = Long.MAX_VALUE;

        for (Map.Entry<String, List<DiscoveryProtos.EndpointInfo>> entry : dcLocationToNodes.entrySet()) {
            String dc = entry.getKey();
            List<DiscoveryProtos.EndpointInfo> nodes = entry.getValue();

            assert !nodes.isEmpty();

            Collections.shuffle(nodes);

            int nodeSize = Math.min(nodes.size(), NODE_SIZE);
            long tcpPing = 0;

            for (DiscoveryProtos.EndpointInfo node : nodes.subList(0, nodeSize)) {
                long currentPing = tcpPing(new InetSocketAddress(node.getAddress(), node.getPort()));
                logger.debug("Address: {}, port: {}, nanos ping: {}", node.getAddress(), node.getPort(), currentPing);

                tcpPing += currentPing;
            }

            tcpPing /= nodeSize;

            minPing = Math.min(minPing, tcpPing);

            dcLocationToTcpPing.put(
                    dc,
                    tcpPing
            );
        }

        HashMap<String, Long> newLocationToPriority = new HashMap<>();

        for (Map.Entry<String, Long> entry : dcLocationToTcpPing.entrySet()) {
            long priority = entry.getValue() - minPing;

            logger.debug("Location: {}, priority: {}", entry.getKey(), priority);
            newLocationToPriority.put(
                    entry.getKey(),
                    priority
            );
        }

        return newLocationToPriority;
    }

    private long tcpPing(InetSocketAddress socketAddress) {
        try (Socket socket = new Socket()) {
            final long startConnection = ticker.read();

            socket.connect(
                    socketAddress,
                    TCP_PING_TIMEOUT_MS
            );

            final long stopConnection = ticker.read();

            return stopConnection - startConnection;
        } catch (IOException e) {
            return TCP_PING_TIMEOUT_MS * 2_000_000L;
        }
    }
}
