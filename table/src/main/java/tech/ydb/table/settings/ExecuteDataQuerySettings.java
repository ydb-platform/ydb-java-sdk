package tech.ydb.table.settings;

import javax.annotation.Nonnull;

import tech.ydb.table.query.stats.QueryStatsCollectionMode;

/**
 * @author Sergey Polovko
 */
public class ExecuteDataQuerySettings extends RequestSettings<ExecuteDataQuerySettings> {
    private boolean keepInQueryCache = true;
    private QueryStatsCollectionMode collectStats = QueryStatsCollectionMode.NONE;

    public boolean isKeepInQueryCache() {
        return keepInQueryCache;
    }

    public ExecuteDataQuerySettings disableQueryCache() {
        keepInQueryCache = false;
        return this;
    }

    @Nonnull
    public QueryStatsCollectionMode collectStats() {
        return collectStats;
    }

    public ExecuteDataQuerySettings setCollectStats(@Nonnull QueryStatsCollectionMode collectStats) {
        this.collectStats = collectStats;
        return this;
    }
}
