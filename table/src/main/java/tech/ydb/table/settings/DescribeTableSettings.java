package tech.ydb.table.settings;

/**
 * @author Sergey Polovko
 */
public class DescribeTableSettings extends RequestSettings<DescribeTableSettings> {
    private boolean includeTableStats;

    public boolean isIncludeTableStats() {
        return includeTableStats;
    }

    public void setIncludeTableStats(boolean includeTableStats) {
        this.includeTableStats = includeTableStats;
    }
}
