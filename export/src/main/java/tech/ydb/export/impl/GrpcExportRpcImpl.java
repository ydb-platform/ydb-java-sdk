package tech.ydb.export.impl;

import java.util.concurrent.CompletableFuture;

import javax.annotation.WillNotClose;

import tech.ydb.core.Result;
import tech.ydb.core.grpc.GrpcRequestSettings;
import tech.ydb.core.grpc.GrpcTransport;
import tech.ydb.core.operation.Operation;
import tech.ydb.core.operation.OperationBinder;
import tech.ydb.export.ExportRpc;
import tech.ydb.proto.OperationProtos;
import tech.ydb.proto.export.YdbExport;
import tech.ydb.proto.export.v1.ExportServiceGrpc;
import tech.ydb.proto.operation.v1.OperationServiceGrpc;

/**
 * @author Kirill Kurdyukov
 */
public class GrpcExportRpcImpl implements ExportRpc {
    private final GrpcTransport transport;

    private GrpcExportRpcImpl(GrpcTransport grpcTransport) {
        this.transport = grpcTransport;
    }

    public static GrpcExportRpcImpl useTransport(@WillNotClose GrpcTransport grpcTransport) {
        return new GrpcExportRpcImpl(grpcTransport);
    }

    @Override
    public CompletableFuture<Operation<Result<YdbExport.ExportToS3Result>>> exportS3(
            YdbExport.ExportToS3Request request,
            GrpcRequestSettings settings
    ) {
        return transport.unaryCall(ExportServiceGrpc.getExportToS3Method(), settings, request)
                .thenApply(OperationBinder.bindAsync(
                        transport, YdbExport.ExportToS3Response::getOperation, YdbExport.ExportToS3Result.class)
                );
    }

    @Override
    public CompletableFuture<Operation<Result<YdbExport.ExportToYtResult>>> exportYt(
            YdbExport.ExportToYtRequest request,
            GrpcRequestSettings settings
    ) {
        return transport.unaryCall(ExportServiceGrpc.getExportToYtMethod(), settings, request)
                .thenApply(OperationBinder.bindAsync(
                        transport, YdbExport.ExportToYtResponse::getOperation, YdbExport.ExportToYtResult.class
                )
        );
    }

    @Override
    public CompletableFuture<Operation<Result<YdbExport.ExportToS3Result>>> findExportToS3(
            String operationId, GrpcRequestSettings settings
    ) {
        OperationProtos.GetOperationRequest request = OperationProtos.GetOperationRequest.newBuilder()
                        .setId(operationId)
                        .build();
        return transport
                .unaryCall(OperationServiceGrpc.getGetOperationMethod(), settings, request)
                .thenApply(OperationBinder.bindAsync(
                        transport, OperationProtos.GetOperationResponse::getOperation, YdbExport.ExportToS3Result.class
                ));
    }

    @Override
    public CompletableFuture<Operation<Result<YdbExport.ExportToYtResult>>> findExportToYT(
            String operationId, GrpcRequestSettings settings
    ) {
        OperationProtos.GetOperationRequest request = OperationProtos.GetOperationRequest.newBuilder()
                        .setId(operationId)
                        .build();
        return transport
                .unaryCall(OperationServiceGrpc.getGetOperationMethod(), settings, request)
                .thenApply(OperationBinder.bindAsync(
                        transport, OperationProtos.GetOperationResponse::getOperation, YdbExport.ExportToYtResult.class
                ));
    }
}
