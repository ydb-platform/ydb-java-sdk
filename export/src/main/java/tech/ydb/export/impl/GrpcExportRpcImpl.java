package tech.ydb.export.impl;

import java.util.concurrent.CompletableFuture;

import javax.annotation.WillNotClose;

import tech.ydb.core.grpc.GrpcRequestSettings;
import tech.ydb.core.grpc.GrpcTransport;
import tech.ydb.core.operation.Operation;
import tech.ydb.core.operation.OperationManager;
import tech.ydb.export.ExportRpc;
import tech.ydb.export.YdbExport;
import tech.ydb.export.v1.ExportServiceGrpc;

/**
 * @author Kirill Kurdyukov
 */
public class GrpcExportRpcImpl implements ExportRpc {

    private final GrpcTransport grpcTransport;
    private final OperationManager operationManager;

    private GrpcExportRpcImpl(GrpcTransport grpcTransport) {
        this.grpcTransport = grpcTransport;
        this.operationManager = new OperationManager(grpcTransport);
    }

    public static GrpcExportRpcImpl useTransport(@WillNotClose GrpcTransport grpcTransport) {
        return new GrpcExportRpcImpl(grpcTransport);
    }

    @Override
    public CompletableFuture<Operation<YdbExport.ExportToS3Result>> exportS3(
            YdbExport.ExportToS3Request exportToS3Request,
            GrpcRequestSettings grpcRequestSettings
    ) {
        return grpcTransport.unaryCall(
                ExportServiceGrpc.getExportToS3Method(),
                grpcRequestSettings,
                exportToS3Request
        ).thenApply(
                operationManager.operationUnwrapper(
                        YdbExport.ExportToS3Response::getOperation,
                        YdbExport.ExportToS3Result.class
                )
        );
    }

    @Override
    public CompletableFuture<Operation<YdbExport.ExportToYtResult>> exportYt(
            YdbExport.ExportToYtRequest exportToYtRequest,
            GrpcRequestSettings grpcRequestSettings
    ) {
        return grpcTransport.unaryCall(
                ExportServiceGrpc.getExportToYtMethod(),
                grpcRequestSettings,
                exportToYtRequest
        ).thenApply(
                operationManager.operationUnwrapper(
                        YdbExport.ExportToYtResponse::getOperation,
                        YdbExport.ExportToYtResult.class
                )
        );
    }
}
