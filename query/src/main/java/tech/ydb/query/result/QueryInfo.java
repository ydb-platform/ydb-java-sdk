package tech.ydb.query.result;

import tech.ydb.common.transaction.VirtualTimestamp;


/**
 *
 * @author Aleksandr Gorshenin
 */
public class QueryInfo {
    private final QueryStats stats;
    private final VirtualTimestamp commitTimestamp;
    private final VirtualTimestamp snapshotTimestamp;

    public QueryInfo(QueryStats stats) {
        this(stats, null, null);
    }

    public QueryInfo(QueryStats stats, VirtualTimestamp commitVt, VirtualTimestamp snapshotVt) {
        this.stats = stats;
        this.commitTimestamp = commitVt;
        this.snapshotTimestamp = snapshotVt;
    }

    public boolean hasStats() {
        return stats != null;
    }

    public boolean hasCommitTimestamp() {
        return commitTimestamp != null;
    }

    public boolean hasSnapshotTimestamp() {
        return snapshotTimestamp != null;
    }

    public QueryStats getStats() {
        return stats;
    }

    public VirtualTimestamp getCommitTimestamp() {
        return commitTimestamp;
    }

    public VirtualTimestamp getSnapshotTimestamp() {
        return snapshotTimestamp;
    }
}
