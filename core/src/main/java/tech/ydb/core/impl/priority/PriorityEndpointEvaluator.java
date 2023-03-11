package tech.ydb.core.impl.priority;

import tech.ydb.discovery.DiscoveryProtos;

/**t
 * @author Kirill Kurdyukov
 */
public interface PriorityEndpointEvaluator {

    long evaluatePriority(
            String selfLocation,
            DiscoveryProtos.EndpointInfo endpointInfo
    );
}