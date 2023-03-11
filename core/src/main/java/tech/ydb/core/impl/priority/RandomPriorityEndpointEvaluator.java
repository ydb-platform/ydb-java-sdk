package tech.ydb.core.impl.priority;

import tech.ydb.discovery.DiscoveryProtos;

/**
 * @author Kurdyukov Kirill
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
