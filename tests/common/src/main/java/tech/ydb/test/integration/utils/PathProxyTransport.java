package tech.ydb.test.integration.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tech.ydb.core.Status;
import tech.ydb.core.grpc.GrpcRequestSettings;
import tech.ydb.core.grpc.GrpcTransport;
import tech.ydb.scheme.SchemeOperationProtos;
import tech.ydb.scheme.v1.SchemeServiceGrpc;

/**
 *
 * @author Aleksandr Gorshenin
 */
public class PathProxyTransport extends ProxyGrpcTransport {
    private static final Logger logger = LoggerFactory.getLogger(PathProxyTransport.class);

    private final GrpcTransport transport;
    private final String path;
    private final boolean dropOnExit;

    public PathProxyTransport(GrpcTransport transport, String path, boolean dropOnExit) {
        this.transport = transport;
        this.path = path;
        this.dropOnExit = dropOnExit;
    }

    public void init() {
        SchemeOperationProtos.MakeDirectoryRequest request = SchemeOperationProtos.MakeDirectoryRequest
                .newBuilder()
                .setPath(path)
                .build();

        GrpcRequestSettings settings = GrpcRequestSettings.newBuilder()
                .build();

        transport.unaryCall(SchemeServiceGrpc.getMakeDirectoryMethod(), settings, request)
                .join()
                .getStatus().expectSuccess("can't create path " + path);

        logger.debug("test path {} is created", path);
    }

    @Override
    protected GrpcTransport origin() {
        return transport;
    }

    @Override
    public String getDatabase() {
        return transport.getDatabase() + "/" + path;
    }

    @Override
    public void close() {
        if (dropOnExit) {
            SchemeOperationProtos.RemoveDirectoryRequest request = SchemeOperationProtos.RemoveDirectoryRequest
                    .newBuilder()
                    .setPath(path)
                    .build();

            GrpcRequestSettings settings = GrpcRequestSettings.newBuilder()
                    .build();

            Status status = transport
                    .unaryCall(SchemeServiceGrpc.getRemoveDirectoryMethod(), settings, request)
                    .join()
                    .getStatus();

            if (!status.isSuccess()) {
                logger.warn("can't remove test path {} with status {}", path, status);

            }
        }
        transport.close();
    }
}
