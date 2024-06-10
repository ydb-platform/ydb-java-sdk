package tech.ydb.export.impl;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import tech.ydb.core.Result;
import tech.ydb.core.grpc.GrpcRequestSettings;
import tech.ydb.core.operation.Operation;
import tech.ydb.core.settings.BaseRequestSettings;
import tech.ydb.export.ExportClient;
import tech.ydb.export.ExportRpc;
import tech.ydb.export.result.ExportToS3Result;
import tech.ydb.export.result.ExportToYtResult;
import tech.ydb.export.settings.ExportToS3Settings;
import tech.ydb.export.settings.ExportToYtSettings;
import tech.ydb.export.settings.FindExportSettings;
import tech.ydb.proto.export.YdbExport;

/**
 * @author Kirill Kurdyukov
 */
public class ExportClientImpl implements ExportClient {

    private final ExportRpc exportRpc;

    public ExportClientImpl(ExportRpc exportRpc) {
        this.exportRpc = exportRpc;
    }

    private GrpcRequestSettings makeGrpcRequestSettings(BaseRequestSettings settings) {
        String traceId = settings.getTraceId() == null ? UUID.randomUUID().toString() : settings.getTraceId();
        return GrpcRequestSettings.newBuilder()
                .withDeadline(settings.getRequestTimeout())
                .withTraceId(traceId)
                .build();
    }

    @Override
    public CompletableFuture<Operation<Result<ExportToS3Result>>> startExportToS3(
            String endpoint, String bucket, String accessKey, String secretKey, ExportToS3Settings settings
    ) {
        YdbExport.ExportToS3Settings.Builder builder = YdbExport.ExportToS3Settings.newBuilder()
                .setEndpoint(endpoint)
                .setBucket(bucket)
                .setAccessKey(accessKey)
                .setSecretKey(secretKey);

        if (settings.getSchema() != null) {
            builder.setScheme(settings.getSchema().toProto());
        }

        if (settings.getNumberOfRetries() != null) {
            builder.setNumberOfRetries(settings.getNumberOfRetries());
        }

        if (settings.getStorageClass() != null) {
            builder.setStorageClass(settings.getStorageClass().toProto());
        }

        if (settings.getCompression() != null) {
            builder.setCompression(settings.getCompression());
        }

        if (settings.getRegion() != null) {
            builder.setRegion(settings.getRegion());
        }

        if (settings.getDescription() != null) {
            builder.setDescription(settings.getDescription());
        }

        for (ExportToS3Settings.Item item : settings.getItemList()) {
            builder.addItems(
                    YdbExport.ExportToS3Settings.Item.newBuilder()
                            .setSourcePath(item.getSourcePath())
                            .setDestinationPrefix(item.getDestinationPrefix())
                            .build()
            );
        }

        YdbExport.ExportToS3Request request = YdbExport.ExportToS3Request.newBuilder()
                .setSettings(builder.build())
                .setOperationParams(Operation.buildParams(settings))
                .build();

        return exportRpc.exportS3(request, makeGrpcRequestSettings(settings))
                .thenApply(op -> op.transform(r -> r.map(ExportToS3Result::new)));
    }

    @Override
    public CompletableFuture<Operation<Result<ExportToYtResult>>> startExportToYt(
            String host, String token, ExportToYtSettings settings
    ) {
        YdbExport.ExportToYtSettings.Builder builder = YdbExport.ExportToYtSettings.newBuilder()
                .setHost(host)
                .setToken(token);

        if (settings.getPort() != null) {
            builder.setPort(settings.getPort());
        }

        if (settings.getNumberOfRetries() != null) {
            builder.setNumberOfRetries(settings.getNumberOfRetries());
        }

        if (settings.getUseTypeV3() != null) {
            builder.setUseTypeV3(settings.getUseTypeV3());
        }

        if (settings.getDescription() != null) {
            builder.setDescription(settings.getDescription());
        }

        for (ExportToYtSettings.Item item : settings.getItemList()) {
            builder.addItems(
                    YdbExport.ExportToYtSettings.Item.newBuilder()
                            .setSourcePath(item.getSourcePath())
                            .setDestinationPath(item.getDestinationPath())
                            .build()
            );
        }

        YdbExport.ExportToYtRequest request = YdbExport.ExportToYtRequest.newBuilder()
                .setSettings(builder.build())
                .setOperationParams(Operation.buildParams(settings))
                .build();

        return exportRpc.exportYt(request, makeGrpcRequestSettings(settings))
                .thenApply(op -> op.transform(r -> r.map(ExportToYtResult::new)));
    }

    @Override
    public CompletableFuture<Operation<Result<ExportToS3Result>>> findExportToS3(
            String operationId, FindExportSettings settings
    ) {
        return exportRpc.findExportToS3(operationId, makeGrpcRequestSettings(settings))
                .thenApply(op -> op.transform(r -> r.map(ExportToS3Result::new)));
    }

    @Override
    public CompletableFuture<Operation<Result<ExportToYtResult>>> findExportToYT(
            String operationId, FindExportSettings settings
    ) {
        return exportRpc.findExportToYT(operationId, makeGrpcRequestSettings(settings))
                .thenApply(op -> op.transform(r -> r.map(ExportToYtResult::new)));
    }
}
