package ru.yandex.ydb.table.rpc.grpc;

import java.util.concurrent.CompletableFuture;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import ru.yandex.ydb.OperationProtos.GetOperationRequest;
import ru.yandex.ydb.OperationProtos.GetOperationResponse;
import ru.yandex.ydb.core.Result;
import ru.yandex.ydb.core.grpc.GrpcTransport;
import ru.yandex.ydb.core.rpc.RpcTransport;
import ru.yandex.ydb.operation.v1.OperationServiceGrpc;
import ru.yandex.ydb.table.rpc.OperationRpc;


/**
 * @author Sergey Polovko
 */
@ParametersAreNonnullByDefault
public final class GrpcOperationRpc implements OperationRpc {

    private final GrpcTransport transport;

    private GrpcOperationRpc(GrpcTransport transport) {
        this.transport = transport;
    }

    @Nullable
    public static GrpcOperationRpc create(RpcTransport transport) {
        if (transport instanceof GrpcTransport) {
            return new GrpcOperationRpc((GrpcTransport) transport);
        }
        return null;
    }

    @Override
    public CompletableFuture<Result<GetOperationResponse>> getOperation(GetOperationRequest request) {
        return transport.unaryCall(OperationServiceGrpc.METHOD_GET_OPERATION, request);
    }

    @Override
    public void close() {
        // nop
    }
}
