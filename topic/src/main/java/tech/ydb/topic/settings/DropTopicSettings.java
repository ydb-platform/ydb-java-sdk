package tech.ydb.topic.settings;

import tech.ydb.core.settings.OperationSettings;

/**
 * @author Nikolay Perfilov
 */
public class DropTopicSettings extends OperationSettings {
    private DropTopicSettings(Builder builder) {
        super(builder);
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public static class Builder extends OperationBuilder<Builder> {
        @Override
        public DropTopicSettings build() {
            return new DropTopicSettings(this);
        }
    }
}
