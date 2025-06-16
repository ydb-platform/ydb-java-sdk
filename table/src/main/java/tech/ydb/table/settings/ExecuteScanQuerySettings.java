package tech.ydb.table.settings;

import tech.ydb.core.grpc.GrpcFlowControl;
import tech.ydb.core.settings.BaseRequestSettings;
import tech.ydb.proto.table.YdbTable;
import tech.ydb.table.query.stats.QueryStatsCollectionMode;


public class ExecuteScanQuerySettings extends BaseRequestSettings {
    public enum Mode {
        UNSPECIFIED,
        EXPLAIN,
        EXEC;

        public YdbTable.ExecuteScanQueryRequest.Mode toPb() {
            switch (this) {
                case UNSPECIFIED:
                    return YdbTable.ExecuteScanQueryRequest.Mode.MODE_UNSPECIFIED;
                case EXPLAIN:
                    return YdbTable.ExecuteScanQueryRequest.Mode.MODE_EXPLAIN;
                case EXEC:
                    return YdbTable.ExecuteScanQueryRequest.Mode.MODE_EXEC;
                default:
                    throw new IllegalStateException("Unsupported ExecuteScanQueryRequest mode.");
            }
        }
    }

    private final Mode mode;
    private final QueryStatsCollectionMode collectStats;
    private final GrpcFlowControl flowControl;

    public ExecuteScanQuerySettings(Builder builder) {
        super(builder);
        this.mode = builder.mode;
        this.collectStats = builder.collectStats;
        this.flowControl = builder.flowControl;
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public static final class Builder extends BaseBuilder<Builder> {
        private Mode mode = Mode.EXEC;
        private QueryStatsCollectionMode collectStats = QueryStatsCollectionMode.NONE;
        private GrpcFlowControl flowControl = null;

        public Builder setMode(Mode mode) {
            this.mode = mode;
            return this;
        }

        public Builder setCollectStats(QueryStatsCollectionMode collectStats) {
            this.collectStats = collectStats;
            return this;
        }

        public Builder setGrpcFlowControl(GrpcFlowControl ctrl) {
            this.flowControl = ctrl;
            return this;
        }

        @Override
        public ExecuteScanQuerySettings build() {
            return new ExecuteScanQuerySettings(this);
        }
    }

    public Mode getMode() {
        return mode;
    }

    public QueryStatsCollectionMode getCollectStats() {
        return collectStats;
    }

    public GrpcFlowControl getGrpcFlowControl() {
        return flowControl;
    }
}
