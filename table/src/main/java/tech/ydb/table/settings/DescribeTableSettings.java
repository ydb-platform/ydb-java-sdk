package tech.ydb.table.settings;

/**
 * @author Sergey Polovko
 */
public class DescribeTableSettings extends RequestSettings<DescribeTableSettings> {
    private boolean includeTableStats;
    private boolean includeShardKeyBounds;

    public boolean isIncludeTableStats() {
        return includeTableStats;
    }

    public boolean isIncludeShardKeyBounds() {
        return includeShardKeyBounds;
    }

    public void setIncludeTableStats(boolean includeTableStats) {
        this.includeTableStats = includeTableStats;
    }

    public void setIncludeShardKeyBounds(boolean includeShardKeyBounds) {
        this.includeShardKeyBounds = includeShardKeyBounds;
    }
}
