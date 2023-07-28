package tech.ydb.table.query.stats;

import com.google.protobuf.MessageOrBuilder;

public interface CompilationStatsOrBuilder extends MessageOrBuilder {
    boolean getFromCache();

    long getDurationUs();

    long getCpuTimeUs();
}
