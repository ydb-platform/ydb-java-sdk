package tech.ydb.topic.write.impl.multi;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;

import tech.ydb.topic.description.PartitionInfo;
import tech.ydb.topic.settings.AutoPartitioningStrategy;
import tech.ydb.topic.settings.MultiWriterSettings.RoutingMode;
import tech.ydb.topic.write.impl.multi.PartitionLineage.Range;

/** Immutable, validated topic snapshot. Only a compatible successor can replace it. */
final class Topology {
    private final Map<Long, PartitionInfo> partitions;
    private final List<PartitionInfo> active = new ArrayList<>();
    private final List<Range> ranges = new ArrayList<>();
    private final RoutingMode routingMode;
    private final PartitionLineage lineage;

    Topology(List<PartitionInfo> description, RoutingMode requested, AutoPartitioningStrategy strategy) {
        Objects.requireNonNull(description, "partitions");
        Objects.requireNonNull(requested, "routing mode");
        Objects.requireNonNull(strategy, "auto-partitioning strategy");
        Map<Long, PartitionInfo> indexed = new TreeMap<>();
        for (PartitionInfo partition : description) {
            if (partition == null || partition.getPartitionId() < 0) {
                throw new IllegalArgumentException("Partition IDs must be nonnegative");
            }
            if (indexed.put(partition.getPartitionId(), partition) != null) {
                throw new IllegalArgumentException("Duplicate partition ID " + partition.getPartitionId());
            }
        }
        this.partitions = Collections.unmodifiableMap(indexed);
        for (PartitionInfo partition : indexed.values()) {
            if (partition.isActive()) {
                active.add(partition);
            }
        }
        if (active.isEmpty()) {
            throw new IllegalArgumentException("Topology has no active partitions");
        }
        boolean anyRange = active.stream().anyMatch(PartitionInfo::hasKeyRange);
        boolean autoPartitioned = strategy != AutoPartitioningStrategy.DISABLED;
        this.routingMode = requested == RoutingMode.AUTO
                ? (anyRange || autoPartitioned ? RoutingMode.BOUND : RoutingMode.HASH) : requested;
        this.lineage = new PartitionLineage(partitions, routingMode == RoutingMode.BOUND, autoPartitioned);
        if (routingMode == RoutingMode.BOUND) {
            for (PartitionInfo partition : active) {
                ranges.add(lineage.range(partition.getPartitionId()));
            }
            PartitionLineage.validateCoverage(ranges, Range.WHOLE);
        } else if (routingMode == RoutingMode.HASH && anyRange) {
            throw new IllegalArgumentException("Hash routing requires partitions without key ranges");
        }
    }

    RoutingMode getRoutingMode() {
        return routingMode;
    }

    Map<Long, PartitionInfo> getPartitions() {
        return partitions;
    }

    PartitionInfo get(long id) {
        return partitions.get(id);
    }

    long route(String key, Long explicitId) {
        if (routingMode == RoutingMode.EXPLICIT) {
            if (explicitId == null) {
                throw new IllegalArgumentException("Explicit routing requires a partition ID");
            }
            PartitionInfo selected = partitions.get(explicitId);
            if (selected == null || !selected.isActive()) {
                throw new IllegalArgumentException("Explicit partition is not active: " + explicitId);
            }
            return explicitId;
        }
        if (explicitId != null) {
            throw new IllegalArgumentException("Partition ID requires explicit routing");
        }
        Objects.requireNonNull(key, "key");
        if (routingMode == RoutingMode.BOUND) {
            return routeBound(KeyHash.boundKey(key));
        }
        return active.get(KeyHash.kafkaHash(key) % active.size()).getPartitionId();
    }

    byte[] partitionKey(String key) {
        return routingMode == RoutingMode.BOUND ? KeyHash.boundKey(Objects.requireNonNull(key, "key")) : null;
    }

    long routeBound(byte[] hashedKey) {
        if (routingMode != RoutingMode.BOUND) {
            throw new IllegalStateException("Bound routing is not selected");
        }
        int low = 0;
        int high = ranges.size();
        while (low < high) {
            int middle = (low + high) >>> 1;
            if (compareUnsigned(ranges.get(middle).from, hashedKey) <= 0) {
                low = middle + 1;
            } else {
                high = middle;
            }
        }
        return ranges.get(low - 1).id;
    }

    /** Includes the partition itself, then all ancestors in deterministic breadth-first order. */
    List<Long> ancestors(long id) {
        return lineage.ancestors(id);
    }

    boolean isDescendant(long child, long ancestor) {
        return partitions.containsKey(child) && ancestors(child).contains(ancestor);
    }

    boolean sameLayout(Topology other) {
        if (other == null || routingMode != other.routingMode
                || !partitions.keySet().equals(other.partitions.keySet())) {
            return false;
        }
        for (PartitionInfo partition : partitions.values()) {
            long id = partition.getPartitionId();
            PartitionInfo compared = other.get(id);
            if (partition.isActive() != compared.isActive() || !sameBounds(id, other)
                    || !sameIds(partition.getParentPartitionIds(), compared.getParentPartitionIds())
                    || !sameIds(partition.getChildPartitionIds(), compared.getChildPartitionIds())) {
                return false;
            }
        }
        return true;
    }

    void validateRefresh(Topology previous) {
        if (routingMode != previous.routingMode) {
            throw new IllegalArgumentException("Routing mode must not change during writer lifetime");
        }
        if (routingMode == RoutingMode.HASH && !activeIds().equals(previous.activeIds())) {
            throw new IllegalArgumentException("Hash partition set changed; ordering cannot be preserved");
        }
        for (PartitionInfo old : previous.partitions.values()) {
            long id = old.getPartitionId();
            PartitionInfo current = get(id);
            if (current == null) {
                throw new IllegalArgumentException("Previously known partition disappeared: " + id);
            }
            if (!sameBounds(id, previous)
                    || !sameIds(current.getParentPartitionIds(), old.getParentPartitionIds())
                    || (!old.isActive() && (current.isActive()
                    || !sameIds(current.getChildPartitionIds(), old.getChildPartitionIds())))) {
                throw new IllegalArgumentException("Incompatible partition history: " + id);
            }
        }
        for (long id : partitions.keySet()) {
            if (!previous.partitions.containsKey(id) && !inheritsFromActive(id, previous)) {
                throw new IllegalArgumentException("New partition has no previously active ancestor: " + id);
            }
        }
    }

    private boolean inheritsFromActive(long id, Topology previous) {
        for (long ancestor : ancestors(id)) {
            PartitionInfo old = previous.get(ancestor);
            if (old != null && old.isActive()) {
                return true;
            }
        }
        return false;
    }

    private Set<Long> activeIds() {
        Set<Long> ids = new HashSet<>();
        for (PartitionInfo partition : active) {
            ids.add(partition.getPartitionId());
        }
        return ids;
    }

    private boolean sameBounds(long id, Topology other) {
        Range left = lineage.range(id);
        Range right = other.lineage.range(id);
        if (left == null || right == null) {
            return left == right;
        }
        return Arrays.equals(left.from, right.from) && Arrays.equals(left.to, right.to);
    }

    private static boolean sameIds(List<Long> left, List<Long> right) {
        return left.size() == right.size() && new HashSet<>(left).equals(new HashSet<>(right));
    }

    static int compareUnsigned(byte[] left, byte[] right) {
        int count = Math.min(left.length, right.length);
        for (int index = 0; index < count; index++) {
            int difference = (left[index] & 0xff) - (right[index] & 0xff);
            if (difference != 0) {
                return difference;
            }
        }
        return Integer.compare(left.length, right.length);
    }
}
