package tech.ydb.table.query.stats;

import java.io.Serializable;
import java.util.Objects;

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

    public long getBytes() {
        return this.bytes;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        } else if (!(obj instanceof OperationStats)) {
            return super.equals(obj);
        } else {
            OperationStats other = (OperationStats) obj;
            return Objects.equals(getRows(), other.getRows()) && Objects.equals(getBytes(), other.getBytes());
        }
    }

    @Override
    public int hashCode() {
        if (this.memoizedHashCode == 0) {
            this.memoizedHashCode = Objects.hash(getRows(), getBytes());
        }
        return this.memoizedHashCode;
    }

    @Override
    public String toString() {
        return "OperationStats{" + "rows=" + rows + ", bytes=" + bytes + '}';
    }
}
