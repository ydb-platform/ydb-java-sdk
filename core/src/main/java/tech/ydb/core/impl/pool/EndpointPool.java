package tech.ydb.core.impl.pool;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import javax.annotation.Nullable;

import com.google.common.annotations.VisibleForTesting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tech.ydb.core.grpc.BalancingSettings;

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

    private List<PriorityEndpoint> records = new ArrayList<>();
    private Map<Integer, PriorityEndpoint> recordsByNodeId = new HashMap<>();
    private Map<String, PriorityEndpoint> recordsByEndpoint = new HashMap<>();

    private boolean needToRunDiscovery = false;
    // Number of endpoints with best load factor (priority)
    private int bestEndpointsCount = -1;

    public EndpointPool(BalancingSettings balancingSettings) {
        logger.debug("Creating endpoint pool with balancing settings policy: {}", balancingSettings.getPolicy());
        this.balancingSettings = balancingSettings;
    }

    @Nullable
    public EndpointRecord getEndpoint(@Nullable Integer preferredNodeID) {
        recordsLock.readLock().lock();
        try {
            if (preferredNodeID != null) {
                PriorityEndpoint knownEndpoint = recordsByNodeId.get(preferredNodeID);
                if (knownEndpoint != null) {
                    return knownEndpoint.record;
                }
            }
            if (bestEndpointsCount > 0) {
                // returns value in range [0, n)
                int idx = ThreadLocalRandom.current().nextInt(bestEndpointsCount);
                return records.get(idx).record;
            } else {
                return null;
            }
        } finally {
            recordsLock.readLock().unlock();
        }
    }

    // Sets new endpoints, returns removed
    public List<EndpointRecord> setNewState(String selfLocation, List<EndpointRecord> endpoints) {
        PriorityPicker picker = PriorityPicker.from(balancingSettings, selfLocation, endpoints);

        Map<String, PriorityEndpoint> newRecordsByEndpoint = new HashMap<>();
        Map<Integer, PriorityEndpoint> newRecordsByNodeId = new HashMap<>();
        List<PriorityEndpoint> newRecords = new ArrayList<>();
        int newBestEndpointsCount = 0;
        int bestPriority = Integer.MAX_VALUE;

        logger.debug("init new state with {} endpoints", endpoints.size());
        for (EndpointRecord endpoint : endpoints) {
            int priority = picker.getEndpointPriority(endpoint.getLocation());

            PriorityEndpoint entry = new PriorityEndpoint(endpoint, priority);
            String hostAndPort = endpoint.getHostAndPort();

            if (!newRecordsByEndpoint.containsKey(hostAndPort)) {
                logger.debug("added endpoint {}", endpoint);
                newRecordsByEndpoint.put(hostAndPort, entry);
                if (endpoint.getNodeId() != 0) {
                    newRecordsByNodeId.put(endpoint.getNodeId(), entry);
                }
                newRecords.add(entry);

                if (priority < bestPriority) {
                    bestPriority = priority;
                    newBestEndpointsCount = 0;
                }
                if (priority == bestPriority) {
                    newBestEndpointsCount += 1;
                }
            } else {
                logger.warn("duplicate endpoint {}", endpoint.getHostAndPort());
            }
        }

        newRecords.sort(PriorityEndpoint.COMPARATOR);
        List<EndpointRecord> removed = new ArrayList<>();

        recordsLock.writeLock().lock();
        try {
            for (PriorityEndpoint entry : records) {
                if (!newRecordsByEndpoint.containsKey(entry.record.getHostAndPort())) {
                    removed.add(entry.record);
                }
            }

            records = newRecords;
            recordsByNodeId = newRecordsByNodeId;
            recordsByEndpoint = newRecordsByEndpoint;
            bestEndpointsCount = newBestEndpointsCount;
            needToRunDiscovery = false;
        } finally {
            recordsLock.writeLock().unlock();
        }
        return removed;
    }

    public void pessimizeEndpoint(EndpointRecord endpoint) {
        if (endpoint == null) {
            return;
        }

        PriorityEndpoint knownEndpoint = recordsByEndpoint.get(endpoint.getHostAndPort());
        if (knownEndpoint == null) {
            return;
        }

        if (knownEndpoint.isPessimized()) {
            logger.trace("Endpoint {} is already pessimized", endpoint);
            return;
        }

        recordsLock.writeLock().lock();
        try {
            knownEndpoint.pessimize();

            records.sort(PriorityEndpoint.COMPARATOR);

            long bestPriority = records.get(0).priority;
            int newBestCount = 0;
            int newPessimizedCount = 0;
            for (PriorityEndpoint record : records) {
                if (record.getPriority() == bestPriority) {
                    newBestCount++;
                }
                if (record.isPessimized()) {
                    newPessimizedCount++;
                }
            }

            bestEndpointsCount = newBestCount;
            needToRunDiscovery = 100 * newPessimizedCount > records.size() * DISCOVERY_PESSIMIZATION_THRESHOLD;
            if (needToRunDiscovery) {
                logger.debug("launching discovery due to pessimization threshold is exceeded: {}/{} is more than {}",
                        newPessimizedCount, records.size(), DISCOVERY_PESSIMIZATION_THRESHOLD);
            }

            logger.trace("Endpoint {} was pessimized. New pessimization ratio: {}/{}",
                    endpoint, newPessimizedCount, records.size());
        } finally {
            recordsLock.writeLock().unlock();
        }
    }

    public boolean needToRunDiscovery() {
        return needToRunDiscovery;
    }

    @VisibleForTesting
    static class PriorityEndpoint {
        static final Comparator<PriorityEndpoint> COMPARATOR = Comparator
                .comparingLong(PriorityEndpoint::getPriority)
                .thenComparing(e -> e.record.getHostAndPort());

        private final EndpointRecord record;
        private long priority;

        PriorityEndpoint(EndpointRecord record, long priority) {
            this.record = record;
            this.priority = priority;
        }

        public long getPriority() {
            return this.priority;
        }

        public EndpointRecord getEndpoint() {
            return this.record;
        }

        public void pessimize() {
            this.priority = Long.MAX_VALUE;
        }

        public boolean isPessimized() {
            return priority == Long.MAX_VALUE;
        }
    }

    @VisibleForTesting
    Map<Integer, PriorityEndpoint> getEndpointsByNodeId() {
        return this.recordsByNodeId;
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
