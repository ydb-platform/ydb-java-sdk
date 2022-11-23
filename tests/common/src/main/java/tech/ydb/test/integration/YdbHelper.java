package tech.ydb.test.integration;

import tech.ydb.core.grpc.GrpcTransport;

/**
 *
 * @author Aleksandr Gorshenin
 */
public interface YdbHelper {
    GrpcTransport createTransport();
}
