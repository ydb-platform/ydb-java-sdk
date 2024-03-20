package tech.ydb.query.settings;

import tech.ydb.core.settings.OperationSettings;

/**
 *
 * @author Aleksandr Gorshenin
 */
public class RollbackTransactionSettings extends OperationSettings {
    private RollbackTransactionSettings(Builder builder) {
        super(builder);
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public static class Builder extends OperationBuilder<Builder> {
        @Override
        public RollbackTransactionSettings build() {
            return new RollbackTransactionSettings(this);
        }
    }
}
