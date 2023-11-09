package tech.ydb.query.settings;

import tech.ydb.core.settings.OperationSettings;

/**
 *
 * @author Aleksandr Gorshenin
 */
public class BeginTransactionSettings extends OperationSettings {
    private BeginTransactionSettings(Builder builder) {
        super(builder);
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public static class Builder extends OperationBuilder<Builder> {
        @Override
        public BeginTransactionSettings build() {
            return new BeginTransactionSettings(this);
        }
    }
}
