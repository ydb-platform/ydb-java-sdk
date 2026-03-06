package tech.ydb.topic.read.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;
import java.util.concurrent.locks.ReentrantLock;

import tech.ydb.topic.description.OffsetsRange;

/**
 * @author Nikolay Perfilov
 */
public class DisjointOffsetRangeSet {
    private final NavigableMap<Long, OffsetsRangeImpl> ranges = new TreeMap<>();
    private final ReentrantLock rangesLock = new ReentrantLock();

    public void add(Collection<OffsetsRange> ranges) {
        rangesLock.lock();
        try {
            for (OffsetsRange range: ranges) {
                addImpl(range);
            }
        } finally {
            rangesLock.unlock();
        }
    }

    private void addImpl(OffsetsRange rangeToCommit) {
        Map.Entry<Long, OffsetsRangeImpl> floorEntry = ranges.floorEntry(rangeToCommit.getStart());
        if (floorEntry != null && floorEntry.getValue().getEnd() > rangeToCommit.getStart()) {
            throwClashesException(floorEntry.getValue(), rangeToCommit);
        }
        Map.Entry<Long, OffsetsRangeImpl> ceilingEntry = ranges.ceilingEntry(rangeToCommit.getStart());
        if (ceilingEntry != null && rangeToCommit.getEnd() > ceilingEntry.getValue().getStart()) {
            throwClashesException(ceilingEntry.getValue(), rangeToCommit);
        }
        boolean mergedFloor = false;
        if (floorEntry != null && floorEntry.getValue().getEnd() == rangeToCommit.getStart()) {
            floorEntry.getValue().setEnd(rangeToCommit.getEnd());
            mergedFloor = true;
        }
        if (ceilingEntry != null) {
            OffsetsRangeImpl ceilingValue = ceilingEntry.getValue();
            if (rangeToCommit.getEnd() == ceilingValue.getStart()) {
                ranges.remove(ceilingEntry.getKey());
                if (mergedFloor) {
                    floorEntry.getValue().setEnd(ceilingValue.getEnd());
                } else {
                    ceilingValue.setStart(rangeToCommit.getStart());
                    ranges.put(rangeToCommit.getStart(), ceilingValue);
                }
                return;
            }
        }
        if (!mergedFloor) {
            ranges.put(rangeToCommit.getStart(), new OffsetsRangeImpl(rangeToCommit));
        }
    }

    public List<OffsetsRange> getRangesAndClear() {
        rangesLock.lock();
        try {
            return new ArrayList<>(ranges.values());
        } finally {
            ranges.clear();
            rangesLock.unlock();
        }

    }

    private void throwClashesException(OffsetsRangeImpl existingRange, OffsetsRange newRange) {
        String errMessage = "Error adding new offset range. Added range [" +
                newRange.getStart() + "," + newRange.getEnd() + ") clashes with existing range [" +
                existingRange.getStart() + "," + existingRange.getEnd() + ")";
        throw new RuntimeException(errMessage);
    }
}
