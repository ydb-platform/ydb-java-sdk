package tech.ydb.table.query.stats;

import tech.ydb.proto.table.YdbTable.QueryStatsCollection.Mode;


public enum QueryStatsCollectionMode {
    UNSPECIFIED,
    NONE,
    BASIC,
    FULL,
    PROFILE;

    public Mode toPb() {
        switch (this) {
            case UNSPECIFIED:
                return Mode.STATS_COLLECTION_UNSPECIFIED;
            case NONE:
                return Mode.STATS_COLLECTION_NONE;
            case BASIC:
                return Mode.STATS_COLLECTION_BASIC;
            case FULL:
                return Mode.STATS_COLLECTION_FULL;
            case PROFILE:
                return Mode.STATS_COLLECTION_PROFILE;
            default:
                throw new IllegalStateException("Unsupported query statistic collection mode.");
        }
    }
}
