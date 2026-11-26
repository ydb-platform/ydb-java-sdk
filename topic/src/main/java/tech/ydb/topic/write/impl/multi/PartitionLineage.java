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
import java.util.Set;

import tech.ydb.topic.description.PartitionInfo;

/** Validates reciprocal split edges, cyclic ancestry and complete coverage of retired ranges. */
final class PartitionLineage {
    private final Map<Long, PartitionInfo> partitions;
    private final Map<Long, Range> ranges = new HashMap<>();

    PartitionLineage(Map<Long, PartitionInfo> partitions, boolean bounded, boolean autoPartitioned) {
        this.partitions = partitions;
        long roots = partitions.values().stream().filter(info -> info.getParentPartitionIds().isEmpty()).count();
        for (PartitionInfo partition : partitions.values()) {
            long id = partition.getPartitionId();
            if (partition.hasKeyRange()) {
                ranges.put(id, new Range(id, partition.getFromBound(), partition.getToBound()));
            } else if (bounded) {
                if (!autoPartitioned || roots != 1 || !partition.getParentPartitionIds().isEmpty()) {
                    throw new IllegalArgumentException("Missing key range for partition " + id);
                }
                // An auto-partitioned topic can omit its original whole-keyspace range.
                ranges.put(id, new Range(id, Range.WHOLE.from, Range.WHOLE.to));
            }
        }
        validate();
    }

    Range range(long id) {
        return ranges.get(id);
    }

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

    private void validate() {
        Map<Long, Integer> indegrees = new HashMap<>();
        Deque<Long> roots = new ArrayDeque<>();
        for (PartitionInfo partition : partitions.values()) {
            long id = partition.getPartitionId();
            List<Long> children = partition.getChildPartitionIds();
            List<Long> parents = partition.getParentPartitionIds();
            validateIds(children, id);
            validateIds(parents, id);
            if (partition.isActive() != children.isEmpty()) {
                throw new IllegalArgumentException("Incomplete or inconsistent split state for partition " + id);
            }
            validateEdges(id, parents, children);
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

    private void validateEdges(long id, List<Long> parents, List<Long> children) {
        Range parentRange = ranges.get(id);
        List<Range> childRanges = new ArrayList<>();
        for (long child : children) {
            if (!partitions.get(child).getParentPartitionIds().contains(id)) {
                throw new IllegalArgumentException("Nonreciprocal parent/child relationship");
            }
            if (parentRange != null) {
                Range childRange = ranges.get(child);
                if (childRange == null) {
                    throw new IllegalArgumentException("Split child is missing its key-range metadata");
                }
                childRanges.add(childRange);
            }
        }
        if (!childRanges.isEmpty()) {
            validateCoverage(childRanges, parentRange);
        }
        for (long parent : parents) {
            PartitionInfo info = partitions.get(parent);
            if (info.isActive() || !info.getChildPartitionIds().contains(id)) {
                throw new IllegalArgumentException("Nonreciprocal or active parent relationship");
            }
        }
    }

    static void validateCoverage(List<Range> ranges, Range expected) {
        ranges.sort((left, right) -> Topology.compareUnsigned(left.from, right.from));
        if (!Arrays.equals(expected.from, ranges.get(0).from)
                || !Arrays.equals(expected.to, ranges.get(ranges.size() - 1).to)) {
            throw new IllegalArgumentException("Key ranges do not cover both expected endpoints");
        }
        for (int index = 1; index < ranges.size(); index++) {
            Range left = ranges.get(index - 1);
            Range right = ranges.get(index);
            if (left.to.length == 0 || right.from.length == 0 || !Arrays.equals(left.to, right.from)) {
                throw new IllegalArgumentException("Key ranges contain a gap or overlap");
            }
        }
    }

    static final class Range {
        static final Range WHOLE = new Range(-1, new byte[0], new byte[0]);

        final long id;
        final byte[] from;
        final byte[] to;

        Range(long id, byte[] from, byte[] to) {
            if (from.length > 0 && to.length > 0 && Topology.compareUnsigned(from, to) >= 0) {
                throw new IllegalArgumentException("Empty or inverted key range for partition " + id);
            }
            this.id = id;
            this.from = from;
            this.to = to;
        }
    }
}
