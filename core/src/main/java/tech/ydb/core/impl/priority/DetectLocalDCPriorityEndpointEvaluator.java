package tech.ydb.core.impl.priority;

import tech.ydb.core.utils.Timer;
import tech.ydb.discovery.DiscoveryProtos;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collector;
import java.util.stream.Collectors;

/**
 * @author Kirill Kurdyukov
 */
public class DetectLocalDCPriorityEndpointEvaluator implements PriorityEndpointEvaluator {

    private static final int TCP_PING_TIMEOUT = 5000;
    private static final int LOCALITY_SHIFT = 1000;
    private static final int DELTA_PING_BETWEEN_DC = 10_000_000;
    private static final int NODE_SIZE = 5;

    private Map<String, Long> locationToPriority;

    @Override
    public long evaluatePriority(DiscoveryProtos.EndpointInfo endpointInfo) {
        return locationToPriority.getOrDefault(endpointInfo.getLocation(), Long.MAX_VALUE);
    }

    @Override
    public void prepareStatement(DiscoveryProtos.ListEndpointsResult result) {
        Map<String, List<DiscoveryProtos.EndpointInfo>> dcLocationToNodes = result
                .getEndpointsList()
                .stream()
                .collect(Collectors
                        .groupingBy(DiscoveryProtos.EndpointInfo::getLocation)
                );

        HashMap<String, Long> dcLocationToTcpPing = new HashMap<>();
        long minPing = Integer.MAX_VALUE;

        for (Map.Entry<String, List<DiscoveryProtos.EndpointInfo>> entry : dcLocationToNodes.entrySet()) {
            String dc = entry.getKey();
            List<DiscoveryProtos.EndpointInfo> nodes = entry.getValue();

            Collections.shuffle(nodes);

            int nodeSize = Math.min(nodes.size(), NODE_SIZE);
            long tcpPing = nodes
                    .subList(0, nodeSize)
                    .stream()
                    .map(this::tcpPing)
                    .reduce((a, b) -> {
                        if (a.equals(Long.MAX_VALUE) || b.equals(Long.MAX_VALUE)) {
                            return Long.MAX_VALUE;
                        } else {
                            return a + b;
                        }
                    })
                    .orElseThrow(RuntimeException::new) / nodeSize;

            minPing = Math.min(minPing, tcpPing);

            dcLocationToTcpPing.put(
                    dc,
                    tcpPing
            );
        }

        synchronized (this) {
            long finalMinPing = minPing;
            locationToPriority = dcLocationToTcpPing
                    .entrySet()
                    .stream()
                    .collect(Collectors
                            .toMap(
                                    Map.Entry::getKey,
                                    entry -> (entry.getValue() - finalMinPing)
                                            / DELTA_PING_BETWEEN_DC * LOCALITY_SHIFT
                            )
                    );
        }
    }

    private long tcpPing(DiscoveryProtos.EndpointInfo endpoint) {
        try (final Socket socket = new Socket()) {
            final long startConnection = Timer.nanoTime();

            socket.connect(
                    new InetSocketAddress(endpoint.getAddress(), endpoint.getPort()),
                    TCP_PING_TIMEOUT
            );

            final long stopConnection = Timer.nanoTime();

            return stopConnection - startConnection;
        } catch (IOException e) {
            return Long.MAX_VALUE;
        }
    }
}
