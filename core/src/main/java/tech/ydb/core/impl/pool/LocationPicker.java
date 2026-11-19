package tech.ydb.core.impl.pool;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.net.SocketFactory;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Ticker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Kirill Kurdyukov
 */
class LocationPicker implements PriorityPicker {
    private static final Logger logger = LoggerFactory.getLogger(PriorityPicker.class);

    private static final int LOCALITY_SHIFT = 1000;
    private static final int DETECT_DC_NODE_SIZE = 3;
    private static final int DETECT_DC_TCP_PING_TIMEOUT_MS = 5000;

    private final String preferredLocation;

    LocationPicker(String preferable, String selfLocation) {
        this.preferredLocation = getLocationFromConfig(preferable, selfLocation);
    }

    LocationPicker(List<EndpointRecord> endpoints) {
        this.preferredLocation = detectLocalDC(endpoints, Ticker.systemTicker());
    }

    @Override
    public int getPriority(EndpointRecord endpoint) {
        if (preferredLocation == null || preferredLocation.equalsIgnoreCase(endpoint.getLocation())) {
            return 0;
        }

        return LOCALITY_SHIFT;
    }

    @VisibleForTesting
    static String getLocationFromConfig(String preferable, String selfLocation) {
        if (preferable != null && !preferable.isEmpty()) {
            return preferable;
        }
        if (selfLocation != null && !selfLocation.isEmpty()) {
            return selfLocation;
        }
        return null;
    }

    @VisibleForTesting
    static String detectLocalDC(List<EndpointRecord> endpoints, Ticker ticker) {
        // Endpoints without a known location carry no information about datacenters. They must be
        // filtered out because groupingBy rejects a null key - the endpoint the transport bootstraps
        // with has no location until the first discovery completes.
        Map<String, List<EndpointRecord>> dcLocationToNodes = endpoints
                .stream()
                .filter(endpoint -> endpoint.getLocation() != null && !endpoint.getLocation().isEmpty())
                .collect(Collectors.groupingBy(EndpointRecord::getLocation));

        if (dcLocationToNodes.size() < 2) {
            return null;
        }

        long minPing = Long.MAX_VALUE;
        String localDC = null;

        for (Map.Entry<String, List<EndpointRecord>> entry : dcLocationToNodes.entrySet()) {
            String dc = entry.getKey();
            List<EndpointRecord> nodes = entry.getValue();

            Collections.shuffle(nodes);

            int nodeSize = Math.min(nodes.size(), DETECT_DC_NODE_SIZE);
            long tcpPing = 0;
            int reachableCount = 0;

            for (EndpointRecord node : nodes.subList(0, nodeSize)) {
                try {
                    long currentPing = tcpPing(new InetSocketAddress(node.getHost(), node.getPort()), ticker);
                    logger.debug("{}, nanos ping: {}", node, currentPing);
                    tcpPing += currentPing;
                    reachableCount++;
                } catch (IOException ex) {
                    logger.warn("{} is not reachable with error {}", node, ex.getMessage());
                }
            }

            if (reachableCount == 0) {
                logger.debug("No reachable nodes in {}, skip it", dc);
                continue;
            }

            tcpPing /= reachableCount;

            if (minPing > tcpPing) {
                minPing = tcpPing;
                localDC = dc;
            }
        }

        return localDC;
    }

    private static long tcpPing(InetSocketAddress socketAddress, Ticker ticker) throws IOException {
        try (Socket socket = SocketFactory.getDefault().createSocket()) {
            long started = ticker.read();
            socket.connect(socketAddress, DETECT_DC_TCP_PING_TIMEOUT_MS);
            return ticker.read() - started;
        }
    }
}
