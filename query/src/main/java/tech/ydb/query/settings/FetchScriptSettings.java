package tech.ydb.query.settings;

import java.time.Duration;

import tech.ydb.core.settings.BaseRequestSettings;

/**
 *  operationId_ = "";
 *       fetchToken_ = "";
 */
public class FetchScriptSettings extends BaseRequestSettings {
    private final String operationId;
    private final String fetchToken;

    private FetchScriptSettings(Builder builder) {
        super(builder);
        this.operationId = builder.operationId;
        this.fetchToken = builder.fetchToken;
    }

    public String getOperationId() {
        return operationId;
    }

    public String getFetchToken() {
        return fetchToken;
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public static class Builder extends BaseBuilder<Builder> {
        private String operationId = "";
        private String fetchToken = "";

        public Builder withEOperationId(String operationId ) {
            this.operationId = operationId;
            return this;
        }

        public Builder withFetchToken(String fetchToken ) {
            this.fetchToken = fetchToken;
            return this;
        }

        @Override
        public FetchScriptSettings build() {
            return new FetchScriptSettings(this);
        }
    }
}
