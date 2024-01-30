package tech.ydb.topic.read;

import java.util.List;

import tech.ydb.topic.description.OffsetsRange;

/**
 * @author Nikolay Perfilov
 */
public class PartitionOffsets {
    private final PartitionSession partitionSession;
    private final List<OffsetsRange> offsets;

    public PartitionOffsets(PartitionSession partitionSession, List<OffsetsRange> offsets) {
        this.partitionSession = partitionSession;
        this.offsets = offsets;
    }

    public PartitionSession getPartitionSession() {
        return partitionSession;
    }

    public List<OffsetsRange> getOffsets() {
        return offsets;
    }

}
