package tech.ydb.test.integration;

import tech.ydb.core.grpc.GrpcTransport;

/**
 *
 * @author Aleksandr Gorshenin
 */
public interface YdbHelper extends AutoCloseable {
    GrpcTransport createTransport();

    String endpoint();
    String database();

    boolean useTls();
    byte[] pemCert();

    String authToken();

    String getStdErr();

    @Override
    void close();
}
