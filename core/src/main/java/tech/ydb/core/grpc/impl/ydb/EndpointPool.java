package tech.ydb.core.grpc.impl.ydb;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import tech.ydb.core.Result;
import tech.ydb.core.Status;
import tech.ydb.core.StatusCode;
import tech.ydb.core.grpc.BalancingPolicy;
import tech.ydb.core.grpc.BalancingSettings;
import tech.ydb.discovery.DiscoveryProtos;
import tech.ydb.discovery.DiscoveryProtos.ListEndpointsResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Nikolay Perfilov
 */
public final class EndpointPool {
    private static final Logger logger = LoggerFactory.getLogger(EndpointPool.class);

    // Max, min load factor returned by discovery service
    static final float LOAD_MAX = 100;
    static final float LOAD_MIN = -100;
    // Is used to convert float to integer load factor
    // same integer values will be selected randomly.
    static final float MULTIPLICATOR = 10;
    static final int LOCALITY_SHIFT = Math.round(LOAD_MAX * MULTIPLICATOR);

    private final Supplier<CompletableFuture<Result<ListEndpointsResult>>> provider;
    private final BalancingSettings balancingSettings;
    private final AtomicBoolean updateInProgress = new AtomicBoolean();
    private final ReadWriteLock recordsLock = new ReentrantReadWriteLock();
    private AtomicLong lastUpdateTime;
    private List<EndpointEntry> records = new ArrayList<>();
    private Map<String, EndpointEntry> knownEndpoints = new HashMap<>();
    // Number of endpoints with best load factor (priority)
    private int bestEndpointsCount = -1;
    private final Random random;

    public EndpointPool(Supplier<CompletableFuture<Result<ListEndpointsResult>>> provider,
                        BalancingSettings balancingSettings) {
        this.provider = provider;
        this.balancingSettings = balancingSettings;
        this.lastUpdateTime = new AtomicLong(Instant.EPOCH.toEpochMilli());
        this.random = new Random();
    }

    public static class EndpointUpdateResultData {
        public List<String> removed;
        public Status discoveryStatus;

        public EndpointUpdateResultData(List<String> removed, Status discoveryStatus) {
            this.removed = removed;
            this.discoveryStatus = discoveryStatus;
        }
    }

    public static class EndpointUpdateResult {
        public CompletableFuture<EndpointUpdateResultData> data;
        public boolean discoveryWasPerformed;

        public EndpointUpdateResult(CompletableFuture<EndpointUpdateResultData> data, boolean discoveryWasPerformed) {
            this.data = data;
            this.discoveryWasPerformed = discoveryWasPerformed;
        }
    }

    private static class EndpointEntry {
        private final EndpointRecord endpoint;
        private int priority;

        public EndpointEntry(EndpointRecord endpoint, int priority) {
            this.endpoint = Objects.requireNonNull(endpoint);
            this.priority = priority;
        }

        @Override
        public String toString() {
            return "{" + endpoint.getHostAndPort() + "(priority=" + priority + ")}";
        }
    }

    public EndpointUpdateResult updateAsync() {
        boolean updateStarted = updateInProgress.compareAndSet(false, true);
        if (!updateStarted) {
            logger.debug("couldn't start update: already in progress");
            return new EndpointUpdateResult(null, false);
        } else {
            logger.debug("update started");
        }
        CompletableFuture<EndpointUpdateResultData> future = new CompletableFuture<>();
        logger.debug("updating endpoints, calling ListEndpoints...");
        provider.get().thenAccept(result -> {
            List<String> removed = null;
            if (result.isSuccess()) {
                List<EndpointEntry> newRecords = new ArrayList<>();
                ListEndpointsResult response = result.expect("couldn't get response from ListEndpointsResult");
                logger.debug("successfully received ListEndpoints result with {} endpoints",
                        response.getEndpointsList().size());

                final String preferredLocation = getPreferredLocation(response.getSelfLocation());
                for (DiscoveryProtos.EndpointInfo endpoint : response.getEndpointsList()) {
                    int loadFactor = Math.round(
                            MULTIPLICATOR * Math.min(LOAD_MAX, Math.max(LOAD_MIN, endpoint.getLoadFactor()))
                    );
                    if (balancingSettings.policy == BalancingPolicy.USE_PREFERABLE_LOCATION
                            && !endpoint.getLocation().equals(preferredLocation)) {
                        loadFactor += LOCALITY_SHIFT;
                    }
                    newRecords.add(
                        new EndpointEntry(
                            new EndpointRecord(endpoint.getAddress(), endpoint.getPort()),
                            loadFactor
                        )
                    );
                }
                lastUpdateTime.set(Instant.now().toEpochMilli());
                removed = setNewState(newRecords);
            }
            future.complete(new EndpointUpdateResultData(removed, result.toStatus()));
            updateInProgress.set(false);
        }).exceptionally(e -> {
            future.complete(new EndpointUpdateResultData(null, Status.of(StatusCode.CLIENT_INTERNAL_ERROR)));
            updateInProgress.set(false);
            return null;
        });
        return new EndpointUpdateResult(future, true);
    }

    public EndpointRecord getEndpoint(String preferredEndpoint) {
        recordsLock.readLock().lock();
        try {
            if (!preferredEndpoint.isEmpty()) {
                EndpointEntry knownEndpoint = knownEndpoints.get(preferredEndpoint);
                if (knownEndpoint != null) {
                    return knownEndpoint.endpoint;
                }
            }
            if (bestEndpointsCount == -1) {
                assert records.isEmpty();
                return null;
            } else {
                // returns value in range [0, n)
                int idx = random.nextInt(bestEndpointsCount);
                return records.get(idx).endpoint;
            }
        } finally {
            recordsLock.readLock().unlock();
        }
    }

    public EndpointRecord getEndpoint() {
        return getEndpoint("");
    }

    public List<EndpointRecord> getRecords() {
        recordsLock.readLock().lock();
        try {
            return records
                    .stream()
                    .map(e -> e.endpoint)
                    .collect(Collectors.toList());
        } finally {
            recordsLock.readLock().unlock();
        }
    }

    public BalancingPolicy getBalancingPolicy() {
        return balancingSettings.policy;
    }

    public String getPreferredLocation(String selfLocation) {
        switch (balancingSettings.policy) {
            case USE_ALL_NODES:
                return null;
            case USE_PREFERABLE_LOCATION:
                if (balancingSettings.preferableLocation == null || balancingSettings.preferableLocation.isEmpty()) {
                    return selfLocation;
                } else {
                    return balancingSettings.preferableLocation;
                }
        }
        return "";
    }

    // Sets new endpoints, returns removed
    private List<String> setNewState(List<EndpointEntry> newRecords) {
        Set<String> index = new HashSet<String>();
        List<EndpointEntry> uniqueRecords = new ArrayList<>();
        for (EndpointEntry entry : newRecords) {
            if (index.add(entry.endpoint.getHostAndPort())) {
                uniqueRecords.add(entry);
            }
        }
        uniqueRecords.sort(Comparator.comparingInt(e -> e.priority));

        int newBestEndpointsCount = getBestEndpointsCount(uniqueRecords);
        if (logger.isDebugEnabled()) {
            logger.debug("setting new state with {} best endpoints of {}. Endpoints: {}",
                    newBestEndpointsCount,
                    newRecords.size(),
                    uniqueRecords
                            .stream()
                            .map(EndpointEntry::toString)
                            .collect(Collectors.joining(", "))
            );
        }

        List<String> removed = new ArrayList<>();

        // This method could not be called more than once simultaneously due to updateInProgress AtomicBoolean.
        // So there is no need to lock recordsLock until we change records
        Map<String, EndpointEntry> newKnownEndpoints = new HashMap<>(knownEndpoints);
        for (EndpointEntry record : records) {
            String hostWithPort = record.endpoint.getHostAndPort();
            if (!index.contains(hostWithPort)) {
                removed.add(hostWithPort);
                assert newKnownEndpoints.remove(hostWithPort) != null;
            }
        }

        for (EndpointEntry record : uniqueRecords) {
            newKnownEndpoints.put(record.endpoint.getHostAndPort(), record);
        }

        assert uniqueRecords.size() == newKnownEndpoints.size();

        recordsLock.writeLock().lock();
        try {
            records = uniqueRecords;
            knownEndpoints = newKnownEndpoints;
            bestEndpointsCount = newBestEndpointsCount;
        } finally {
            recordsLock.writeLock().unlock();
        }
        return removed;
    }

    // returns amount of endpoints with best load factor (priority)
    private static int getBestEndpointsCount(List<EndpointEntry> records) {
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
}
