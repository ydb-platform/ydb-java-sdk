package tech.ydb.topic.read.impl;

import tech.ydb.topic.description.OffsetsRange;

/**
 * @author Nikolay Perfilov
 */
public class OffsetsRangeImpl implements OffsetsRange {
    private long start;
    private long end;

    public OffsetsRangeImpl(long start, long end) {
        this.start = start;
        this.end = end;
    }

    public OffsetsRangeImpl(OffsetsRange offsetsRange) {
        this.start = offsetsRange.getStart();
        this.end = offsetsRange.getEnd();
    }

    @Override
    public long getStart() {
        return start;
    }

    @Override
    public long getEnd() {
        return end;
    }

    public void setStart(long start) {
        this.start = start;
    }

    public void setEnd(long end) {
        this.end = end;
    }
}
