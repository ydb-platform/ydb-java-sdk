package tech.ydb.topic.settings;

import tech.ydb.core.settings.OperationSettings;

public class TopicClientOperationSettings extends OperationSettings {
    private final boolean preferReadyChannel;

    protected TopicClientOperationSettings(TopicClientOperationBuilder<?> builder) {
        super(builder);
        this.preferReadyChannel = builder.preferReadyChannel;
    }

    public boolean isPreferReadyChannel() {
        return preferReadyChannel;
    }

    public static class TopicClientOperationBuilder<Self extends TopicClientOperationBuilder<?>>
            extends OperationBuilder<Self> {
        private boolean preferReadyChannel = false;

        public Self withPreferReadyChannel(boolean value) {
            this.preferReadyChannel = value;
            return self();
        }

        @Override
        public TopicClientOperationSettings build() {
            return new TopicClientOperationSettings(this);
        }
    }
}
