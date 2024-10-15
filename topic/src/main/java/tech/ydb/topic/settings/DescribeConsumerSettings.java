package tech.ydb.topic.settings;

import tech.ydb.core.settings.OperationSettings;

/**
 *
 * @author Aleksandr Gorshenin
 */
public class DescribeConsumerSettings extends OperationSettings {
    private final boolean includeStats;
    private final boolean includeLocation;

    public DescribeConsumerSettings(Builder builder) {
        super(builder);
        this.includeStats = builder.includeStats;
        this.includeLocation = builder.includeLocation;
    }

    public boolean isIncludeStats() {
        return includeStats;
    }

    public boolean isIncludeLocation() {
        return includeLocation;
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public static class Builder extends OperationBuilder<Builder> {
        private boolean includeStats = false;
        private boolean includeLocation = false;

        public Builder withIncludeStats(boolean value) {
            this.includeStats = value;
            return this;
        }

        public Builder withIncludeLocation(boolean value) {
            this.includeLocation = value;
            return this;
        }

        @Override
        public DescribeConsumerSettings build() {
            return new DescribeConsumerSettings(this);
        }
    }
}

