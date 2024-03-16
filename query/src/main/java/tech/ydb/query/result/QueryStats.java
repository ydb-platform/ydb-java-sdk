package tech.ydb.query.result;

import java.util.List;
import java.util.stream.Collectors;

import tech.ydb.proto.YdbQueryStats;

/**
 *
 * @author Aleksandr Gorshenin
 */
public class QueryStats {
    private final List<QueryPhase> queryPhases;
    private final Compilation compilationStats;

    private final String queryPlan;
    private final String queryAst;

    private final long processCpuTimeUs;
    private final long totalDurationUs;
    private final long totalCpuTimeUs;

    public QueryStats(YdbQueryStats.QueryStats stats) {
        this.queryPhases = stats.getQueryPhasesList().stream().map(QueryPhase::new).collect(Collectors.toList());
        this.compilationStats = new Compilation(stats.getCompilation());
        this.queryPlan = stats.getQueryPlan();
        this.queryAst = stats.getQueryAst();
        this.processCpuTimeUs = stats.getProcessCpuTimeUs();
        this.totalDurationUs = stats.getTotalDurationUs();
        this.totalCpuTimeUs = stats.getTotalCpuTimeUs();
    }

    public List<QueryPhase> getPhases() {
        return this.queryPhases;
    }

    public Compilation getComplilationStats() {
        return this.compilationStats;
    }

    public String getQueryPlan() {
        return this.queryPlan;
    }

    public String getQueryAst() {
        return this.queryAst;
    }

    public long getTotalDurationUs() {
        return this.totalDurationUs;
    }

    public long getTotalCpuTimeUs() {
        return this.totalCpuTimeUs;
    }

    public long getProcessCpuTimeUs() {
        return this.processCpuTimeUs;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("QueryStats{");
        sb.append("plan='").append(queryPlan).append("', ");
        sb.append("ast='").append(queryAst).append("', ");
        sb.append("totalDurationUs=").append(totalDurationUs).append(", ");
        sb.append("totalCpuTimeUs=").append(totalCpuTimeUs).append(", ");
        sb.append("processCpuTimeUs=").append(processCpuTimeUs).append(", ");
        sb.append("compilation=").append(compilationStats).append(", ");
        sb.append("phases=[");
        sb.append(queryPhases.stream().map(QueryPhase::toString).collect(Collectors.joining(", ")));
        sb.append("]}");
        return sb.toString();
    }

    public static class Compilation {
        private final boolean isFromCache;
        private final long durationUs;
        private final long cpuTimeUs;

        public Compilation(YdbQueryStats.CompilationStats stats) {
            this.isFromCache = stats.getFromCache();
            this.durationUs = stats.getDurationUs();
            this.cpuTimeUs = stats.getCpuTimeUs();
        }

        public long getDurationUs() {
            return this.durationUs;
        }

        public long getCpuTimeUs() {
            return this.cpuTimeUs;
        }

        public boolean isFromCache() {
            return this.isFromCache;
        }

        @Override
        public String toString() {
            return "Compilation{durationUs=" + durationUs + ", cpuTimeUs=" + cpuTimeUs + ", cache=" + isFromCache + "}";
        }
    }

    public static class QueryPhase {
        private final List<TableAccess> tableAccesses;
        private final long durationUs;
        private final long cpuTimeUs;
        private final long affectedShards;
        private final boolean isLiteralPhase;

        public QueryPhase(YdbQueryStats.QueryPhaseStats stats) {
            this.tableAccesses = stats.getTableAccessList().stream().map(TableAccess::new).collect(Collectors.toList());
            this.durationUs = stats.getDurationUs();
            this.cpuTimeUs = stats.getCpuTimeUs();
            this.affectedShards = stats.getAffectedShards();
            this.isLiteralPhase = stats.getLiteralPhase();
        }

        public long getDurationUs() {
            return this.durationUs;
        }

        public long getCpuTimeUs() {
            return this.cpuTimeUs;
        }

        public long getAffectedShards() {
            return this.affectedShards;
        }

        public boolean isLiteralPhase() {
            return this.isLiteralPhase;
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder("QueryPhase{");
            sb.append("durationUs=").append(durationUs);
            sb.append(", cpuTimeUs=").append(cpuTimeUs);
            sb.append(", affectedShards=").append(affectedShards);
            sb.append(", literal=").append(isLiteralPhase);
            sb.append(", tables=[");
            sb.append(tableAccesses.stream().map(TableAccess::toString).collect(Collectors.joining(", ")));
            sb.append("]}");
            return sb.toString();
        }
    }

    public static class TableAccess {
        private final String name;
        private final Operation reads;
        private final Operation updates;
        private final Operation deletes;
        private final long partitionsCount;

        public TableAccess(YdbQueryStats.TableAccessStats stats) {
            this.name = stats.getName();
            this.reads = new Operation(stats.getReads());
            this.updates = new Operation(stats.getUpdates());
            this.deletes = new Operation(stats.getDeletes());
            this.partitionsCount = stats.getPartitionsCount();
        }

        public String getTableName() {
            return this.name;
        }

        public long getPartitionsCount() {
            return this.partitionsCount;
        }

        public Operation getReads() {
            return reads;
        }

        public Operation getUpdates() {
            return updates;
        }

        public Operation getDeletes() {
            return deletes;
        }

        @Override
        public String toString() {
            return "TableAccess{name=" + name
                    + ", partitions=" + partitionsCount
                    + ", reads={rows=" + reads.rows + ", byte=" + reads.bytes + "}"
                    + ", updates={rows=" + updates.rows + ", byte=" + updates.bytes + "}"
                    + ", deletes={rows=" + deletes.rows + ", byte=" + deletes.bytes + "}"
                    + "}";
        }
    }

    public static class Operation {
        private final long rows;
        private final long bytes;

        public Operation(YdbQueryStats.OperationStats stats) {
            this.rows = stats.getRows();
            this.bytes = stats.getBytes();
        }

        public long getRows() {
            return this.rows;
        }

        public long getBytes() {
            return this.bytes;
        }

        @Override
        public String toString() {
            return "OperationStats{rows=" + rows + ", bytes=" + bytes + "}";
        }
    }
}
