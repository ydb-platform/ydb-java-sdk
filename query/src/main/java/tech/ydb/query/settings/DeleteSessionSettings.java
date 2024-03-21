package tech.ydb.query.settings;

import tech.ydb.core.settings.OperationSettings;

/**
 *
 * @author Aleksandr Gorshenin
 */
public class DeleteSessionSettings extends OperationSettings {
    private DeleteSessionSettings(Builder builder) {
        super(builder);
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public static class Builder extends OperationBuilder<Builder> {
        @Override
        public DeleteSessionSettings build() {
            return new DeleteSessionSettings(this);
        }
    }
}
