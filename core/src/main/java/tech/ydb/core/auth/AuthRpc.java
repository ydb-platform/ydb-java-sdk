package tech.ydb.core.auth;

import tech.ydb.core.grpc.GrpcTransport;

/**
 *
 * @author Aleksandr Gorshenin
 */
public interface AuthRpc {
    String getEndpoint();
    String getDatabase();

    GrpcTransport createTransport();
}
