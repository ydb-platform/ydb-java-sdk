package tech.ydb.query.script.settings;

import tech.ydb.core.settings.OperationSettings;

/**
 * Settings for retrieving metadata of a previously started script operation.
 * <p>
 * Extends {@link OperationSettings} and enables async fetching by default.
 *
 * <p>Author: Evgeny Kuvardin
 */
public class FindScriptSettings extends OperationSettings {

    private FindScriptSettings(Builder builder) {
        super(builder);
    }

    public static Builder newBuilder() {
        return new Builder().withAsyncMode(true);
    }

    public static class Builder extends OperationSettings.OperationBuilder<Builder> {
        @Override
        public FindScriptSettings build() {
            return new FindScriptSettings(this);
        }
    }
}
