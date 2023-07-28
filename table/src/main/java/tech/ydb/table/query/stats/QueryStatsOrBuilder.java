package tech.ydb.table.query.stats;

import java.util.List;

import com.google.protobuf.ByteString;
import com.google.protobuf.MessageOrBuilder;

public interface QueryStatsOrBuilder extends MessageOrBuilder {
    List<QueryPhaseStats> getQueryPhasesList();

    QueryPhaseStats getQueryPhases(int var1);

    int getQueryPhasesCount();

    List<? extends QueryPhaseStatsOrBuilder> getQueryPhasesOrBuilderList();

    QueryPhaseStatsOrBuilder getQueryPhasesOrBuilder(int var1);

    boolean hasCompilation();

    CompilationStats getCompilation();

    CompilationStatsOrBuilder getCompilationOrBuilder();

    long getProcessCpuTimeUs();

    String getQueryPlan();

    ByteString getQueryPlanBytes();

    String getQueryAst();

    ByteString getQueryAstBytes();

    long getTotalDurationUs();

    long getTotalCpuTimeUs();
}
