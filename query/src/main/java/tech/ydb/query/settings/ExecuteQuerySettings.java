package tech.ydb.query.settings;

import tech.ydb.core.settings.BaseRequestSettings;

/**
 *
 * @author Aleksandr Gorshenin
 */
public class ExecuteQuerySettings extends BaseRequestSettings {
    private final QueryExecMode execMode;
    private final QueryStatsMode statsMode;

    private ExecuteQuerySettings(Builder builder) {
        super(builder);
        this.execMode = builder.execMode;
        this.statsMode = builder.statsMode;
    }

    public QueryExecMode getExecMode() {
        return this.execMode;
    }

    public QueryStatsMode getStatsMode() {
        return this.statsMode;
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public static class Builder extends BaseBuilder<Builder> {
        private QueryExecMode execMode = QueryExecMode.EXECUTE;
        private QueryStatsMode statsMode = QueryStatsMode.NONE;

        public Builder withExecMode(QueryExecMode mode) {
            this.execMode = mode;
            return this;
        }

        public Builder withStatsMode(QueryStatsMode mode) {
            this.statsMode = mode;
            return this;
        }

        @Override
        public ExecuteQuerySettings build() {
            return new ExecuteQuerySettings(this);
        }
    }
}
