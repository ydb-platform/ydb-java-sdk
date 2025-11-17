package tech.ydb.query.script.settings;

import tech.ydb.core.settings.BaseRequestSettings;

/**
 * Settings for configuring the fetch phase of a previously executed YQL script.
 * <p>
 * These settings define which operation results to fetch, pagination options,
 * row limits, and which result set index to retrieve.
 * Used with {@code QuerySession.fetchScriptResults(...)} and similar APIs.
 *
 * <p>Author: Evgeny Kuvardin
 */
public class FetchScriptSettings extends BaseRequestSettings {
    private final int rowsLimit;
    private final long setResultSetIndex;

    private FetchScriptSettings(Builder builder) {
        super(builder);
        this.rowsLimit = builder.rowsLimit;
        this.setResultSetIndex = builder.setResultSetIndex;
    }

    /**
     * Returns the maximum number of rows to retrieve in this fetch request.
     *
     * <p>If not set , the server will use its default limit.</p>
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
    public long getSetResultSetIndex() {
        return setResultSetIndex;
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public static class Builder extends BaseBuilder<Builder> {

        private int rowsLimit = 0;
        private long setResultSetIndex = 0;

        @Override
        public FetchScriptSettings build() {
            return new FetchScriptSettings(this);
        }

        public Builder withRowsLimit(int rowsLimit) {
            this.rowsLimit = rowsLimit;
            return this;
        }

        public Builder withSetResultSetIndex(long setResultSetIndex) {
            this.setResultSetIndex = setResultSetIndex;
            return this;
        }

    }
}
