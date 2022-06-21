package tech.ydb.table.settings;

import javax.annotation.Nonnull;

import tech.ydb.table.YdbTable;

import static tech.ydb.table.YdbTable.QueryStatsCollection.Mode.STATS_COLLECTION_NONE;

/**
 * @author Sergey Polovko
 */
public class ExecuteDataQuerySettings extends RequestSettings<ExecuteDataQuerySettings> {

    private boolean keepInQueryCache = false;

    private YdbTable.QueryStatsCollection.Mode collectStats = STATS_COLLECTION_NONE;

    public boolean isKeepInQueryCache() {
        return keepInQueryCache;
    }

    public ExecuteDataQuerySettings keepInQueryCache() {
        keepInQueryCache = true;
        return this;
    }

    @Nonnull
    public YdbTable.QueryStatsCollection.Mode collectStats() {
        return collectStats;
    }

    public ExecuteDataQuerySettings setCollectStats(@Nonnull YdbTable.QueryStatsCollection.Mode collectStats) {
        this.collectStats = collectStats;
        return this;
    }
}
