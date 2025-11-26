package tech.ydb.table.query.stats;

import java.util.Objects;

public final class TableAccessStats {
    private final String name;
    private final OperationStats reads;
    private final OperationStats updates;
    private final OperationStats deletes;
    private final long partitionsCount;

    public TableAccessStats(tech.ydb.proto.YdbQueryStats.TableAccessStats protoAutoGenTableAccessStats) {
        this(
                protoAutoGenTableAccessStats.getName(),
                new OperationStats(protoAutoGenTableAccessStats.getReads()),
                new OperationStats(protoAutoGenTableAccessStats.getUpdates()),
                new OperationStats(protoAutoGenTableAccessStats.getDeletes()),
                protoAutoGenTableAccessStats.getPartitionsCount()
        );
    }

    public TableAccessStats(
            String name,
            OperationStats reads,
            OperationStats updates,
            OperationStats deletes,
            long partitionsCount
    ) {
        this.name = name;
        this.reads = reads;
        this.updates = updates;
        this.deletes = deletes;
        this.partitionsCount = partitionsCount;
    }

    public String getName() {
        return name;
    }

    public OperationStats getReads() {
        return this.reads;
    }

    public OperationStats getUpdates() {
        return this.updates;
    }

    public OperationStats getDeletes() {
        return this.deletes;
    }

    public long getPartitionsCount() {
        return this.partitionsCount;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        } else if (!(obj instanceof TableAccessStats)) {
            return super.equals(obj);
        } else {
            TableAccessStats other = (TableAccessStats) obj;
            return Objects.equals(getName(), other.getName()) &&
                    Objects.equals(getReads(), other.getReads()) &&
                    Objects.equals(getUpdates(), other.getUpdates()) &&
                    Objects.equals(getDeletes(), other.getDeletes()) &&
                    Objects.equals(getPartitionsCount(), other.getPartitionsCount());
        }
    }

    @Override
    public int hashCode() {
        return Objects.hash(getName(), getReads(), getUpdates(), getDeletes(), getPartitionsCount());
    }

    @Override
    public String toString() {
        return "TableAccessStats{" + "name='" + name + '\'' + ", reads=" + reads + ", updates=" + updates +
                ", deletes=" + deletes + ", partitionsCount=" + partitionsCount + '}';
    }
}
