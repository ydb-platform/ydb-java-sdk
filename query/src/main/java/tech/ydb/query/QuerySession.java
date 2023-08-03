package tech.ydb.query;

import tech.ydb.core.grpc.GrpcReadStream;
import tech.ydb.query.result.QueryResultPart;
import tech.ydb.query.settings.ExecuteQuerySettings;

/**
 *
 * @author Aleksandr Gorshenin
 */
public interface QuerySession extends AutoCloseable {

    GrpcReadStream<QueryResultPart> executeQuery(String query, TxMode tx, ExecuteQuerySettings settings);

    @Override
    void close();
}
