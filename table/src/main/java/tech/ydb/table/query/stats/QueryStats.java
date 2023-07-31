package tech.ydb.table.query.stats;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public final class QueryStats implements Serializable {
    private static final QueryStats DEFAULT_INSTANCE = new QueryStats();
    private final List<QueryPhaseStats> queryPhases;
    private CompilationStats compilation;
    private long processCpuTimeUs;
    private String queryPlan;
    private String queryAst;
    private long totalDurationUs;
    private long totalCpuTimeUs;
    private int memoizedHashCode;

    private QueryStats() {
        this.queryPhases = Collections.emptyList();
        this.queryPlan = "";
        this.queryAst = "";
    }

    public QueryStats(tech.ydb.proto.YdbQueryStats.QueryStats protoAutoGenQueryStats) {
        this.queryPhases = protoAutoGenQueryStats.getQueryPhasesList().stream().map(QueryPhaseStats::new)
                .collect(Collectors.toList());
        this.compilation = new CompilationStats(protoAutoGenQueryStats.getCompilation());
        this.processCpuTimeUs = protoAutoGenQueryStats.getProcessCpuTimeUs();
        this.queryPlan = protoAutoGenQueryStats.getQueryPlan();
        this.queryAst = protoAutoGenQueryStats.getQueryAst();
        this.totalDurationUs = protoAutoGenQueryStats.getTotalDurationUs();
        this.totalCpuTimeUs = protoAutoGenQueryStats.getProcessCpuTimeUs();
    }

    public static QueryStats getDefaultInstance() {
        return DEFAULT_INSTANCE;
    }

    public List<QueryPhaseStats> getQueryPhasesList() {
        return this.queryPhases;
    }

    public int getQueryPhasesCount() {
        return this.queryPhases.size();
    }

    public QueryPhaseStats getQueryPhases(int index) {
        return this.queryPhases.get(index);
    }

    public boolean hasCompilation() {
        return this.compilation != null;
    }

    public CompilationStats getCompilation() {
        return this.compilation == null ? CompilationStats.getDefaultInstance() : this.compilation;
    }

    public void setCompilation(CompilationStats compilation) {
        this.compilation = compilation;
    }

    public long getProcessCpuTimeUs() {
        return this.processCpuTimeUs;
    }

    public void setProcessCpuTimeUs(long processCpuTimeUs) {
        this.processCpuTimeUs = processCpuTimeUs;
    }

    public long getTotalDurationUs() {
        return this.totalDurationUs;
    }

    public void setTotalDurationUs(long totalDurationUs) {
        this.totalDurationUs = totalDurationUs;
    }

    public long getTotalCpuTimeUs() {
        return this.totalCpuTimeUs;
    }

    public void setTotalCpuTimeUs(long totalCpuTimeUs) {
        this.totalCpuTimeUs = totalCpuTimeUs;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        } else if (!(obj instanceof QueryStats)) {
            return super.equals(obj);
        } else {
            QueryStats other = (QueryStats) obj;
            if (!this.getQueryPhasesList().equals(other.getQueryPhasesList())) {
                return false;
            } else if (this.hasCompilation() != other.hasCompilation()) {
                return false;
            } else if (this.hasCompilation() && !this.getCompilation().equals(other.getCompilation())) {
                return false;
            } else if (this.getProcessCpuTimeUs() != other.getProcessCpuTimeUs()) {
                return false;
            } else if (!this.getQueryPlan().equals(other.getQueryPlan())) {
                return false;
            } else if (!this.getQueryAst().equals(other.getQueryAst())) {
                return false;
            } else if (this.getTotalDurationUs() != other.getTotalDurationUs()) {
                return false;
            } else {
                return this.getTotalCpuTimeUs() == other.getTotalCpuTimeUs();
            }
        }
    }

    @Override
    public int hashCode() {
        if (this.memoizedHashCode != 0) {
            return this.memoizedHashCode;
        } else {
            int hash = 41;
            if (this.getQueryPhasesCount() > 0) {
                hash = 37 * hash + 1;
                hash = 53 * hash + this.getQueryPhasesList().hashCode();
            }

            if (this.hasCompilation()) {
                hash = 37 * hash + 2;
                hash = 53 * hash + this.getCompilation().hashCode();
            }

            hash = 37 * hash + 3;
            hash = 53 * hash + Long.hashCode(this.getProcessCpuTimeUs());
            hash = 37 * hash + 4;
            hash = 53 * hash + this.getQueryPlan().hashCode();
            hash = 37 * hash + 5;
            hash = 53 * hash + this.getQueryAst().hashCode();
            hash = 37 * hash + 6;
            hash = 53 * hash + Long.hashCode(this.getTotalDurationUs());
            hash = 37 * hash + 7;
            hash = 53 * hash + Long.hashCode(this.getTotalCpuTimeUs());
            this.memoizedHashCode = hash;
            return hash;
        }
    }

    public String getQueryAst() {
        return queryAst;
    }

    public void setQueryAst(String queryAst) {
        this.queryAst = queryAst;
    }

    public String getQueryPlan() {
        return queryPlan;
    }

    public void setQueryPlan(String queryPlan) {
        this.queryPlan = queryPlan;
    }

    @Override
    public String toString() {
        return "QueryStats{" + "queryPhases=" + queryPhases + ", compilation=" + compilation + ", processCpuTimeUs=" +
                processCpuTimeUs + ", queryPlan='" + queryPlan + '\'' + ", queryAst='" + queryAst + '\'' +
                ", totalDurationUs=" + totalDurationUs + ", totalCpuTimeUs=" + totalCpuTimeUs + '}';
    }
}
