package tech.ydb.query;

import java.util.concurrent.CompletableFuture;

import tech.ydb.core.Result;
import tech.ydb.core.Status;
import tech.ydb.core.grpc.GrpcReadStream;
import tech.ydb.query.result.QueryResultPart;
import tech.ydb.query.settings.BeginTransactionSettings;
import tech.ydb.query.settings.CommitTransactionSettings;
import tech.ydb.query.settings.ExecuteQuerySettings;
import tech.ydb.query.settings.RollbackTransactionSettings;
import tech.ydb.table.query.Params;

/**
 *
 * @author Aleksandr Gorshenin
 */
public interface QuerySession extends AutoCloseable {
    CompletableFuture<Result<QueryTx.Id>> beginTransaction(QueryTx.Mode txMode, BeginTransactionSettings settings);
    CompletableFuture<Status> commitTransaction(QueryTx.Id tx, CommitTransactionSettings settings);
    CompletableFuture<Status> rollbackTransaction(QueryTx.Id tx, RollbackTransactionSettings settings);

    GrpcReadStream<QueryResultPart> executeQuery(String query, QueryTx tx, Params prms, ExecuteQuerySettings settings);

    @Override
    void close();

    default GrpcReadStream<QueryResultPart> executeQuery(String query, QueryTx tx) {
        return executeQuery(query, tx, Params.empty(), ExecuteQuerySettings.newBuilder().build());
    }

    default GrpcReadStream<QueryResultPart> executeQuery(String query, QueryTx tx, Params prms) {
        return executeQuery(query, tx, prms, ExecuteQuerySettings.newBuilder().build());
    }

    default CompletableFuture<Result<QueryTx.Id>> beginTransaction(QueryTx.Mode tx) {
        return beginTransaction(tx, BeginTransactionSettings.newBuilder().build());
    }

    default CompletableFuture<Status> commitTransaction(QueryTx.Id tx) {
        return commitTransaction(tx, CommitTransactionSettings.newBuilder().build());
    }

    default CompletableFuture<Status> rollbackTransaction(QueryTx.Id tx) {
        return rollbackTransaction(tx, RollbackTransactionSettings.newBuilder().build());
    }
}
