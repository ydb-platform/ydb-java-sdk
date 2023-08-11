package tech.ydb.core.impl.pool;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import javax.annotation.Nullable;

import com.google.common.annotations.VisibleForTesting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tech.ydb.core.grpc.BalancingSettings;
import tech.ydb.proto.discovery.DiscoveryProtos;

/**
 * @author Nikolay Perfilov
 * @author Kirill Kurdyukov
 */
public final class EndpointPool {
    private static final Logger logger = LoggerFactory.getLogger(EndpointPool.class);

    // Maximum percent of endpoints pessimized by transport errors to start recheck
    private static final long DISCOVERY_PESSIMIZATION_THRESHOLD = 50;

    private final BalancingSettings balancingSettings;
    private final ReadWriteLock recordsLock = new ReentrantReadWriteLock();
    private final AtomicInteger pessimizationRatio = new AtomicInteger();
    private final EndpointRecord discoveryEndpoint;
    private List<PriorityEndpoint> records = new ArrayList<>();
    private Map<Integer, PriorityEndpoint> endpointsByNodeId = new HashMap<>();

    // Number of endpoints with best load factor (priority)
    private int bestEndpointsCount = -1;

    public EndpointPool(EndpointRecord discoveryEndpoint, BalancingSettings balancingSettings) {
        logger.debug("Creating endpoint pool with balancing settings policy: {}", balancingSettings.getPolicy());

        this.discoveryEndpoint = discoveryEndpoint;
        this.balancingSettings = balancingSettings;
    }

    public EndpointRecord getEndpoint(@Nullable Integer preferredNodeID) {
        recordsLock.readLock().lock();
        try {
            if (preferredNodeID != null) {
                PriorityEndpoint knownEndpoint = endpointsByNodeId.get(preferredNodeID);
                if (knownEndpoint != null) {
                    return knownEndpoint;
                }
            }
            if (bestEndpointsCount > 0) {
                // returns value in range [0, n)
                int idx = ThreadLocalRandom.current().nextInt(bestEndpointsCount);
                return records.get(idx);
            } else {
                return discoveryEndpoint;
            }
        } finally {
            recordsLock.readLock().unlock();
        }
    }

    // Sets new endpoints, returns removed
    public List<EndpointRecord> setNewState(DiscoveryProtos.ListEndpointsResult result) {
        EndpointPriorityFactory priorityFactory = new EndpointPriorityFactory(
                balancingSettings,
                result
        );

        Set<String> newKnownEndpoints = new HashSet<>();
        Map<Integer, PriorityEndpoint> newKnownEndpointsByNodeId = new HashMap<>();
        List<PriorityEndpoint> newRecords = new ArrayList<>();

        logger.debug("init new state with {} endpoints", result.getEndpointsCount());
        for (DiscoveryProtos.EndpointInfo info : result.getEndpointsList()) {
            PriorityEndpoint entry = priorityFactory.createEndpoint(info);
            String endpoint = entry.getHostAndPort();

            if (!newKnownEndpoints.contains(endpoint)) {
                logger.debug("added endpoint {}", entry);
                newKnownEndpoints.add(endpoint);
                if (entry.getNodeId() != 0) {
                    newKnownEndpointsByNodeId.put(entry.getNodeId(), entry);
                }
                newRecords.add(entry);
            } else {
                logger.warn("duplicate endpoint {}", entry.getHostAndPort());
            }
        }

        newRecords.sort(PriorityEndpoint.COMPARATOR);
        int newBestEndpointsCount = getBestEndpointsCount(newRecords);

        List<EndpointRecord> removed = new ArrayList<>();
        for (PriorityEndpoint entry : records) {
            if (!newKnownEndpoints.contains(entry.getHostAndPort())) {
                removed.add(entry);
            }
        }

        recordsLock.writeLock().lock();
        try {
            records = newRecords;
            endpointsByNodeId = newKnownEndpointsByNodeId;
            bestEndpointsCount = newBestEndpointsCount;
            pessimizationRatio.set(0);
        } finally {
            recordsLock.writeLock().unlock();
        }
        return removed;
    }

    public void pessimizeEndpoint(EndpointRecord endpoint) {
        if (!(endpoint instanceof PriorityEndpoint)) {
            logger.trace("Endpoint {} is unknown", endpoint);
            return;
        }

        PriorityEndpoint knownEndpoint = (PriorityEndpoint) endpoint;
        if (knownEndpoint.isPessimized()) {
            logger.trace("Endpoint {} is already pessimized", endpoint);
            return;
        }

        recordsLock.writeLock().lock();
        try {
            knownEndpoint.pessimize();

            int newRatio = (pessimizationRatio.get() * records.size() + 100) / records.size();
            pessimizationRatio.set(newRatio);
            if (needToRunDiscovery()) {
                logger.debug("launching discovery due to pessimization threshold is exceeded: {} is more than {}",
                        newRatio, DISCOVERY_PESSIMIZATION_THRESHOLD);
            }

            records.sort(PriorityEndpoint.COMPARATOR);
            bestEndpointsCount = getBestEndpointsCount(records);

            logger.info("Endpoint {} was pessimized. New pessimization ratio: {}", endpoint, newRatio);
        } finally {
            recordsLock.writeLock().unlock();
        }
    }

    public boolean needToRunDiscovery() {
        return pessimizationRatio.get() > DISCOVERY_PESSIMIZATION_THRESHOLD;
    }

    // returns amount of endpoints with best load factor (priority)
    private static int getBestEndpointsCount(List<PriorityEndpoint> records) {
        if (records.isEmpty()) {
            return -1;
        }

        final long bestPriority = records.get(0).priority;
        int pos = 1;
        while (pos < records.size()) {
            if (records.get(pos).priority != bestPriority) {
                break;
            }
            pos++;
        }
        return pos;
    }

    @VisibleForTesting
    static class PriorityEndpoint extends EndpointRecord {
        static final Comparator<PriorityEndpoint> COMPARATOR = Comparator
                .comparingLong(PriorityEndpoint::getPriority)
                .thenComparing(PriorityEndpoint::getHost)
                .thenComparing(PriorityEndpoint::getPort);

        private long priority;

        PriorityEndpoint(DiscoveryProtos.EndpointInfo endpoint, long priority) {
            super(endpoint.getAddress(), endpoint.getPort(), endpoint.getNodeId());
            this.priority = priority;
        }

        public long getPriority() {
            return this.priority;
        }

        public void pessimize() {
            this.priority = Long.MAX_VALUE;
        }

        public boolean isPessimized() {
            return priority == Long.MAX_VALUE;
        }

        @Override
        public String toString() {
            return "PriorityEndpoint{host=" + getHost() +
                    ", port=" + getPort() +
                    ", node=" + getNodeId() +
                    ", priority= " + priority + "}";
        }
    }

    @VisibleForTesting
    Map<Integer, PriorityEndpoint> getEndpointsByNodeId() {
        return this.endpointsByNodeId;
    }

    @VisibleForTesting
    List<PriorityEndpoint> getRecords() {
        return this.records;
    }

    @VisibleForTesting
    int getBestEndpointCount() {
        return bestEndpointsCount;
    }
}
