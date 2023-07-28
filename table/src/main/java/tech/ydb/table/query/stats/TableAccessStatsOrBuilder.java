package tech.ydb.table.query.stats;

import com.google.protobuf.ByteString;
import com.google.protobuf.MessageOrBuilder;

public interface TableAccessStatsOrBuilder extends MessageOrBuilder {
    String getName();

    ByteString getNameBytes();

    boolean hasReads();

    OperationStats getReads();

    OperationStatsOrBuilder getReadsOrBuilder();

    boolean hasUpdates();

    OperationStats getUpdates();

    OperationStatsOrBuilder getUpdatesOrBuilder();

    boolean hasDeletes();

    OperationStats getDeletes();

    OperationStatsOrBuilder getDeletesOrBuilder();

    long getPartitionsCount();
}
