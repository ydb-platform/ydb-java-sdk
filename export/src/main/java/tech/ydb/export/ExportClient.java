package tech.ydb.export;

import java.util.concurrent.CompletableFuture;

import javax.annotation.WillNotClose;

import tech.ydb.core.Result;
import tech.ydb.core.grpc.GrpcTransport;
import tech.ydb.core.operation.Operation;
import tech.ydb.core.operation.OperationTray;
import tech.ydb.export.impl.ExportClientImpl;
import tech.ydb.export.impl.GrpcExportRpcImpl;
import tech.ydb.export.result.ExportToS3Result;
import tech.ydb.export.result.ExportToYtResult;
import tech.ydb.export.settings.ExportToS3Settings;
import tech.ydb.export.settings.ExportToYtSettings;

/**
 * @author Kirill Kurdyukov
 */
public interface ExportClient {

    static ExportClient newClient(@WillNotClose GrpcTransport transport) {
        return new ExportClientImpl(GrpcExportRpcImpl.useTransport(transport));
    }

    CompletableFuture<Operation<Result<ExportToS3Result>>> startExportToS3(
            String endpoint, String bucket, String accessKey, String secretKey, ExportToS3Settings settings
    );

    CompletableFuture<Operation<Result<ExportToYtResult>>> startExportToYt(
            String host, String token, ExportToYtSettings settings
    );

    default CompletableFuture<Result<ExportToS3Result>> exportToS3(
            String endpoint, String bucket, String accessKey, String secretKey, ExportToS3Settings settings,
            int updateRateSeconds
    ) {
        return startExportToS3(endpoint, bucket, accessKey, secretKey, settings)
                .thenCompose(operation -> OperationTray.fetchOperation(operation, updateRateSeconds));
    }

    default CompletableFuture<Result<ExportToYtResult>> startExportToYt(
            String host, String token, ExportToYtSettings settings, int updateRateSeconds
    ) {
        return startExportToYt(host, token, settings)
                .thenCompose(operation -> OperationTray.fetchOperation(operation, updateRateSeconds));
    }
}
