package tech.ydb.export;

import java.util.concurrent.CompletableFuture;

import tech.ydb.core.Result;
import tech.ydb.core.grpc.GrpcRequestSettings;
import tech.ydb.core.operation.Operation;
import tech.ydb.proto.export.YdbExport;

/**
 * @author Kirill Kurdyukov
 */
public interface ExportRpc {

    CompletableFuture<Operation<Result<YdbExport.ExportToS3Result>>> exportS3(
            YdbExport.ExportToS3Request request, GrpcRequestSettings settings
    );

    CompletableFuture<Operation<Result<YdbExport.ExportToYtResult>>> exportYt(
            YdbExport.ExportToYtRequest request, GrpcRequestSettings settings
    );
}
