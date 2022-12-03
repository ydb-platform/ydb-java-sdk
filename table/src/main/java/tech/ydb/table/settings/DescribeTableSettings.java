package tech.ydb.table.settings;

import tech.ydb.core.settings.RequestSettings;

/**
 * @author Sergey Polovko
 */
public class DescribeTableSettings extends RequestSettings<DescribeTableSettings> {
    private boolean includeTableStats;
    private boolean includeShardKeyBounds;
    private boolean includePartitionStats;

    public boolean isIncludeTableStats() {
        return includeTableStats;
    }

    public boolean isIncludeShardKeyBounds() {
        return includeShardKeyBounds;
    }

    public boolean isIncludePartitionStats() {
        return includePartitionStats;
    }

    public void setIncludeTableStats(boolean includeTableStats) {
        this.includeTableStats = includeTableStats;
    }

    public void setIncludeShardKeyBounds(boolean includeShardKeyBounds) {
        this.includeShardKeyBounds = includeShardKeyBounds;
    }

    public void setIncludePartitionStats(boolean includePartitionStats) {
        this.includePartitionStats = includePartitionStats;
    }
}
