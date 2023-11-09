package tech.ydb.query;

import java.util.concurrent.CompletableFuture;

import tech.ydb.core.Result;
import tech.ydb.core.Status;
import tech.ydb.core.grpc.GrpcReadStream;
import tech.ydb.proto.query.YdbQuery;
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
    interface Tx {
        YdbQuery.TransactionControl toPb();
    }

    CompletableFuture<Result<TxId>> beginTransaction(TxMode txMode, BeginTransactionSettings settings);
    CompletableFuture<Status> commitTransaction(TxId tx, CommitTransactionSettings settings);
    CompletableFuture<Status> rollbackTransaction(TxId tx, RollbackTransactionSettings settings);

    GrpcReadStream<QueryResultPart> executeQuery(String query, Tx tx, Params params, ExecuteQuerySettings settings);

    @Override
    void close();

    default GrpcReadStream<QueryResultPart> executeQuery(String query, Tx tx) {
        return executeQuery(query, tx, Params.empty(), ExecuteQuerySettings.newBuilder().build());
    }

    default GrpcReadStream<QueryResultPart> executeQuery(String query, Tx tx, Params params) {
        return executeQuery(query, tx, params, ExecuteQuerySettings.newBuilder().build());
    }

    default CompletableFuture<Result<TxId>> beginTransaction(TxMode tx) {
        return beginTransaction(tx, BeginTransactionSettings.newBuilder().build());
    }

    default CompletableFuture<Status> commitTransaction(TxId tx) {
        return commitTransaction(tx, CommitTransactionSettings.newBuilder().build());
    }

    default CompletableFuture<Status> rollbackTransaction(TxId tx) {
        return rollbackTransaction(tx, RollbackTransactionSettings.newBuilder().build());
    }
}
