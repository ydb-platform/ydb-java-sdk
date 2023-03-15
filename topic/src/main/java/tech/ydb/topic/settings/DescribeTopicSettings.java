package tech.ydb.topic.settings;

import tech.ydb.core.settings.OperationSettings;

public class DescribeTopicSettings extends OperationSettings {
    /* TODO: renew api and add stats
    private boolean includeStats = false;

    public DescribeTopicSettings(boolean includeStats) {
        this.includeStats = includeStats;
    }

    public void setIncludeStats(boolean includeStats) {
        this.includeStats = includeStats;
    }

    public boolean getIncludeStats() {
        return includeStats;
    }*/

    private DescribeTopicSettings(Builder builder) {
        super(builder);
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public static class Builder extends OperationBuilder<Builder> {
        @Override
        public DescribeTopicSettings build() {
            return new DescribeTopicSettings(this);
        }
    }
}
