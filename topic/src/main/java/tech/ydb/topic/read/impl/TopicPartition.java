package tech.ydb.topic.read.impl;

/**
 *
 * @author Aleksandr Gorshenin {@literal <alexandr268@ydb.tech>}
 */
public class TopicPartition {
    private final String topicPath;
    private final long partitionId;

    public TopicPartition(String topicPath, long partitionId) {
        this.topicPath = topicPath;
        this.partitionId = partitionId;
    }

    public long getPartitionId() {
        return partitionId;
    }

    @Override
    public int hashCode() {
        return 31 * Long.hashCode(partitionId) + topicPath.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof TopicPartition)) {
            return false;
        }
        TopicPartition o = (TopicPartition) obj;
        return partitionId == o.partitionId && topicPath.equals(o.topicPath);
    }
}
