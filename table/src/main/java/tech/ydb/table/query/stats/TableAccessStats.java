package tech.ydb.table.query.stats;

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

    public static TableAccessStats getDefaultInstance() {
        return DEFAULT_INSTANCE;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean hasReads() {
        return this.reads != null;
    }

    public OperationStats getReads() {
        return this.reads == null ? OperationStats.getDefaultInstance() : this.reads;
    }

    public void setReads(OperationStats reads) {
        this.reads = reads;
    }

    public boolean hasUpdates() {
        return this.updates != null;
    }

    public OperationStats getUpdates() {
        return this.updates == null ? OperationStats.getDefaultInstance() : this.updates;
    }

    public void setUpdates(OperationStats updates) {
        this.updates = updates;
    }

    public boolean hasDeletes() {
        return this.deletes != null;
    }

    public OperationStats getDeletes() {
        return this.deletes == null ? OperationStats.getDefaultInstance() : this.deletes;
    }

    public void setDeletes(OperationStats deletes) {
        this.deletes = deletes;
    }

    public long getPartitionsCount() {
        return this.partitionsCount;
    }

    public void setPartitionsCount(long partitionsCount) {
        this.partitionsCount = partitionsCount;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        } else if (!(obj instanceof TableAccessStats)) {
            return super.equals(obj);
        } else {
            TableAccessStats other = (TableAccessStats) obj;
            if (!this.getName().equals(other.getName())) {
                return false;
            } else if (this.hasReads() != other.hasReads()) {
                return false;
            } else if (this.hasReads() && !this.getReads().equals(other.getReads())) {
                return false;
            } else if (this.hasUpdates() != other.hasUpdates()) {
                return false;
            } else if (this.hasUpdates() && !this.getUpdates().equals(other.getUpdates())) {
                return false;
            } else if (this.hasDeletes() != other.hasDeletes()) {
                return false;
            } else if (this.hasDeletes() && !this.getDeletes().equals(other.getDeletes())) {
                return false;
            } else {
                return this.getPartitionsCount() == other.getPartitionsCount();
            }
        }
    }

    @Override
    public int hashCode() {
        if (this.memoizedHashCode != 0) {
            return this.memoizedHashCode;
        } else {
            int hash = 41;
            hash = 37 * hash + 1;
            hash = 53 * hash + this.getName().hashCode();
            if (this.hasReads()) {
                hash = 37 * hash + 3;
                hash = 53 * hash + this.getReads().hashCode();
            }

            if (this.hasUpdates()) {
                hash = 37 * hash + 4;
                hash = 53 * hash + this.getUpdates().hashCode();
            }

            if (this.hasDeletes()) {
                hash = 37 * hash + 5;
                hash = 53 * hash + this.getDeletes().hashCode();
            }

            hash = 37 * hash + 6;
            hash = 53 * hash + Long.hashCode(this.getPartitionsCount());
            this.memoizedHashCode = hash;
            return hash;
        }
    }

    @Override
    public String toString() {
        return "TableAccessStats{" + "name='" + name + '\'' + ", reads=" + reads + ", updates=" + updates +
                ", deletes=" + deletes + ", partitionsCount=" + partitionsCount + '}';
    }
}
