package tech.ydb.export;

import java.util.concurrent.CompletableFuture;

import tech.ydb.core.grpc.GrpcRequestSettings;
import tech.ydb.core.operation.OperationManager;

/**
 * @author Kirill Kurdyukov
 */
public interface ExportRpc {

    CompletableFuture<OperationManager.Operation<YdbExport.ExportToS3Result>> exportS3(
            YdbExport.ExportToS3Request exportToS3Request,
            GrpcRequestSettings grpcRequestSettings
    );

    CompletableFuture<OperationManager.Operation<YdbExport.ExportToYtResult>> exportYt(
            YdbExport.ExportToYtRequest exportToYtRequest,
            GrpcRequestSettings grpcRequestSettings
    );
}
