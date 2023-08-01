package tech.ydb.table.query.stats;

import java.io.Serializable;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public final class QueryStats implements Serializable {
    private final List<QueryPhaseStats> queryPhases;
    private final CompilationStats compilation;
    private final long processCpuTimeUs;
    private final String queryPlan;
    private final String queryAst;
    private final long totalDurationUs;
    private final long totalCpuTimeUs;

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
        return this.compilation;
    }

    public long getProcessCpuTimeUs() {
        return this.processCpuTimeUs;
    }

    public long getTotalDurationUs() {
        return this.totalDurationUs;
    }

    public long getTotalCpuTimeUs() {
        return this.totalCpuTimeUs;
    }

    public String getQueryAst() {
        return queryAst;
    }

    public String getQueryPlan() {
        return queryPlan;
    }


    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        } else if (!(obj instanceof QueryStats)) {
            return super.equals(obj);
        } else {
            QueryStats other = (QueryStats) obj;
            return Objects.equals(getQueryPhasesList(), other.getQueryPhasesList()) &&
                    Objects.equals(getCompilation(), other.getCompilation()) &&
                    Objects.equals(getProcessCpuTimeUs(), other.getProcessCpuTimeUs()) &&
                    Objects.equals(getQueryPlan(), other.getQueryPlan()) &&
                    Objects.equals(getQueryAst(), other.getQueryAst()) &&
                    Objects.equals(getTotalDurationUs(), other.getTotalDurationUs()) &&
                    Objects.equals(getTotalCpuTimeUs(), other.getTotalCpuTimeUs());
        }
    }

    @Override
    public int hashCode() {
        return Objects.hash(getQueryPhasesList(), getCompilation(), getProcessCpuTimeUs(), getQueryPlan(),
                getQueryAst(), getTotalDurationUs(), getTotalCpuTimeUs());
    }

    @Override
    public String toString() {
        return "QueryStats{" + "queryPhases=" + queryPhases + ", compilation=" + compilation + ", processCpuTimeUs=" +
                processCpuTimeUs + ", queryPlan='" + queryPlan + '\'' + ", queryAst='" + queryAst + '\'' +
                ", totalDurationUs=" + totalDurationUs + ", totalCpuTimeUs=" + totalCpuTimeUs + '}';
    }
}
