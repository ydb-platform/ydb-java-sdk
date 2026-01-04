package tech.ydb.topic.read;

/**
 * @author Nikolay Perfilov
 */
public class PartitionSession {
    private final long id;
    private final long partitionId;
    private final String path;

    public PartitionSession(long id, long partitionId, String path) {
        this.id = id;
        this.partitionId = partitionId;
        this.path = path;
    }

    public long getId() {
        return id;
    }

    public long getPartitionId() {
        return partitionId;
    }

    public String getPath() {
        return path;
    }

    @Override
    public String toString() {
        return "Partition session " + id + " (partition " + partitionId + ") for " + path;
    }
}
