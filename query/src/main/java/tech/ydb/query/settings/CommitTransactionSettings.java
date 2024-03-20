package tech.ydb.query.settings;

import tech.ydb.core.settings.OperationSettings;

/**
 *
 * @author Aleksandr Gorshenin
 */
public class CommitTransactionSettings extends OperationSettings {
    private CommitTransactionSettings(Builder builder) {
        super(builder);
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public static class Builder extends OperationBuilder<Builder> {
        @Override
        public CommitTransactionSettings build() {
            return new CommitTransactionSettings(this);
        }
    }
}
