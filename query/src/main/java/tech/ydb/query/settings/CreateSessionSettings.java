package tech.ydb.query.settings;

import tech.ydb.core.settings.OperationSettings;

/**
 *
 * @author Aleksandr Gorshenin
 */
public class CreateSessionSettings extends OperationSettings {
    private CreateSessionSettings(Builder builder) {
        super(builder);
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public static class Builder extends OperationBuilder<Builder> {
        @Override
        public CreateSessionSettings build() {
            return new CreateSessionSettings(this);
        }
    }
}
