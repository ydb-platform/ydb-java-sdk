package tech.ydb.table.query.stats;

import java.io.Serializable;
import java.util.Objects;

public final class OperationStats implements Serializable {
    private final long rows;
    private final long bytes;
    private int memoizedHashCode;

    public OperationStats(tech.ydb.proto.YdbQueryStats.OperationStats protoAutoGenOperationStats) {
        this.rows = protoAutoGenOperationStats.getRows();
        this.bytes = protoAutoGenOperationStats.getBytes();
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
