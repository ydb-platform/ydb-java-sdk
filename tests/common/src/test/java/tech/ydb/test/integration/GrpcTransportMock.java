package tech.ydb.test.integration;


import java.util.concurrent.CompletableFuture;

import org.mockito.MockedStatic;
import org.mockito.Mockito;

import tech.ydb.core.Result;
import tech.ydb.core.grpc.GrpcTransport;
import tech.ydb.core.grpc.GrpcTransportBuilder;

/**
 *
 * @author Aleksandr Gorshenin
 */
public class GrpcTransportMock implements AutoCloseable {
    private final MockedStatic<GrpcTransport> staticGrpcTransport = Mockito.mockStatic(GrpcTransport.class);

    private final GrpcTransportBuilder builderMock = Mockito.mock(GrpcTransportBuilder.class);
    private final GrpcTransport transportMock = Mockito.mock(GrpcTransport.class);

    public void setup(String database) {
        staticGrpcTransport.when(() -> GrpcTransport.forEndpoint(Mockito.any(), Mockito.any()))
                .thenReturn(builderMock);

        Mockito.when(builderMock.build()).thenReturn(transportMock);

        Mockito.when(transportMock.getDatabase()).thenReturn(database);
        Mockito.when(transportMock.unaryCall(Mockito.any(), Mockito.any(), Mockito.any()))
                .thenReturn(CompletableFuture.completedFuture(Result.success(Boolean.TRUE)));
    }

    @Override
    public void close() {
        staticGrpcTransport.close();
    }
}
