package tech.ydb.topic.read.impl;

/**
 * @author Nikolay Perfilov
 */
public class OffsetsRange {
    private final long start;
    private final long end;

    public OffsetsRange(long start, long end) {
        this.start = start;
        this.end = end;
    }

    public long getStart() {
        return start;
    }

    public long getEnd() {
        return end;
    }
}
