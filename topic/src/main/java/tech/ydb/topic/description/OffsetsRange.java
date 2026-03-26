package tech.ydb.topic.description;

import tech.ydb.topic.read.impl.OffsetsRangeImpl;

/**
 * @author Nikolay Perfilov
 */
public interface OffsetsRange {
    long getStart();

    long getEnd();

    static OffsetsRange of(long start) {
        return new OffsetsRangeImpl(start, start + 1);
    }

    static OffsetsRange of(long start, long end) {
        return new OffsetsRangeImpl(start, end);
    }
}
