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

/**
 * @author Kirill Kurdyukov
 */
public class PriorityPicker {
    private static final Logger logger = LoggerFactory.getLogger(PriorityPicker.class);

    private static final int LOCALITY_SHIFT = 1000;
    private static final int DETECT_DC_NODE_SIZE = 3;
    private static final int DETECT_DC_TCP_PING_TIMEOUT_MS = 5000;

    private final String prefferedLocation;

    private PriorityPicker(String location) {
        this.prefferedLocation = location;
    }

    public int getEndpointPriority(String location) {
        if (prefferedLocation == null || prefferedLocation.equalsIgnoreCase(location)) {
            return 0;
        }

        return LOCALITY_SHIFT;
    }

    public static PriorityPicker from(BalancingSettings settings, String selfLocation, List<EndpointRecord> endpoints) {
        switch (settings.getPolicy()) {
            case USE_ALL_NODES:
                return new PriorityPicker(null);
            case USE_PREFERABLE_LOCATION:
                return new PriorityPicker(getLocationFromConfig(settings.getPreferableLocation(), selfLocation));
            case USE_DETECT_LOCAL_DC:
                return new PriorityPicker(detectLocalDC(endpoints, Ticker.systemTicker()));
            default:
                throw new RuntimeException("Not implemented balancing policy: " + settings.getPolicy().name());
        }
    }

    @VisibleForTesting
    static String getLocationFromConfig(String prefferable, String selfLocation) {
        if (prefferable != null && !prefferable.isEmpty()) {
            return prefferable;
        }
        if (selfLocation != null && !selfLocation.isEmpty()) {
            return selfLocation;
        }
        return null;
    }

    @VisibleForTesting
    static String detectLocalDC(List<EndpointRecord> endpoints, Ticker ticker) {
        Map<String, List<EndpointRecord>> dcLocationToNodes = endpoints
                .stream()
                .collect(Collectors.groupingBy(EndpointRecord::getLocation));

        if (dcLocationToNodes.size() < 2) {
            return null;
        }

        long minPing = Long.MAX_VALUE;
        String localDC = null;

        for (Map.Entry<String, List<EndpointRecord>> entry : dcLocationToNodes.entrySet()) {
            String dc = entry.getKey();
            List<EndpointRecord> nodes = entry.getValue();

            assert !nodes.isEmpty();

            Collections.shuffle(nodes);

            int nodeSize = Math.min(nodes.size(), DETECT_DC_NODE_SIZE);
            long tcpPing = 0;

            for (EndpointRecord node : nodes.subList(0, nodeSize)) {
                long currentPing = tcpPing(new InetSocketAddress(node.getHost(), node.getPort()), ticker);
                logger.debug("Address: {}, port: {}, nanos ping: {}", node.getHost(), node.getPort(), currentPing);
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

    private static long tcpPing(InetSocketAddress socketAddress, Ticker ticker) {
        try (Socket socket = new Socket()) {
            final long startConnection = ticker.read();
            socket.connect(socketAddress, DETECT_DC_TCP_PING_TIMEOUT_MS);
            final long stopConnection = ticker.read();
            return stopConnection - startConnection;
        } catch (IOException e) {
            return Long.MAX_VALUE;
        }
    }
}
