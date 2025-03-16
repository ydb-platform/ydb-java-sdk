package tech.ydb.query.settings;

import tech.ydb.core.settings.BaseRequestSettings;

/**
 *
 * @author Aleksandr Gorshenin
 */
public class ExecuteQuerySettings extends BaseRequestSettings {
    private final QueryExecMode execMode;
    private final QueryStatsMode statsMode;
    private final String resourcePool;

    private ExecuteQuerySettings(Builder builder) {
        super(builder);
        this.execMode = builder.execMode;
        this.statsMode = builder.statsMode;
        this.resourcePool = builder.resourcePool;
    }

    public QueryExecMode getExecMode() {
        return this.execMode;
    }

    public QueryStatsMode getStatsMode() {
        return this.statsMode;
    }

    public String getResourcePool() {
        return this.resourcePool;
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public static class Builder extends BaseBuilder<Builder> {
        private QueryExecMode execMode = QueryExecMode.EXECUTE;
        private QueryStatsMode statsMode = QueryStatsMode.NONE;
        private String resourcePool = "default";

        public Builder withExecMode(QueryExecMode mode) {
            this.execMode = mode;
            return this;
        }

        public Builder withStatsMode(QueryStatsMode mode) {
            this.statsMode = mode;
            return this;
        }

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
