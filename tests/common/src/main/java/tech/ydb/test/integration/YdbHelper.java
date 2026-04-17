package tech.ydb.test.integration;

import tech.ydb.core.grpc.GrpcTransport;
import tech.ydb.core.grpc.GrpcTransportBuilder;

/**
 *
 * @author Aleksandr Gorshenin
 */
public interface YdbHelper extends AutoCloseable {
    @FunctionalInterface
    interface TransportCustomizer {
        GrpcTransportBuilder apply(GrpcTransportBuilder builder);
    }

    GrpcTransport createTransport();

    default GrpcTransport createTransport(TransportCustomizer customizer) {
        return createTransport();
    }

    String endpoint();
    String database();

    boolean useTls();
    byte[] pemCert();

    String authToken();

    @Override
    void close();
}
