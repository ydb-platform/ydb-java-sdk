package tech.ydb.table.settings;

/**
 * @author Sergey Polovko
 */
public class ExecuteDataQuerySettings extends RequestSettings<ExecuteDataQuerySettings> {

    private boolean keepInQueryCache = true;

    public boolean isKeepInQueryCache() {
        return keepInQueryCache;
    }

    public ExecuteDataQuerySettings disableQueryCache() {
        keepInQueryCache = false;
        return this;
    }
}
