package tech.ydb.topic.description;

import tech.ydb.proto.topic.YdbTopic;

/**
 *
 * @author Aleksandr Gorshenin
 */
public class PartitionLocation {
    private final int nodeId;
    private final long generation;

    public PartitionLocation(YdbTopic.PartitionLocation location) {
        this.nodeId = location.getNodeId();
        this.generation = location.getGeneration();
    }

    public int getNodeId() {
        return nodeId;
    }

    public long getGeneration() {
        return generation;
    }

    @Override
    public String toString() {
        return "PartitionLocation{nodeId=" + nodeId + ", generation=" + generation + "}";
    }
}
