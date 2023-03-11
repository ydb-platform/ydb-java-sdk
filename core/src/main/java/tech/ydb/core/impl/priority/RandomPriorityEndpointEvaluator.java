package tech.ydb.core.impl.priority;

import tech.ydb.discovery.DiscoveryProtos;

/**
 * @author Kirill Kurdyukov
 */
public class RandomPriorityEndpointEvaluator implements PriorityEndpointEvaluator {

    @Override
    public long evaluatePriority(
            String selfLocation,
            DiscoveryProtos.EndpointInfo endpointInfo
    ) {
        return 0;
    }
}
