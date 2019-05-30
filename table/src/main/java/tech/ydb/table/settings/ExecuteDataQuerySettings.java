package tech.ydb.table.settings;

/**
 * @author Sergey Polovko
 */
public class ExecuteDataQuerySettings extends RequestSettings<ExecuteDataQuerySettings> {

    private boolean keepInQueryCache = false;

    public boolean isKeepInQueryCache() {
        return keepInQueryCache;
    }

    public ExecuteDataQuerySettings keepInQueryCache() {
        keepInQueryCache = true;
        return this;
    }
}
