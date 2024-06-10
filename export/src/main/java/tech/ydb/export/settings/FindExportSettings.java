package tech.ydb.export.settings;

import tech.ydb.core.settings.OperationSettings;

/**
 *
 * @author Aleksandr Gorshenin
 */
public class FindExportSettings extends OperationSettings {
    private FindExportSettings(Builder builder) {
        super(builder);
    }

    public static Builder newBuilder() {
        return new Builder().withAsyncMode(true);
    }

    public static class Builder extends OperationSettings.OperationBuilder<Builder> {
        @Override
        public FindExportSettings build() {
            return new FindExportSettings(this);
        }
    }
}
