package tech.ydb.table.query.stats;

import java.io.Serializable;

public final class OperationStats implements Serializable {
    private static final OperationStats DEFAULT_INSTANCE = new OperationStats();
    private long rows;
    private long bytes;
    private int memoizedHashCode;

    private OperationStats() {
    }
    public OperationStats(tech.ydb.proto.YdbQueryStats.OperationStats protoAutoGenOperationStats) {
        this.rows = protoAutoGenOperationStats.getRows();
        this.bytes = protoAutoGenOperationStats.getBytes();
    }

    public static OperationStats getDefaultInstance() {
        return DEFAULT_INSTANCE;
    }

    public long getRows() {
        return this.rows;
    }

    public void setRows(long rows) {
        this.rows = rows;
    }

    public long getBytes() {
        return this.bytes;
    }

    public void setBytes(long bytes) {
        this.bytes = bytes;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        } else if (!(obj instanceof OperationStats)) {
            return super.equals(obj);
        } else {
            OperationStats other = (OperationStats) obj;
            if (this.getRows() != other.getRows()) {
                return false;
            } else {
                return this.getBytes() == other.getBytes();
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
            hash = 53 * hash + Long.hashCode(this.getRows());
            hash = 37 * hash + 2;
            hash = 53 * hash + Long.hashCode(this.getBytes());
            this.memoizedHashCode = hash;
            return hash;
        }
    }

    @Override
    public String toString() {
        return "OperationStats{" + "rows=" + rows + ", bytes=" + bytes + '}';
    }
}
