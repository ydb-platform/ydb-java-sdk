package tech.ydb.table.query;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import tech.ydb.core.Result;
import tech.ydb.table.settings.ExecuteDataQuerySettings;
import tech.ydb.table.transaction.TxControl;


/**
 * @author Sergey Polovko
 */
public interface DataQuery {

    String getId();

    Params.KnownTypes newParams();

    Optional<String> getText();

    CompletableFuture<Result<DataQueryResult>> execute(
        TxControl txControl, Params params, ExecuteDataQuerySettings settings);

    default CompletableFuture<Result<DataQueryResult>> execute(TxControl txControl, Params params) {
        return execute(txControl, params, new ExecuteDataQuerySettings());
    }

    default CompletableFuture<Result<DataQueryResult>> execute(TxControl txControl) {
        return execute(txControl, Params.empty(), new ExecuteDataQuerySettings());
    }
}
