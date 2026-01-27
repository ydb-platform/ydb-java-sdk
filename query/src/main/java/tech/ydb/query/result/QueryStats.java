package tech.ydb.query.result;

import java.util.List;
import java.util.Objects;
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

    public YdbQueryStats.QueryStats toProtobuf() {
        return YdbQueryStats.QueryStats.newBuilder()
                .setQueryAst(queryAst)
                .setQueryPlan(queryPlan)
                .setTotalCpuTimeUs(totalCpuTimeUs)
                .setTotalDurationUs(totalDurationUs)
                .setProcessCpuTimeUs(processCpuTimeUs)
                .setCompilation(compilationStats.toProtobuf())
                .addAllQueryPhases(queryPhases.stream().map(QueryPhase::toProtobuf).collect(Collectors.toList()))
                .build();
    }

    public List<QueryPhase> getPhases() {
        return this.queryPhases;
    }

    /*
     * @deprecated Use {{@link #getCompilationStats()}} instead
     */
    @Deprecated
    public Compilation getComplilationStats() {
        return this.compilationStats;
    }

    public Compilation getCompilationStats() {
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
    public int hashCode() {
        int hash = Objects.hash(queryPlan, queryAst, compilationStats, queryPhases);
        hash = 31 * hash + (int) (processCpuTimeUs ^ (processCpuTimeUs >>> 32));
        hash = 31 * hash + (int) (totalDurationUs ^ (totalDurationUs >>> 32));
        hash = 31 * hash + (int) (totalCpuTimeUs ^ (totalCpuTimeUs >>> 32));
        return hash;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (other == null || getClass() != other.getClass()) {
            return false;
        }

        QueryStats o = (QueryStats) other;
        return Objects.equals(queryPlan, o.queryPlan)
                && Objects.equals(queryAst, o.queryAst)
                && Objects.equals(compilationStats, o.compilationStats)
                && Objects.equals(queryPhases, o.queryPhases)
                && processCpuTimeUs == o.processCpuTimeUs
                && totalDurationUs == o.totalDurationUs
                && totalCpuTimeUs == o.totalCpuTimeUs;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("QueryStats{");
        sb.append("\n\tplan='").append(queryPlan).append("',");
        sb.append("\n\tast='").append(queryAst).append("',");
        sb.append("\n\ttotalDurationUs=").append(totalDurationUs).append(",");
        sb.append("\n\ttotalCpuTimeUs=").append(totalCpuTimeUs).append(",");
        sb.append("\n\tprocessCpuTimeUs=").append(processCpuTimeUs).append(",");
        sb.append("\n\tcompilation=").append(compilationStats).append(",");
        sb.append("\n\tphases=[");
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

        public YdbQueryStats.CompilationStats toProtobuf() {
            return YdbQueryStats.CompilationStats.newBuilder()
                    .setCpuTimeUs(cpuTimeUs)
                    .setDurationUs(durationUs)
                    .setFromCache(isFromCache)
                    .build();
        }

        @Override
        public String toString() {
            return "Compilation{durationUs=" + durationUs + ", cpuTimeUs=" + cpuTimeUs + ", cache=" + isFromCache + "}";
        }

        @Override
        public int hashCode() {
            int hash = Boolean.hashCode(isFromCache);
            hash = 31 * hash + (int) (durationUs ^ (durationUs >>> 32));
            hash = 31 * hash + (int) (cpuTimeUs ^ (cpuTimeUs >>> 32));
            return hash;
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) {
                return true;
            }
            if (other == null || getClass() != other.getClass()) {
                return false;
            }

            Compilation o = (Compilation) other;
            return isFromCache == o.isFromCache && durationUs == o.durationUs && cpuTimeUs == o.cpuTimeUs;
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

        public List<TableAccess> getTableAccesses() {
            return this.tableAccesses;
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

        public YdbQueryStats.QueryPhaseStats toProtobuf() {
            return YdbQueryStats.QueryPhaseStats.newBuilder()
                    .setAffectedShards(affectedShards)
                    .setCpuTimeUs(cpuTimeUs)
                    .setDurationUs(durationUs)
                    .setLiteralPhase(isLiteralPhase)
                    .addAllTableAccess(tableAccesses.stream().map(TableAccess::toProtobuf).collect(Collectors.toList()))
                    .build();
        }

        @Override
        public int hashCode() {
            int hash = Boolean.hashCode(isLiteralPhase);
            hash = 31 * hash + (int) (durationUs ^ (durationUs >>> 32));
            hash = 31 * hash + (int) (cpuTimeUs ^ (cpuTimeUs >>> 32));
            hash = 31 * hash + (int) (affectedShards ^ (affectedShards >>> 32));
            hash = 31 * hash + tableAccesses.hashCode();
            return hash;
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) {
                return true;
            }
            if (other == null || getClass() != other.getClass()) {
                return false;
            }

            QueryPhase o = (QueryPhase) other;
            return durationUs == o.durationUs && cpuTimeUs == o.cpuTimeUs && affectedShards == o.affectedShards
                    && isLiteralPhase == o.isLiteralPhase && Objects.equals(tableAccesses, o.tableAccesses);
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

        public YdbQueryStats.TableAccessStats toProtobuf() {
            return YdbQueryStats.TableAccessStats.newBuilder()
                    .setName(name)
                    .setPartitionsCount(partitionsCount)
                    .setReads(reads.toProtobuf())
                    .setDeletes(deletes.toProtobuf())
                    .setUpdates(updates.toProtobuf())
                    .build();
        }

        @Override
        public int hashCode() {
            int hash = Objects.hash(name, reads, updates, deletes);
            hash = 31 * hash + (int) (partitionsCount ^ (partitionsCount >>> 32));
            return hash;
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) {
                return true;
            }
            if (other == null || getClass() != other.getClass()) {
                return false;
            }

            TableAccess o = (TableAccess) other;
            return Objects.equals(name, o.name)
                    && Objects.equals(reads, o.reads)
                    && Objects.equals(updates, o.updates)
                    && Objects.equals(deletes, o.deletes)
                    && partitionsCount == o.partitionsCount;
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

        public YdbQueryStats.OperationStats toProtobuf() {
            return YdbQueryStats.OperationStats.newBuilder().setRows(rows).setBytes(bytes).build();
        }

        @Override
        public int hashCode() {
            int hash = 31 + (int) (rows ^ (rows >>> 32));
            hash = 31 * hash + (int) (bytes ^ (bytes >>> 32));
            return hash;
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) {
                return true;
            }
            if (other == null || getClass() != other.getClass()) {
                return false;
            }

            Operation o = (Operation) other;
            return rows == o.rows && bytes == o.bytes;
        }
    }
}
