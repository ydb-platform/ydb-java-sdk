package tech.ydb.table.query;

import java.util.List;
import java.util.stream.Collectors;

import tech.ydb.proto.ValueProtos;
import tech.ydb.proto.YdbQueryStats;
import tech.ydb.proto.table.YdbTable;
import tech.ydb.table.query.stats.QueryStats;
import tech.ydb.table.result.ResultSetReader;
import tech.ydb.table.result.impl.ProtoValueReaders;

/**
 * @author Sergey Polovko
 */
public class DataQueryResult {

    private final String txId;
    private final List<ValueProtos.ResultSet> resultSets;
    private final YdbQueryStats.QueryStats queryStats;

    public DataQueryResult(YdbTable.ExecuteQueryResult result) {
        this.txId = result.getTxMeta().getId();
        this.resultSets = result.getResultSetsList();
        this.queryStats = result.hasQueryStats() ? result.getQueryStats() : null;
    }

    public DataQueryResult(String txId, List<ValueProtos.ResultSet> results, YdbQueryStats.QueryStats stats) {
        this.txId = txId;
        this.resultSets = results;
        this.queryStats = stats;
    }

    @Deprecated
    public DataQueryResult(String txId, List<ValueProtos.ResultSet> results, QueryStats stats) {
        this.txId = txId;
        this.resultSets = results;
        this.queryStats = YdbQueryStats.QueryStats.newBuilder()
                .setQueryAst(stats.getQueryAst())
                .setQueryPlan(stats.getQueryPlan())
                .setTotalCpuTimeUs(stats.getTotalCpuTimeUs())
                .setTotalDurationUs(stats.getTotalDurationUs())
                .setProcessCpuTimeUs(stats.getProcessCpuTimeUs())
                .setCompilation(YdbQueryStats.CompilationStats.newBuilder()
                        .setCpuTimeUs(stats.getCompilation().getCpuTimeUs())
                        .setDurationUs(stats.getCompilation().getDurationUs())
                        .setFromCache(stats.getCompilation().getFromCache())
                        .build())
                .addAllQueryPhases(stats.getQueryPhasesList().stream().map(
                        phase -> YdbQueryStats.QueryPhaseStats.newBuilder()
                                .setAffectedShards(phase.getAffectedShards())
                                .setCpuTimeUs(phase.getCpuTimeUs())
                                .setDurationUs(phase.getDurationUs())
                                .setLiteralPhase(phase.getLiteralPhase())
                                .addAllTableAccess(phase.getTableAccessList().stream().map(
                                        table -> YdbQueryStats.TableAccessStats.newBuilder()
                                                .setName(table.getName())
                                                .setPartitionsCount(table.getPartitionsCount())
                                                .setReads(YdbQueryStats.OperationStats.newBuilder()
                                                        .setRows(table.getReads().getRows())
                                                        .setBytes(table.getReads().getBytes())
                                                        .build())
                                                .setDeletes(YdbQueryStats.OperationStats.newBuilder()
                                                        .setRows(table.getDeletes().getRows())
                                                        .setBytes(table.getDeletes().getBytes())
                                                        .build())
                                                .setUpdates(YdbQueryStats.OperationStats.newBuilder()
                                                        .setRows(table.getUpdates().getRows())
                                                        .setBytes(table.getUpdates().getBytes())
                                                        .build())
                                                .build()).collect(Collectors.toList()))
                                .build()).collect(Collectors.toList()))
                .build();
    }

    public String getTxId() {
        return txId;
    }

    public int getResultSetCount() {
        return resultSets.size();
    }

    public ResultSetReader getResultSet(int index) {
        return ProtoValueReaders.forResultSet(resultSets.get(index));
    }

    public ValueProtos.ResultSet getRawResultSet(int index) {
        return resultSets.get(index);
    }

    public boolean isTruncated(int index) {
        return resultSets.get(index).getTruncated();
    }

    public int getRowCount(int index) {
        return resultSets.get(index).getRowsCount();
    }

    public boolean isEmpty() {
        return txId.isEmpty() && resultSets.isEmpty();
    }

    public QueryStats getQueryStats() {
        return new QueryStats(queryStats);
    }

    public YdbQueryStats.QueryStats getRawQueryStats() {
        return queryStats;
    }

    public boolean hasQueryStats() {
        return queryStats != null;
    }
}
