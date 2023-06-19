package tech.ydb.table.settings;

import tech.ydb.core.settings.BaseRequestSettings;
import tech.ydb.proto.table.YdbTable;


public class ExecuteScanQuerySettings extends BaseRequestSettings {
    private final YdbTable.ExecuteScanQueryRequest.Mode mode;
    private final YdbTable.QueryStatsCollection.Mode collectStats;

    public ExecuteScanQuerySettings(Builder builder) {
        super(builder);
        this.mode = builder.mode;
        this.collectStats = builder.collectStats;
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public static final class Builder extends BaseBuilder<Builder> {
        private YdbTable.ExecuteScanQueryRequest.Mode mode = YdbTable.ExecuteScanQueryRequest.Mode.MODE_EXEC;
        private YdbTable.QueryStatsCollection.Mode collectStats = YdbTable.QueryStatsCollection.Mode.
                STATS_COLLECTION_NONE;

        public Builder mode(YdbTable.ExecuteScanQueryRequest.Mode mode) {
            this.mode = mode;
            return this;
        }

        public Builder collectStats(YdbTable.QueryStatsCollection.Mode collectStats) {
            this.collectStats = collectStats;
            return this;
        }

        @Override
        public ExecuteScanQuerySettings build() {
            return new ExecuteScanQuerySettings(this);
        }
    }


    public YdbTable.ExecuteScanQueryRequest.Mode getMode() {
        return mode;
    }

    public YdbTable.QueryStatsCollection.Mode getCollectStats() {
        return collectStats;
    }
}
