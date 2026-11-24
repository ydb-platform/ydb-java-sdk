package tech.ydb.topic.write.impl.multi;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;

import tech.ydb.topic.description.PartitionInfo;
import tech.ydb.topic.settings.MultiWriterSettings.RoutingMode;

/** An immutable, validated describe snapshot. Incomplete split snapshots must be refreshed. */
final class Topology {
    private final Map<Long, PartitionInfo> partitions;
    private final List<PartitionInfo> active;
    private final List<Range> ranges;
    private final RoutingMode routingMode;

    Topology(List<PartitionInfo> description, RoutingMode requested) {
        Objects.requireNonNull(description, "partitions");
        Objects.requireNonNull(requested, "routing mode");
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
        this.active = new ArrayList<>();
        for (PartitionInfo partition : indexed.values()) {
            if (partition.isActive()) {
                active.add(partition);
            }
        }
        if (active.isEmpty()) {
            throw new IllegalArgumentException("Topology has no active partitions");
        }
        validateGraph();
        boolean anyRange = active.stream().anyMatch(PartitionInfo::hasKeyRange);
        this.routingMode = requested == RoutingMode.AUTO
                ? (anyRange ? RoutingMode.BOUND : RoutingMode.HASH) : requested;
        this.ranges = new ArrayList<>();
        if (routingMode == RoutingMode.BOUND) {
            for (PartitionInfo partition : active) {
                if (!partition.hasKeyRange()) {
                    throw new IllegalArgumentException("Missing active partition key range");
                }
                ranges.add(new Range(partition));
            }
            ranges.sort((left, right) -> compareUnsigned(left.from, right.from));
            validateRanges();
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
        if (!partitions.containsKey(id)) {
            throw new IllegalArgumentException("Unknown partition " + id);
        }
        Set<Long> visited = new LinkedHashSet<>();
        Deque<Long> remaining = new ArrayDeque<>();
        remaining.add(id);
        while (!remaining.isEmpty()) {
            long next = remaining.removeFirst();
            if (visited.add(next)) {
                List<Long> parents = new ArrayList<>(partitions.get(next).getParentPartitionIds());
                Collections.sort(parents);
                remaining.addAll(parents);
            }
        }
        return Collections.unmodifiableList(new ArrayList<>(visited));
    }

    /** A partition is also considered a descendant of itself. */
    boolean isDescendant(long child, long ancestor) {
        return partitions.containsKey(child) && ancestors(child).contains(ancestor);
    }

    boolean sameLayout(Topology other) {
        if (other == null || routingMode != other.routingMode
                || !partitions.keySet().equals(other.partitions.keySet())) {
            return false;
        }
        for (PartitionInfo partition : partitions.values()) {
            PartitionInfo compared = other.get(partition.getPartitionId());
            if (partition.isActive() != compared.isActive() || !sameBounds(partition, compared)
                    || !sameIds(partition.getParentPartitionIds(), compared.getParentPartitionIds())
                    || !sameIds(partition.getChildPartitionIds(), compared.getChildPartitionIds())) {
                return false;
            }
        }
        return true;
    }

    /** Validate this successor against the writer's previously accepted snapshot. */
    void validateRefresh(Topology previous) {
        if (routingMode != previous.routingMode) {
            throw new IllegalArgumentException("Routing mode must not change during writer lifetime");
        }
        if (routingMode == RoutingMode.HASH && !activeIds().equals(previous.activeIds())) {
            throw new IllegalArgumentException("Hash partition set changed; ordering cannot be preserved");
        }
        for (PartitionInfo old : previous.partitions.values()) {
            PartitionInfo current = get(old.getPartitionId());
            if (current == null) {
                throw new IllegalArgumentException("Previously known partition disappeared: " + old.getPartitionId());
            }
            if (!sameBounds(current, old)
                    || !sameIds(current.getParentPartitionIds(), old.getParentPartitionIds())
                    || (!old.isActive() && (current.isActive()
                    || !sameIds(current.getChildPartitionIds(), old.getChildPartitionIds())))) {
                throw new IllegalArgumentException("Incompatible partition history: " + old.getPartitionId());
            }
        }
        for (PartitionInfo current : partitions.values()) {
            if (!previous.partitions.containsKey(current.getPartitionId())) {
                boolean inherited = false;
                for (long ancestor : ancestors(current.getPartitionId())) {
                    PartitionInfo old = previous.get(ancestor);
                    if (old != null && old.isActive()) {
                        inherited = true;
                        break;
                    }
                }
                if (!inherited) {
                    throw new IllegalArgumentException("New partition has no previously active ancestor");
                }
            }
        }
    }

    private Set<Long> activeIds() {
        Set<Long> ids = new HashSet<>();
        for (PartitionInfo partition : active) {
            ids.add(partition.getPartitionId());
        }
        return ids;
    }

    private void validateGraph() {
        Map<Long, Integer> indegrees = new HashMap<>();
        Deque<Long> roots = new ArrayDeque<>();
        for (PartitionInfo partition : partitions.values()) {
            long id = partition.getPartitionId();
            List<Long> children = partition.getChildPartitionIds();
            List<Long> parents = partition.getParentPartitionIds();
            validateIds(children, id);
            validateIds(parents, id);
            if ((partition.isActive() && !children.isEmpty()) || (!partition.isActive() && children.isEmpty())) {
                throw new IllegalArgumentException("Incomplete or inconsistent split state for partition " + id);
            }
            for (long child : children) {
                PartitionInfo childInfo = partitions.get(child);
                if (!childInfo.getParentPartitionIds().contains(id)) {
                    throw new IllegalArgumentException("Nonreciprocal parent/child relationship");
                }
                validateContainedRange(partition, childInfo);
            }
            validateChildCoverage(partition);
            for (long parent : parents) {
                PartitionInfo parentInfo = partitions.get(parent);
                if (parentInfo.isActive() || !parentInfo.getChildPartitionIds().contains(id)) {
                    throw new IllegalArgumentException("Nonreciprocal or active parent relationship");
                }
            }
            indegrees.put(id, parents.size());
            if (parents.isEmpty()) {
                roots.add(id);
            }
        }
        int visited = 0;
        while (!roots.isEmpty()) {
            long id = roots.removeFirst();
            visited++;
            for (long child : partitions.get(id).getChildPartitionIds()) {
                int degree = indegrees.get(child) - 1;
                indegrees.put(child, degree);
                if (degree == 0) {
                    roots.add(child);
                }
            }
        }
        if (visited != partitions.size()) {
            throw new IllegalArgumentException("Partition lineage contains a cycle");
        }
    }

    private void validateIds(List<Long> ids, long owner) {
        Set<Long> distinct = new HashSet<>();
        for (Long id : ids) {
            if (id == null || id < 0 || id == owner || !partitions.containsKey(id) || !distinct.add(id)) {
                throw new IllegalArgumentException("Invalid or missing lineage partition for " + owner);
            }
        }
    }

    private void validateRanges() {
        if (ranges.get(0).from.length != 0 || ranges.get(ranges.size() - 1).to.length != 0) {
            throw new IllegalArgumentException("Key ranges do not cover both unbounded endpoints");
        }
        for (int index = 0; index < ranges.size(); index++) {
            Range current = ranges.get(index);
            if (current.from.length > 0 && current.to.length > 0
                    && compareUnsigned(current.from, current.to) >= 0) {
                throw new IllegalArgumentException("Empty or inverted key range");
            }
            if (index > 0) {
                Range previous = ranges.get(index - 1);
                if (previous.to.length == 0 || current.from.length == 0
                        || !Arrays.equals(previous.to, current.from)) {
                    throw new IllegalArgumentException("Key ranges contain a gap or overlap");
                }
            }
        }
    }

    private void validateChildCoverage(PartitionInfo parent) {
        if (!parent.hasKeyRange() || parent.getChildPartitionIds().isEmpty()) {
            return;
        }
        List<Range> children = new ArrayList<>();
        for (long child : parent.getChildPartitionIds()) {
            PartitionInfo childInfo = partitions.get(child);
            if (!childInfo.hasKeyRange()) {
                throw new IllegalArgumentException("Split child is missing its parent key-range metadata");
            }
            children.add(new Range(childInfo));
        }
        children.sort((left, right) -> compareUnsigned(left.from, right.from));
        if (!Arrays.equals(parent.getFromBound(), children.get(0).from)
                || !Arrays.equals(parent.getToBound(), children.get(children.size() - 1).to)) {
            throw new IllegalArgumentException("Split children do not cover the parent key range");
        }
        for (int index = 1; index < children.size(); index++) {
            Range left = children.get(index - 1);
            Range right = children.get(index);
            if (left.to.length == 0 || right.from.length == 0 || !Arrays.equals(left.to, right.from)) {
                throw new IllegalArgumentException("Split children contain a gap or overlap");
            }
        }
    }

    private static void validateContainedRange(PartitionInfo parent, PartitionInfo child) {
        if (!parent.hasKeyRange() || !child.hasKeyRange()) {
            return;
        }
        byte[] parentFrom = parent.getFromBound();
        byte[] parentTo = parent.getToBound();
        byte[] childFrom = child.getFromBound();
        byte[] childTo = child.getToBound();
        if ((parentFrom.length > 0 && (childFrom.length == 0 || compareUnsigned(childFrom, parentFrom) < 0))
                || (parentTo.length > 0 && (childTo.length == 0 || compareUnsigned(childTo, parentTo) > 0))) {
            throw new IllegalArgumentException("Child key range is outside its parent");
        }
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

    private static boolean sameBounds(PartitionInfo left, PartitionInfo right) {
        return left.hasKeyRange() == right.hasKeyRange()
                && Arrays.equals(left.getFromBound(), right.getFromBound())
                && Arrays.equals(left.getToBound(), right.getToBound());
    }

    private static boolean sameIds(List<Long> left, List<Long> right) {
        return left.size() == right.size() && new HashSet<>(left).equals(new HashSet<>(right));
    }

    private static final class Range {
        private final long id;
        private final byte[] from;
        private final byte[] to;

        private Range(PartitionInfo partition) {
            this.id = partition.getPartitionId();
            this.from = partition.getFromBound();
            this.to = partition.getToBound();
        }
    }
}
