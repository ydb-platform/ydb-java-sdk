package tech.ydb.query.settings;

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
    private final String operationId;
    private final String fetchToken;
    private final int rowsLimit;
    private final int setResultSetIndex;

    private FetchScriptSettings(Builder builder) {
        super(builder);
        this.operationId = builder.operationId;
        this.fetchToken = builder.fetchToken;
        this.rowsLimit = builder.rowsLimit;
        this.setResultSetIndex = builder.setResultSetIndex;
    }

    /**
     * Returns the identifier of the operation whose results should be fetched.
     *
     * <p>This ID corresponds to the operation returned by
     * {@code QuerySession.executeScript(...)} or a similar asynchronous call.</p>
     *
     * @return the operation ID string
     */
    public String getOperationId() {
        return operationId;
    }

    /**
     * Returns the fetch token used to continue fetching paginated results.
     *
     * <p>When a previous fetch request indicates more data is available,
     * this token can be used to retrieve the next portion of results.</p>
     *
     * @return the fetch token, or an empty string if not set
     */
    public String getFetchToken() {
        return fetchToken;
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
    public int getSetResultSetIndex() {
        return setResultSetIndex;
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public static class Builder extends BaseBuilder<Builder> {

        private int rowsLimit = 0;
        private int setResultSetIndex = 0;
        private String operationId = "";
        private String fetchToken = "";

        @Override
        public FetchScriptSettings build() {
            return new FetchScriptSettings(this);
        }

        public Builder withEOperationId(String operationId) {
            this.operationId = operationId;
            return this;
        }

        public Builder withFetchToken(String fetchToken) {
            this.fetchToken = fetchToken;
            return this;
        }

        public Builder withRowsLimit(int rowsLimit) {
            this.rowsLimit = rowsLimit;
            return this;
        }

        public Builder withSetResultSetIndex(int setResultSetIndex) {
            this.setResultSetIndex = setResultSetIndex;
            return this;
        }

    }
}
