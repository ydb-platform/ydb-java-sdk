package tech.ydb.query.settings;

import tech.ydb.core.settings.BaseRequestSettings;

/**
 *
 * @author Aleksandr Gorshenin
 */
public class ExecuteQuerySettings extends BaseRequestSettings {
    private final QueryExecMode execMode;
    private final QueryStatsMode statsMode;
    private final boolean concurrentResultSets;
    private final String resourcePool;

    private ExecuteQuerySettings(Builder builder) {
        super(builder);
        this.execMode = builder.execMode;
        this.statsMode = builder.statsMode;
        this.concurrentResultSets = builder.concurrentResultSets;
        this.resourcePool = builder.resourcePool;
    }

    public QueryExecMode getExecMode() {
        return this.execMode;
    }

    public QueryStatsMode getStatsMode() {
        return this.statsMode;
    }

    public boolean isConcurrentResultSets() {
        return this.concurrentResultSets;
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
        private boolean concurrentResultSets = false;
        private String resourcePool = null;

        public Builder withExecMode(QueryExecMode mode) {
            this.execMode = mode;
            return this;
        }

        public Builder withStatsMode(QueryStatsMode mode) {
            this.statsMode = mode;
            return this;
        }

        public Builder withConcurrentResultSets(boolean value) {
            this.concurrentResultSets = value;
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
        public ExecuteQuerySettings build() {
            return new ExecuteQuerySettings(this);
        }
    }
}
