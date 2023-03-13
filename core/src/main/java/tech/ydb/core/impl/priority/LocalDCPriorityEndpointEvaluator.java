package tech.ydb.core.impl.priority;

import tech.ydb.discovery.DiscoveryProtos;

/**
 * @author Kirill Kurdyukov
 */
public class LocalDCPriorityEndpointEvaluator implements PriorityEndpointEvaluator {

    private static final int LOCALITY_SHIFT = 1000;

    private final String localDCLocation;

    private volatile String preferred;

    public LocalDCPriorityEndpointEvaluator(String localDCLocation) {
        this.localDCLocation = localDCLocation;
    }

    @Override
    public long evaluatePriority(DiscoveryProtos.EndpointInfo endpointInfo) {
        return preferred.equalsIgnoreCase(endpointInfo.getLocation()) ? 0 : LOCALITY_SHIFT;
    }

    @Override
    public void prepareStatement(DiscoveryProtos.ListEndpointsResult result) {
        preferred = localDCLocation;

        if (preferred == null || preferred.isEmpty()) {
            preferred = result.getSelfLocation();
        }
    }
}
