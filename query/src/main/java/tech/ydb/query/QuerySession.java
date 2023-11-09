package tech.ydb.query;

import java.util.concurrent.CompletableFuture;

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
    CompletableFuture<Status> beginTransaction(TxMode tx, BeginTransactionSettings settings);
    CompletableFuture<Status> commitTransaction(TxMode tx, CommitTransactionSettings settings);
    CompletableFuture<Status> rollbackTransaction(TxMode tx, RollbackTransactionSettings settings);

    GrpcReadStream<QueryResultPart> executeQuery(String query, TxMode tx, Params params, ExecuteQuerySettings settings);

    @Override
    void close();

    default GrpcReadStream<QueryResultPart> executeQuery(String query, TxMode tx) {
        return executeQuery(query, tx, Params.empty(), ExecuteQuerySettings.newBuilder().build());
    }

    default GrpcReadStream<QueryResultPart> executeQuery(String query, TxMode tx, Params params) {
        return executeQuery(query, tx, params, ExecuteQuerySettings.newBuilder().build());
    }

    default CompletableFuture<Status> beginTransaction(TxMode tx) {
        return beginTransaction(tx, BeginTransactionSettings.newBuilder().build());
    }

    default CompletableFuture<Status> commitTransaction(TxMode tx) {
        return commitTransaction(tx, CommitTransactionSettings.newBuilder().build());
    }

    default CompletableFuture<Status> rollbackTransaction(TxMode tx) {
        return rollbackTransaction(tx, RollbackTransactionSettings.newBuilder().build());
    }
}
