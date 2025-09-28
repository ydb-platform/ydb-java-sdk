package tech.ydb.query.settings;

import tech.ydb.core.grpc.GrpcFlowControl;
import tech.ydb.core.settings.BaseRequestSettings;

import java.time.Duration;

/**

 */
public class ExecuteScriptSettings extends BaseRequestSettings {
    private final QueryExecMode execMode;
    private final QueryStatsMode statsMode;
    private final String resourcePool;
    private final Duration ttl;

    private ExecuteScriptSettings(Builder builder) {
        super(builder);
        this.execMode = builder.execMode;
        this.statsMode = builder.statsMode;
        this.ttl = builder.ttl;
        this.resourcePool = builder.resourcePool;
    }

    public QueryExecMode getExecMode() {
        return this.execMode;
    }

    public Duration getTtl() {
        return ttl;
    }

    public QueryStatsMode getStatsMode() {
        return this.statsMode;
    }

    /**
     * Get resource pool for query execution
     * @return resource pool name
     */
    public String getResourcePool() {
        return this.resourcePool;
    }


    public static Builder newBuilder() {
        return new Builder();
    }

    public static class Builder extends BaseBuilder<Builder> {
        private QueryExecMode execMode = QueryExecMode.EXECUTE;
        private QueryStatsMode statsMode = QueryStatsMode.NONE;
        private String resourcePool = null;
        private Duration ttl =null;

        public Builder withExecMode(QueryExecMode mode) {
            this.execMode = mode;
            return this;
        }

        public Builder withStatsMode(QueryStatsMode mode) {
            this.statsMode = mode;
            return this;
        }

        public Builder withTtl(Duration value) {
            this.ttl = value;
            return this;
        }

        /**
         * Set resource pool which query try to use.
         * If no pool specify or poolId is empty or poolId equals "default"
         * the unremovable resource pool "default" will be used
         *
         * @param poolId resource pool identifier
         *
         * @return builder
         */
        public Builder withResourcePool(String poolId) {
            this.resourcePool = poolId;
            return this;
        }

        @Override
        public ExecuteScriptSettings build() {
            return new ExecuteScriptSettings(this);
        }
    }
}
