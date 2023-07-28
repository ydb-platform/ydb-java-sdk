package tech.ydb.table.query.stats;

import java.util.List;

import com.google.protobuf.MessageOrBuilder;

public interface QueryPhaseStatsOrBuilder extends MessageOrBuilder {
    long getDurationUs();

    List<TableAccessStats> getTableAccessList();

    TableAccessStats getTableAccess(int var1);

    int getTableAccessCount();

    List<? extends TableAccessStatsOrBuilder> getTableAccessOrBuilderList();

    TableAccessStatsOrBuilder getTableAccessOrBuilder(int var1);

    long getCpuTimeUs();

    long getAffectedShards();

    boolean getLiteralPhase();
}
