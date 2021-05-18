package tech.ydb.table.query;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import tech.ydb.core.Result;
import tech.ydb.table.settings.ExecuteDataQuerySettings;
import tech.ydb.table.transaction.TxControl;
import tech.ydb.table.values.Type;


/**
 * @author Sergey Polovko
 */
public interface DataQuery {

    String getId();

    Params newParams();

    /**
     * Returns parameter types
     *
     * @return unmodifiable map of types
     */
    Map<String, Type> types();

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
