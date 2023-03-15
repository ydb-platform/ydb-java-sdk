package tech.ydb.core.impl.priority;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Ticker;

import tech.ydb.discovery.DiscoveryProtos;

/**
 * @author Kirill Kurdyukov
 */
public class DetectLocalDCPriorityEndpointEvaluator implements PriorityEndpointEvaluator {

    private static final int TCP_PING_TIMEOUT_MS = 5000;
    private static final int LOCALITY_SHIFT = 1000;
    private static final int NODE_SIZE = 5;

    private final Ticker ticker;

    private Map<String, Long> locationToPriority;

    public DetectLocalDCPriorityEndpointEvaluator() {
        ticker = Ticker.systemTicker();
    }

    @VisibleForTesting
    public DetectLocalDCPriorityEndpointEvaluator(Ticker ticker) {
        this.ticker = ticker;
    }

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
            newLocationToPriority.put(
                    entry.getKey(),
                    (entry.getValue() - minPing) * LOCALITY_SHIFT
            );
        }

        synchronized (this) {
            locationToPriority = newLocationToPriority;
        }
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
