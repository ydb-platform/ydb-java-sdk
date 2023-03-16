package tech.ydb.core.impl.pool;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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

    private final String locationDC;
    private final Ticker ticker;

    public EndpointPriorityFactory(
            BalancingSettings settings,
            DiscoveryProtos.ListEndpointsResult endpointsResult
    ) {
        this(settings, endpointsResult, Ticker.systemTicker());
    }

    @VisibleForTesting
    EndpointPriorityFactory(
            BalancingSettings settings,
            DiscoveryProtos.ListEndpointsResult endpointsResult,
            Ticker ticker
    ) {
        this.ticker = ticker;

        switch (settings.getPolicy()) {
            case USE_ALL_NODES:
                locationDC = null;
                break;
            case USE_PREFERABLE_LOCATION:
                String preferred = settings.getPreferableLocation();

                if (preferred == null || preferred.isEmpty()) {
                    preferred = endpointsResult.getSelfLocation();
                }

                locationDC = preferred;
                break;
            case USE_DETECT_LOCAL_DC:
                locationDC = detectLocalDC(endpointsResult);
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
                locationDC == null
                        ? 0 : locationDC.equalsIgnoreCase(endpointInfo.getLocation())
                        ? 0 : LOCALITY_SHIFT
        );
    }

    private String detectLocalDC(DiscoveryProtos.ListEndpointsResult endpointsResult) {
        Map<String, List<DiscoveryProtos.EndpointInfo>> dcLocationToNodes = endpointsResult
                .getEndpointsList()
                .stream()
                .collect(Collectors
                        .groupingBy(DiscoveryProtos.EndpointInfo::getLocation)
                );

        if (dcLocationToNodes.isEmpty()) {
            return null;
        }

        long minPing = Long.MAX_VALUE;
        String localDC = null;

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

            if (minPing > tcpPing) {
                minPing = tcpPing;
                localDC = dc;
            }
        }

        return localDC;
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
