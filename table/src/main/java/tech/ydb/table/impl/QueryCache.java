package tech.ydb.table.impl;

import javax.annotation.Nullable;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;


/**
 * @author Sergey Polovko
 */
final class QueryCache {

    private final Cache<String, DataQueryImpl> cache;

    QueryCache(int maxSize) {
        this.cache = CacheBuilder.newBuilder()
            .maximumSize(maxSize)
            .build();
    }

    @Nullable
    DataQueryImpl find(String text) {
        String key = DataQueryImpl.makeHash(text);
        return cache.getIfPresent(key);
    }

    void put(DataQueryImpl query) {
        cache.put(query.getTextHash(), query);
    }

    void remove(DataQueryImpl query) {
        cache.invalidate(query.getTextHash());
    }

    void clear() {
        cache.asMap().clear();
    }
}
