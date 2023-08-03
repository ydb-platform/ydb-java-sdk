package tech.ydb.query.settings;

import tech.ydb.core.settings.OperationSettings;

/**
 *
 * @author Aleksandr Gorshenin
 */
public class ExecuteQuerySettings extends OperationSettings {
    private ExecuteQuerySettings(Builder builder) {
        super(builder);
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public static class Builder extends OperationSettings.OperationBuilder<Builder> {
        @Override
        public ExecuteQuerySettings build() {
            return new ExecuteQuerySettings(this);
        }
    }
}
