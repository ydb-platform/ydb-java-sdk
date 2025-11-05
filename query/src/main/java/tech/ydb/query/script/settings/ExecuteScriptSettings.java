package tech.ydb.query.script.settings;

import java.time.Duration;

import tech.ydb.core.settings.BaseRequestSettings;
import tech.ydb.query.settings.QueryExecMode;
import tech.ydb.query.settings.QueryStatsMode;

/**
 * Settings for configuring script execution requests.
 * <p>
 * Used by {@code QuerySession.executeScript(...)} and similar APIs.
 *
 * <p>Author: Evgeny Kuvardin
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

    /**
     * Returns the execution mode for the script.
     *
     * <p>Defines how the script should be processed, e.g. executed, explained, validated, or parsed.</p>
     *
     * @return the {@link QueryExecMode} used for execution
     */
    public QueryExecMode getExecMode() {
        return this.execMode;
    }

    /**
     * Returns the time-to-live (TTL) duration for the script results.
     *
     * <p>Specifies how long results of the executed script will be kept available
     * before automatic cleanup on the server.</p>
     *
     * @return the TTL value, or {@code null} if not set
     */
    public Duration getTtl() {
        return ttl;
    }

    /**
     * Returns the statistics collection mode for script execution.
     *
     * <p>Determines how detailed execution statistics should be gathered
     * (none, basic, full, or profiling level).</p>
     *
     * @return the {@link QueryStatsMode} used for statistics collection
     */
    public QueryStatsMode getStatsMode() {
        return this.statsMode;
    }

    /**
     * Returns the name of the resource pool assigned to the script execution.
     *
     * <p>Resource pools define isolated resource groups for workload management.
     * If not specified, the default pool is used.</p>
     *
     * @return the resource pool name, or {@code null} if not set
     */
    public String getResourcePool() {
        return this.resourcePool;
    }


    /**
     * Creates a new {@link Builder} instance for constructing {@link ExecuteScriptSettings}.
     *
     * @return a new builder
     */
    public static Builder newBuilder() {
        return new Builder();
    }

    /**
     * Builder for creating immutable {@link ExecuteScriptSettings} instances.
     * <p>
     * Provides fluent configuration for script execution settings
     */
    public static class Builder extends BaseBuilder<Builder> {
        private QueryExecMode execMode = QueryExecMode.EXECUTE;
        private QueryStatsMode statsMode = QueryStatsMode.NONE;
        private String resourcePool = null;
        private Duration ttl = null;

        /**
         * Sets the execution mode for the script.
         *
         * @param mode the desired execution mode
         * @return this builder instance for chaining
         * @see QueryExecMode
         */
        public Builder withExecMode(QueryExecMode mode) {
            this.execMode = mode;
            return this;
        }

        /**
         * Sets the statistics collection mode for the script execution.
         *
         * @param mode the desired statistics mode
         * @return this builder instance for chaining
         * @see QueryStatsMode
         */
        public Builder withStatsMode(QueryStatsMode mode) {
            this.statsMode = mode;
            return this;
        }

        /**
         * Sets the time-to-live (TTL) duration for script results.
         *
         * <p>After this duration expires, stored script results may be deleted
         * from the server automatically.</p>
         *
         * @param value the TTL duration
         * @return this builder instance for chaining
         */
        public Builder withTtl(Duration value) {
            this.ttl = value;
            return this;
        }

        /**
         * Specifies the resource pool to use for query execution.
         * <p>
         * If no pool is specified, or the ID is empty, or equal to {@code "default"},
         * the unremovable resource pool "default" will be used.
         *
         * @param poolId resource pool identifier
         * @return this builder instance for chaining
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
