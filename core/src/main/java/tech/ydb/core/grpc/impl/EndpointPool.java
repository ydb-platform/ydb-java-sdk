package tech.ydb.core.grpc.impl;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;

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

    // Max, min load factor returned by discovery service
    static final float LOAD_MAX = 100;
    static final float LOAD_MIN = -100;
    // Is used to convert float to integer load factor
    // same integer values will be selected randomly.
    static final float MULTIPLICATOR = 10;
    static final int LOCALITY_SHIFT = Math.round(LOAD_MAX * MULTIPLICATOR);

    private final BalancingSettings balancingSettings;
    private final ReadWriteLock recordsLock = new ReentrantReadWriteLock();
    private final AtomicInteger pessimizationRatio = new AtomicInteger();
    private List<PriorityEndpoint> records = new ArrayList<>();
    private Map<String, PriorityEndpoint> knownEndpoints = new HashMap<>();
    private Map<Integer, String> knownEndpointsByNodeId = new HashMap<>();
    // Number of endpoints with best load factor (priority)
    private int bestEndpointsCount = -1;
    private final Random random;

    @VisibleForTesting
    EndpointPool(BalancingSettings balancingSettings, Random random) {
        this.balancingSettings = balancingSettings;
        this.random = random;
    }

    public EndpointPool(BalancingSettings balancingSettings) {
        this(balancingSettings, new Random());
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
                int idx = random.nextInt(bestEndpointsCount);
                return records.get(idx);
            }
        } finally {
            recordsLock.readLock().unlock();
        }
    }

    public String getEndpointByNodeId(int nodeId) {
        String endpoint = knownEndpointsByNodeId.get(nodeId);
        logger.debug("Node id {} is Endpoint {}", nodeId, endpoint);
        return endpoint;
    }

    private String getPreferredLocation(String selfLocation) {
        String prefered = balancingSettings.getPreferableLocation();
        switch (balancingSettings.getPolicy()) {
            case USE_ALL_NODES:
                return null;
            case USE_PREFERABLE_LOCATION:
                if (prefered == null || prefered.isEmpty()) {
                    return selfLocation;
                } else {
                    return prefered;
                }
            default:
                return "";
        }
    }

    // Sets new endpoints, returns removed
    public List<EndpointRecord> setNewState(String selfLocation, List<DiscoveryProtos.EndpointInfo> newRecords) {
        Map<String, PriorityEndpoint> index = new HashMap<>();
        for (DiscoveryProtos.EndpointInfo info : newRecords) {
            PriorityEndpoint entry = new PriorityEndpoint(selfLocation, info);
            index.put(entry.getHostAndPort(), entry);
        }

        List<PriorityEndpoint> uniqueRecords = index.values().stream()
                .sorted(Comparator.comparingInt(e -> e.priority))
                .collect(Collectors.toList());

        int newBestEndpointsCount = getBestEndpointsCount(uniqueRecords);
        if (logger.isDebugEnabled()) {
            logger.debug("setting new state with {} best endpoints of {}. Endpoints: {}",
                    newBestEndpointsCount,
                    newRecords.size(),
                    uniqueRecords
                            .stream()
                            .map(PriorityEndpoint::toString)
                            .collect(Collectors.joining(", "))
            );
        }

        List<EndpointRecord> removed = new ArrayList<>();

        // This method could not be called more than once simultaneously due to updateInProgress AtomicBoolean.
        // So there is no need to lock recordsLock until we change records
        Map<String, PriorityEndpoint> newKnownEndpoints = new HashMap<>(knownEndpoints);
        for (PriorityEndpoint record : records) {
            String hostWithPort = record.getHostAndPort();
            if (!index.containsKey(hostWithPort)) {
                removed.add(record);
                assert newKnownEndpoints.remove(hostWithPort) != null;
            }
        }

        for (PriorityEndpoint record : uniqueRecords) {
            newKnownEndpoints.put(record.getHostAndPort(), record);
        }

        Map<Integer, String> newKnownEndpointsByNodeId = new HashMap<>(uniqueRecords.size());
        for (PriorityEndpoint record : uniqueRecords) {
            newKnownEndpointsByNodeId.put(record.getNodeId(), record.getHostAndPort());
        }

        assert uniqueRecords.size() == newKnownEndpoints.size();

        recordsLock.writeLock().lock();
        try {
            records = uniqueRecords;
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
        recordsLock.readLock().lock();
        try {
            PriorityEndpoint knownEndpoint = knownEndpoints.get(endpoint);
            if (knownEndpoint == null || knownEndpoint.priority == Integer.MAX_VALUE) {
                if (logger.isTraceEnabled()) {
                    logger.trace("Endpoint {} is already pessimized", endpoint);
                }
                return;
            }
        } finally {
            recordsLock.readLock().unlock();
        }
        int newRatio = -1;
        boolean pessimized = false;
        recordsLock.writeLock().lock();
        try {
            for (PriorityEndpoint record : records) {
                if (endpoint.equals(record.getHostAndPort())) {
                    if (record.priority != Integer.MAX_VALUE) {
                        newRatio = (pessimizationRatio.get() * records.size() + 100) / records.size();
                        pessimizationRatio.set(newRatio);
                        record.priority = Integer.MAX_VALUE;
                        PriorityEndpoint knownEndpoint = knownEndpoints.get(endpoint);
                        if (knownEndpoint != null) {
                            knownEndpoint.priority = Integer.MAX_VALUE;
                        }
                        pessimized = true;
                    }
                    break;
                }
            }
            records.sort(Comparator.comparingInt(e -> e.priority));
            bestEndpointsCount = getBestEndpointsCount(records);
        } finally {
            recordsLock.writeLock().unlock();
        }
        if (pessimized) {
            logger.info("Endpoint {} was pessimized. New pessimization ratio: {}", endpoint, newRatio);
        } else {
            logger.trace("Endpoint {} was already pessimized recently", endpoint);
        }
    }

    public boolean needToRunDiscovery() {
        int ratio = pessimizationRatio.get();
        if (ratio > DISCOVERY_PESSIMIZATION_THRESHOLD) {
            logger.info("launching discovery due to pessimization threshold is exceeded: {} is more than {}",
                    ratio, DISCOVERY_PESSIMIZATION_THRESHOLD);
            return true;
        }
        return false;
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
    class PriorityEndpoint extends EndpointRecord {
        private int priority;

        PriorityEndpoint(String selfLocation, DiscoveryProtos.EndpointInfo endpoint) {
            super(endpoint.getAddress(), endpoint.getPort(), endpoint.getNodeId());

            int loadFactor = Math.round(
                    MULTIPLICATOR * Math.min(LOAD_MAX, Math.max(LOAD_MIN, endpoint.getLoadFactor()))
            );
            if (balancingSettings.getPolicy() == BalancingPolicy.USE_PREFERABLE_LOCATION
                    && !endpoint.getLocation().equals(getPreferredLocation(selfLocation))) {
                loadFactor += LOCALITY_SHIFT;
            }

            this.priority = loadFactor;
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
}
