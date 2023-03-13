package tech.ydb.core.impl.priority;

import tech.ydb.discovery.DiscoveryProtos;

/**
 * @author Kirill Kurdyukov
 */
public class RandomPriorityEndpointEvaluator implements PriorityEndpointEvaluator {

    @Override
    public long evaluatePriority(DiscoveryProtos.EndpointInfo endpointInfo) {
        return 0;
    }

    @Override
    public void prepareStatement(DiscoveryProtos.ListEndpointsResult result) {
    }
}
