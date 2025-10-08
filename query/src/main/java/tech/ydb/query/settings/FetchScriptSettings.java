package tech.ydb.query.settings;

import tech.ydb.core.settings.BaseRequestSettings;

/**
 * Script fetch settings to pass to fetch script
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

    public String getOperationId() {
        return operationId;
    }

    public String getFetchToken() {
        return fetchToken;
    }

    public int getRowsLimit() {
        return rowsLimit;
    }

    public int getSetResultSetIndex() {
        return setResultSetIndex;
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public static class Builder extends BaseBuilder<Builder> {
        public int getRowsLimit() {
            return rowsLimit;
        }

        public Builder setRowsLimit(int rowsLimit) {
            this.rowsLimit = rowsLimit;
            return this;
        }

        public int getSetResultSetIndex() {
            return setResultSetIndex;
        }

        public Builder setSetResultSetIndex(int setResultSetIndex) {
            this.setResultSetIndex = setResultSetIndex;
            return this;
        }

        public int rowsLimit = -1;
        public int setResultSetIndex = 0;
        private String operationId = "";
        private String fetchToken = "";

        public Builder withEOperationId(String operationId) {
            this.operationId = operationId;
            return this;
        }

        public Builder withFetchToken(String fetchToken) {
            this.fetchToken = fetchToken;
            return this;
        }

        @Override
        public FetchScriptSettings build() {
            return new FetchScriptSettings(this);
        }
    }
}
