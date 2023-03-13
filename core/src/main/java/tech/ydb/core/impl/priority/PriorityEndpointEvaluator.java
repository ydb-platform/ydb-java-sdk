package tech.ydb.core.impl.priority;

import tech.ydb.discovery.DiscoveryProtos;

/**
 * @author Kirill Kurdyukov
 */
public interface PriorityEndpointEvaluator {

    long evaluatePriority(DiscoveryProtos.EndpointInfo endpointInfo);

    void prepareStatement(DiscoveryProtos.ListEndpointsResult result);
}