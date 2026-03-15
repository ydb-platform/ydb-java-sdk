package tech.ydb.topic.read;

import java.util.List;

import tech.ydb.topic.description.OffsetsRange;

/**
 * @author Nikolay Perfilov
 */
public class PartitionOffsets {
    private final PartitionSession partition;
    private final List<OffsetsRange> offsets;

    public PartitionOffsets(PartitionSession partition, List<OffsetsRange> offsets) {
        this.partition = partition;
        this.offsets = offsets;
    }

    public PartitionSession getPartitionSession() {
        return partition;
    }

    public List<OffsetsRange> getOffsets() {
        return offsets;
    }

}
