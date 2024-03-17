package tech.ydb.query;

import java.util.concurrent.CompletableFuture;

import tech.ydb.core.Result;
import tech.ydb.query.settings.BeginTransactionSettings;
import tech.ydb.query.settings.ExecuteQuerySettings;
import tech.ydb.table.query.Params;

/**
 *
 * @author Aleksandr Gorshenin
 */
public interface QuerySession extends AutoCloseable {
    String getId();

    QueryTransaction currentTransaction();

    QueryTransaction createNewTransaction(QueryTx txMode);

    CompletableFuture<Result<QueryTransaction>> beginTransaction(QueryTx txMode, BeginTransactionSettings settings);

    QueryStream createQuery(String query, QueryTx tx, Params prms, ExecuteQuerySettings settings);

    @Override
    void close();

    default QueryStream createQuery(String query, QueryTx tx, Params prms) {
        return createQuery(query, tx, prms, ExecuteQuerySettings.newBuilder().build());
    }

    default QueryStream createQuery(String query, QueryTx tx) {
        return createQuery(query, tx, Params.empty(), ExecuteQuerySettings.newBuilder().build());
    }

    default CompletableFuture<Result<QueryTransaction>> beginTransaction(QueryTx tx) {
        return beginTransaction(tx, BeginTransactionSettings.newBuilder().build());
    }
}
