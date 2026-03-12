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
    public int hashCode() {
        return (path.hashCode() * 31 + (int) id) * 31 + (int) partitionId;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof PartitionSession)) {
            return false;
        }
        PartitionSession o = (PartitionSession) obj;
        return id == o.id && partitionId == o.partitionId && path.equals(o.path);
    }

    @Override
    public String toString() {
        return "Partition session " + id + " (partition " + partitionId + ") for topic \"" + path + "\"";
    }
}
