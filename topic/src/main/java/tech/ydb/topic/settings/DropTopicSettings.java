package tech.ydb.topic.settings;

/**
 * @author Nikolay Perfilov
 */
public class DropTopicSettings extends TopicClientOperationSettings {
    private DropTopicSettings(Builder builder) {
        super(builder);
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public static class Builder extends TopicClientOperationBuilder<Builder> {
        @Override
        public DropTopicSettings build() {
            return new DropTopicSettings(this);
        }
    }
}
