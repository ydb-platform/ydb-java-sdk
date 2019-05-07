package ru.yandex.ydb.table.query;

import java.util.concurrent.CompletableFuture;

import ru.yandex.ydb.core.Result;
import ru.yandex.ydb.table.settings.ExecuteDataQuerySettings;
import ru.yandex.ydb.table.transaction.TxControl;


/**
 * @author Sergey Polovko
 */
public interface DataQuery {

    String getId();

    Params.KnownTypes newParams();

    CompletableFuture<Result<DataQueryResult>> execute(
        TxControl txControl, Params params, ExecuteDataQuerySettings settings);

    default CompletableFuture<Result<DataQueryResult>> execute(TxControl txControl, Params params) {
        return execute(txControl, params, new ExecuteDataQuerySettings());
    }

    default CompletableFuture<Result<DataQueryResult>> execute(TxControl txControl) {
        return execute(txControl, Params.empty(), new ExecuteDataQuerySettings());
    }
}
