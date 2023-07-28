package tech.ydb.table.query.stats;

import com.google.protobuf.MessageOrBuilder;

public interface OperationStatsOrBuilder extends MessageOrBuilder {
    long getRows();

    long getBytes();
}
