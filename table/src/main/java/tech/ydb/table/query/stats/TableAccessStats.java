package tech.ydb.table.query.stats;

import java.util.Objects;

public final class TableAccessStats {
    private static final TableAccessStats DEFAULT_INSTANCE = new TableAccessStats();
    private String name;
    private OperationStats reads;
    private OperationStats updates;
    private OperationStats deletes;
    private long partitionsCount;
    private int memoizedHashCode;

    private TableAccessStats() {
    }

    public TableAccessStats(tech.ydb.proto.YdbQueryStats.TableAccessStats protoAutoGenTableAccessStats) {
        this.name = protoAutoGenTableAccessStats.getName();
        this.reads = new OperationStats(protoAutoGenTableAccessStats.getReads());
        this.updates = new OperationStats(protoAutoGenTableAccessStats.getUpdates());
        this.deletes = new OperationStats(protoAutoGenTableAccessStats.getDeletes());
        this.partitionsCount = protoAutoGenTableAccessStats.getPartitionsCount();
    }

    private static TableAccessStats getDefaultInstance() {
        return DEFAULT_INSTANCE;
    }

    public String getName() {
        return name;
    }

    public OperationStats getReads() {
        return this.reads == null ? OperationStats.getDefaultInstance() : this.reads;
    }

    public OperationStats getUpdates() {
        return this.updates == null ? OperationStats.getDefaultInstance() : this.updates;
    }

    public OperationStats getDeletes() {
        return this.deletes == null ? OperationStats.getDefaultInstance() : this.deletes;
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
            return Objects.equals(getName(), other.getName()) && Objects.equals(getReads(), other.getReads()) &&
                    Objects.equals(getUpdates(), other.getUpdates()) &&
                    Objects.equals(getDeletes(), other.getDeletes()) &&
                    Objects.equals(getPartitionsCount(), other.getPartitionsCount());
        }
    }

    @Override
    public int hashCode() {
        if (this.memoizedHashCode == 0) {
            this.memoizedHashCode =
                    Objects.hash(getName(), getReads(), getUpdates(), getDeletes(), getPartitionsCount());
        }
        return this.memoizedHashCode;
    }

    @Override
    public String toString() {
        return "TableAccessStats{" + "name='" + name + '\'' + ", reads=" + reads + ", updates=" + updates +
                ", deletes=" + deletes + ", partitionsCount=" + partitionsCount + '}';
    }
}
