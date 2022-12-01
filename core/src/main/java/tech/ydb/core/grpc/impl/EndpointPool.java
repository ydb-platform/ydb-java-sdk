package tech.ydb.core.grpc.impl;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import javax.annotation.Nullable;

import com.google.common.annotations.VisibleForTesting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tech.ydb.core.grpc.BalancingPolicy;
import tech.ydb.core.grpc.BalancingSettings;
import tech.ydb.discovery.DiscoveryProtos;

/**
 * @author Nikolay Perfilov
 */
public final class EndpointPool {
    private static final Logger logger = LoggerFactory.getLogger(EndpointPool.class);

    // Maximum percent of endpoints pessimized by transport errors to start recheck
    private static final long DISCOVERY_PESSIMIZATION_THRESHOLD = 50;

    private static final int LOCALITY_SHIFT = 1000;

    private final BalancingSettings balancingSettings;
    private final ReadWriteLock recordsLock = new ReentrantReadWriteLock();
    private final AtomicInteger pessimizationRatio = new AtomicInteger();
    private List<PriorityEndpoint> records = new ArrayList<>();
    private Map<String, PriorityEndpoint> knownEndpoints = new HashMap<>();
    private Map<Integer, String> knownEndpointsByNodeId = new HashMap<>();
    // Number of endpoints with best load factor (priority)
    private int bestEndpointsCount = -1;

    public EndpointPool(BalancingSettings balancingSettings) {
        this.balancingSettings = balancingSettings;
    }

    public EndpointRecord getEndpoint(@Nullable String preferredEndpoint) {
        recordsLock.readLock().lock();
        try {
            if (preferredEndpoint != null && !preferredEndpoint.isEmpty()) {
                PriorityEndpoint knownEndpoint = knownEndpoints.get(preferredEndpoint);
                if (knownEndpoint != null) {
                    return knownEndpoint;
                }
            }
            if (bestEndpointsCount == -1) {
                assert records.isEmpty();
                return null;
            } else {
                // returns value in range [0, n)
                int idx = ThreadLocalRandom.current().nextInt(bestEndpointsCount);
                return records.get(idx);
            }
        } finally {
            recordsLock.readLock().unlock();
        }
    }

    public String getEndpointByNodeId(int nodeId) {
        recordsLock.readLock().lock();
        try {
            return knownEndpointsByNodeId.get(nodeId);
        } finally {
            recordsLock.readLock().unlock();
        }
    }

    private int getStartPriority(String selfLocation, String location) {
        if (balancingSettings.getPolicy() == BalancingPolicy.USE_ALL_NODES) {
            return 0;
        }

        String prefered = balancingSettings.getPreferableLocation();
        if (prefered == null || prefered.isEmpty()) { // If prefered location were not specified - use self location
            prefered = selfLocation;
        }

        return (location != null && location.equalsIgnoreCase(prefered)) ? 0 : LOCALITY_SHIFT;
    }

    // Sets new endpoints, returns removed
    public List<EndpointRecord> setNewState(DiscoveryProtos.ListEndpointsResult result) {
        String selfLocation = result.getSelfLocation();

        Map<String, PriorityEndpoint> newKnownEndpoints = new HashMap<>();
        Map<Integer, String> newKnownEndpointsByNodeId = new HashMap<>();
        List<PriorityEndpoint> newRecords = new ArrayList<>();

        logger.debug("init new state with location {} and {} endpoints", selfLocation, result.getEndpointsCount());
        for (DiscoveryProtos.EndpointInfo info : result.getEndpointsList()) {
            int priority = getStartPriority(selfLocation, info.getLocation());
            PriorityEndpoint entry = new PriorityEndpoint(info, priority);
            String endpoint = entry.getHostAndPort();

            if (!newKnownEndpoints.containsKey(endpoint)) {
                logger.debug("  added endpoint {}", entry);
                newKnownEndpoints.put(endpoint, entry);
                newKnownEndpointsByNodeId.put(entry.getNodeId(), endpoint);
                newRecords.add(entry);
            } else {
                logger.warn("dublicate endpoint {}", entry.getHostAndPort());
            }
        }

        newRecords.sort(Comparator.comparingInt(PriorityEndpoint::getPriority));
        int newBestEndpointsCount = getBestEndpointsCount(newRecords);

        List<EndpointRecord> removed = new ArrayList<>();
        for (PriorityEndpoint entry: records) {
            if (!newKnownEndpoints.containsKey(entry.getHostAndPort())) {
                removed.add(entry);
            }
        }

        recordsLock.writeLock().lock();
        try {
            records = newRecords;
            knownEndpoints = newKnownEndpoints;
            knownEndpointsByNodeId = newKnownEndpointsByNodeId;
            bestEndpointsCount = newBestEndpointsCount;
            pessimizationRatio.set(0);
        } finally {
            recordsLock.writeLock().unlock();
        }
        return removed;
    }

    public void pessimizeEndpoint(String endpoint) {
        PriorityEndpoint knownEndpoint;
        recordsLock.readLock().lock();
        try {
            knownEndpoint = knownEndpoints.get(endpoint);
            if (knownEndpoint == null) {
                logger.trace("Endpoint {} is unknown", endpoint);
                return;
            }
            if (knownEndpoint.priority == Integer.MAX_VALUE) {
                logger.trace("Endpoint {} is already pessimized", endpoint);
                return;
            }
        } finally {
            recordsLock.readLock().unlock();
        }

        recordsLock.writeLock().lock();
        try {
            knownEndpoint.priority = Integer.MAX_VALUE;

            int newRatio = (pessimizationRatio.get() * records.size() + 100) / records.size();
            pessimizationRatio.set(newRatio);
            if (needToRunDiscovery()) {
                logger.debug("launching discovery due to pessimization threshold is exceeded: {} is more than {}",
                        newRatio, DISCOVERY_PESSIMIZATION_THRESHOLD);
            }

            records.sort(Comparator.comparingInt(PriorityEndpoint::getPriority));
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

        final int bestPriority = records.get(0).priority;
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
        private int priority;

        PriorityEndpoint(DiscoveryProtos.EndpointInfo endpoint, int priority) {
            super(endpoint.getAddress(), endpoint.getPort(), endpoint.getNodeId());
            this.priority = priority;
        }

        public int getPriority() {
            return this.priority;
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
    List<PriorityEndpoint> getRecords() {
        return this.records;
    }

    @VisibleForTesting
    Map<String, PriorityEndpoint> getKnownEndpoints() {
        return this.knownEndpoints;
    }

    @VisibleForTesting
    int getBestEndpointCount() {
        return bestEndpointsCount;
    }
}
