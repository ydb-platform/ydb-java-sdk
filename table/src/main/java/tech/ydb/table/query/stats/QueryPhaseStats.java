package tech.ydb.table.query.stats;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public final class QueryPhaseStats implements Serializable {
    private static final long serialVersionUID = 0L;
    private static final QueryPhaseStats DEFAULT_INSTANCE = new QueryPhaseStats();
    private long durationUs;
    private List<TableAccessStats> tableAccess;
    private long cpuTimeUs;
    private long affectedShards;
    private boolean literalPhase;
    private int memoizedHashCode;

    private QueryPhaseStats() {
        this.tableAccess = Collections.emptyList();
    }

    public QueryPhaseStats(tech.ydb.proto.YdbQueryStats.QueryPhaseStats protoAutoGenQueryPhaseStats) {
        this.durationUs = protoAutoGenQueryPhaseStats.getDurationUs();
        this.tableAccess = protoAutoGenQueryPhaseStats.getTableAccessList().stream().map(TableAccessStats::new)
                .collect(Collectors.toList());
        this.cpuTimeUs = protoAutoGenQueryPhaseStats.getCpuTimeUs();
        this.affectedShards = protoAutoGenQueryPhaseStats.getAffectedShards();
        this.literalPhase = protoAutoGenQueryPhaseStats.getLiteralPhase();
    }

    public static QueryPhaseStats getDefaultInstance() {
        return DEFAULT_INSTANCE;
    }

    public long getDurationUs() {
        return this.durationUs;
    }

    public void setDurationUs(long durationUs) {
        this.durationUs = durationUs;
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

    public void setCpuTimeUs(long cpuTimeUs) {
        this.cpuTimeUs = cpuTimeUs;
    }

    public long getAffectedShards() {
        return this.affectedShards;
    }

    public void setAffectedShards(long affectedShards) {
        this.affectedShards = affectedShards;
    }

    public boolean getLiteralPhase() {
        return this.literalPhase;
    }

    public void setLiteralPhase(boolean literalPhase) {
        this.literalPhase = literalPhase;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        } else if (!(obj instanceof QueryPhaseStats)) {
            return super.equals(obj);
        } else {
            QueryPhaseStats other = (QueryPhaseStats) obj;
            if (this.getDurationUs() != other.getDurationUs()) {
                return false;
            } else if (!this.getTableAccessList().equals(other.getTableAccessList())) {
                return false;
            } else if (this.getCpuTimeUs() != other.getCpuTimeUs()) {
                return false;
            } else if (this.getAffectedShards() != other.getAffectedShards()) {
                return false;
            } else {
                return this.getLiteralPhase() == other.getLiteralPhase();
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
            hash = 53 * hash + Long.hashCode(this.getDurationUs());
            if (this.getTableAccessCount() > 0) {
                hash = 37 * hash + 2;
                hash = 53 * hash + this.getTableAccessList().hashCode();
            }

            hash = 37 * hash + 3;
            hash = 53 * hash + Long.hashCode(this.getCpuTimeUs());
            hash = 37 * hash + 4;
            hash = 53 * hash + Long.hashCode(this.getAffectedShards());
            hash = 37 * hash + 5;
            hash = 53 * hash + Boolean.hashCode(this.getLiteralPhase());
            this.memoizedHashCode = hash;
            return hash;
        }
    }

    public void setTableAccess(List<TableAccessStats> tableAccess) {
        this.tableAccess = tableAccess;
    }

    @Override
    public String toString() {
        return "QueryPhaseStats{" + "durationUs=" + durationUs + ", tableAccess=" + tableAccess + ", cpuTimeUs=" +
                cpuTimeUs + ", affectedShards=" + affectedShards + ", literalPhase=" + literalPhase + '}';
    }
}
