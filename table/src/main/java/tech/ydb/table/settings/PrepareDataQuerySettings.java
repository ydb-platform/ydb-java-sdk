package tech.ydb.table.settings;

import tech.ydb.core.settings.RequestSettings;

/**
 * @author Sergey Polovko
 */
public class PrepareDataQuerySettings extends RequestSettings<PrepareDataQuerySettings> {

    private boolean keepInQueryCache = false;

    public boolean isKeepInQueryCache() {
        return keepInQueryCache;
    }

    public PrepareDataQuerySettings keepInQueryCache() {
        keepInQueryCache = true;
        return this;
    }
}
