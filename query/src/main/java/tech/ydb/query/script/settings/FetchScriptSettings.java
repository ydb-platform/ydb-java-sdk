package tech.ydb.query.script.settings;

import com.google.common.base.Preconditions;

import tech.ydb.core.settings.BaseRequestSettings;

/**
 * Settings for configuring the fetch phase of a previously executed YQL script.
 * Take a note that script should be executed successfully before fetch result
 * <p>
 * These settings define which operation results to fetch, pagination options,
 * row limits, and which result set index to retrieve.
 * Used with {@code QuerySession.fetchScriptResults(...)} and similar APIs.
 *
 * <p>Author: Evgeny Kuvardin
 */
public class FetchScriptSettings extends BaseRequestSettings {
    private final int rowsLimit;
    private final long resultSetIndex;

    private FetchScriptSettings(Builder builder) {
        super(builder);
        this.rowsLimit = builder.rowsLimit;
        this.resultSetIndex = builder.resultSetIndex;
    }

    /**
     * Returns the maximum number of rows to retrieve in this fetch request.
     *
     * <p>If not set, the server will use its default limit.</p>
     *
     * @return the maximum number of rows to fetch
     */
    public int getRowsLimit() {
        return rowsLimit;
    }

    /**
     * Returns the index of the result set to fetch from the executed script.
     *
     * <p>When the executed script produces multiple result sets,
     * this value specifies which one to retrieve (starting from 0).</p>
     *
     * @return the result set index
     */
    public long getResultSetIndex() {
        return resultSetIndex;
    }

    /**
     * Creates a new builder configured for asynchronous operation requests.
     * @return a new builder of FetchScriptSettings
     */
    public static Builder newBuilder() {
        return new Builder();
    }

    public static class Builder extends BaseBuilder<Builder> {

        private int rowsLimit = 0;
        private long resultSetIndex = 0;

        @Override
        public FetchScriptSettings build() {
            return new FetchScriptSettings(this);
        }

        public Builder withRowsLimit(int rowsLimit) {
            Preconditions.checkArgument(rowsLimit >= 0, "rowsLimit must be non-negative");
            this.rowsLimit = rowsLimit;
            return this;
        }

        public Builder withResultSetIndex(long resultSetIndex) {
            Preconditions.checkArgument(rowsLimit >= 0, "resultSetIndex must be non-negative");
            this.resultSetIndex = resultSetIndex;
            return this;
        }

    }
}
