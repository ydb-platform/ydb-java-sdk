package tech.ydb.core.impl.priority;

import tech.ydb.discovery.DiscoveryProtos;

/**
 * @author Kurdyukov Kirill
 */
public class LocalDCPriorityEndpointEvaluator implements PriorityEndpointEvaluator {

    private static final int LOCALITY_SHIFT = 1000;

    private final String localDCLocation;

    public LocalDCPriorityEndpointEvaluator(String localDCLocation) {
        this.localDCLocation = localDCLocation;
    }

    @Override
    public long evaluatePriority(String selfLocation, DiscoveryProtos.EndpointInfo endpointInfo) {
        String preferred = localDCLocation;

        if (preferred == null || preferred.isEmpty()) {
            preferred = selfLocation;
        }

        return preferred.equalsIgnoreCase(endpointInfo.getLocation()) ? 0 : LOCALITY_SHIFT;
    }
}
