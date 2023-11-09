package tech.ydb.query.settings;

import tech.ydb.core.settings.BaseRequestSettings;

/**
 *
 * @author Aleksandr Gorshenin
 */
public class ExecuteQuerySettings extends BaseRequestSettings {
    private ExecuteQuerySettings(Builder builder) {
        super(builder);
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public static class Builder extends BaseBuilder<Builder> {
        @Override
        public ExecuteQuerySettings build() {
            return new ExecuteQuerySettings(this);
        }
    }
}
