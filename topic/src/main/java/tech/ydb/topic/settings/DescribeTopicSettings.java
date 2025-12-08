package tech.ydb.topic.settings;

import tech.ydb.core.settings.OperationSettings;

/**
 * @author Nikolay Perfilov
 */
public class DescribeTopicSettings extends OperationSettings {
    private final boolean includeStats;

    private DescribeTopicSettings(Builder builder) {
        super(builder);
        this.includeStats = builder.includeStats;
    }

    public boolean isIncludeStats() {
        return includeStats;
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public static class Builder extends OperationBuilder<Builder> {
        private boolean includeStats = false;

        public Builder withIncludeStats(boolean includeStats) {
            this.includeStats = includeStats;
            return this;
        }

        @Override
        public DescribeTopicSettings build() {
            return new DescribeTopicSettings(this);
        }
    }
}
