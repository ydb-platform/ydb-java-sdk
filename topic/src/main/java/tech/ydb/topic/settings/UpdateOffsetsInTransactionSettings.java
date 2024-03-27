package tech.ydb.topic.settings;

import tech.ydb.core.settings.OperationSettings;

/**
 * @author Nikolay Perfilov
 */
public class UpdateOffsetsInTransactionSettings extends OperationSettings {
    private UpdateOffsetsInTransactionSettings(Builder builder) {
        super(builder);
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public static class Builder extends OperationBuilder<Builder> {
        @Override
        public UpdateOffsetsInTransactionSettings build() {
            return new UpdateOffsetsInTransactionSettings(this);
        }
    }
}
