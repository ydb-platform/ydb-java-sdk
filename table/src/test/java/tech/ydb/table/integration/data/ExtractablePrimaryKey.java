package tech.ydb.table.integration.data;

import java.util.List;

import tech.ydb.table.utils.Pair;

public interface ExtractablePrimaryKey<T> {
    List<Pair<String, T>> getPrimaryKey();
}