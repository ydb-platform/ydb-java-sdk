package tech.ydb.export;

import java.util.concurrent.CompletableFuture;

import tech.ydb.core.grpc.GrpcRequestSettings;
import tech.ydb.core.operation.Operation;
import tech.ydb.proto.export.YdbExport;

/**
 * @author Kirill Kurdyukov
 */
public interface ExportRpc {

    CompletableFuture<Operation<YdbExport.ExportToS3Result>> exportS3(
            YdbExport.ExportToS3Request exportToS3Request,
            GrpcRequestSettings grpcRequestSettings
    );

    CompletableFuture<Operation<YdbExport.ExportToYtResult>> exportYt(
            YdbExport.ExportToYtRequest exportToYtRequest,
            GrpcRequestSettings grpcRequestSettings
    );
}
