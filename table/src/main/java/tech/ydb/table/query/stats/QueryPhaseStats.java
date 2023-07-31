package tech.ydb.table.query.stats;

import java.io.Serializable;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public final class QueryPhaseStats implements Serializable {
    private final long durationUs;
    private final List<TableAccessStats> tableAccess;
    private final long cpuTimeUs;
    private final long affectedShards;
    private final boolean literalPhase;
    private int memoizedHashCode;

    public QueryPhaseStats(tech.ydb.proto.YdbQueryStats.QueryPhaseStats protoAutoGenQueryPhaseStats) {
        this.durationUs = protoAutoGenQueryPhaseStats.getDurationUs();
        this.tableAccess = protoAutoGenQueryPhaseStats.getTableAccessList().stream().map(TableAccessStats::new)
                .collect(Collectors.toList());
        this.cpuTimeUs = protoAutoGenQueryPhaseStats.getCpuTimeUs();
        this.affectedShards = protoAutoGenQueryPhaseStats.getAffectedShards();
        this.literalPhase = protoAutoGenQueryPhaseStats.getLiteralPhase();
    }

    public long getDurationUs() {
        return this.durationUs;
    }

    public List<TableAccessStats> getTableAccessList() {
        return this.tableAccess;
    }

    public int getTableAccessCount() {
        return this.tableAccess.size();
    }

    public TableAccessStats getTableAccess(int index) {
        return this.tableAccess.get(index);
    }

    public long getCpuTimeUs() {
        return this.cpuTimeUs;
    }

    public long getAffectedShards() {
        return this.affectedShards;
    }

    public boolean getLiteralPhase() {
        return this.literalPhase;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        } else if (!(obj instanceof QueryPhaseStats)) {
            return super.equals(obj);
        } else {
            QueryPhaseStats other = (QueryPhaseStats) obj;
            return Objects.equals(getDurationUs(), other.getDurationUs()) &&
                    Objects.equals(getTableAccessList(), other.getTableAccessList()) &&
                    Objects.equals(getCpuTimeUs(), other.getCpuTimeUs()) &&
                    Objects.equals(getAffectedShards(), other.getAffectedShards()) &&
                    Objects.equals(getLiteralPhase(), other.getLiteralPhase());
        }
    }

    @Override
    public int hashCode() {
        if (this.memoizedHashCode == 0) {
            this.memoizedHashCode =
                    Objects.hash(getDurationUs(), getTableAccessList(), getCpuTimeUs(), getAffectedShards(),
                            getLiteralPhase());
        }
        return this.memoizedHashCode;
    }

    @Override
    public String toString() {
        return "QueryPhaseStats{" + "durationUs=" + durationUs + ", tableAccess=" + tableAccess + ", cpuTimeUs=" +
                cpuTimeUs + ", affectedShards=" + affectedShards + ", literalPhase=" + literalPhase + '}';
    }
}
