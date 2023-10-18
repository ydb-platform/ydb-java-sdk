package tech.ydb.export.impl;

import java.util.concurrent.CompletableFuture;

import tech.ydb.core.operation.Operation;
import tech.ydb.core.operation.OperationUtils;
import tech.ydb.export.ExportClient;
import tech.ydb.export.ExportRpc;
import tech.ydb.export.result.ExportToS3Result;
import tech.ydb.export.result.ExportToYtResult;
import tech.ydb.export.settings.ExportToS3Settings;
import tech.ydb.export.settings.ExportToYtSettings;
import tech.ydb.proto.export.YdbExport;

/**
 * @author Kirill Kurdyukov
 */
public class ExportClientImpl implements ExportClient {

    private final ExportRpc exportRpc;

    public ExportClientImpl(ExportRpc exportRpc) {
        this.exportRpc = exportRpc;
    }

    public CompletableFuture<Operation<ExportToS3Result>> exportS3(
            String endpoint,
            String bucket,
            String accessKey,
            String secretKey,
            ExportToS3Settings exportToS3Settings
    ) {
        YdbExport.ExportToS3Settings.Builder exportToYtSettingsBuilder = YdbExport.ExportToS3Settings.newBuilder()
                .setEndpoint(endpoint)
                .setBucket(bucket)
                .setAccessKey(accessKey)
                .setSecretKey(secretKey);

        if (exportToS3Settings.getSchema() != null) {
            exportToYtSettingsBuilder.setScheme(exportToS3Settings.getSchema().toProto());
        }

        if (exportToS3Settings.getNumberOfRetries() != null) {
            exportToYtSettingsBuilder.setNumberOfRetries(exportToS3Settings.getNumberOfRetries());
        }

        if (exportToS3Settings.getStorageClass() != null) {
            exportToYtSettingsBuilder.setStorageClass(exportToS3Settings.getStorageClass().toProto());
        }

        if (exportToS3Settings.getCompression() != null) {
            exportToYtSettingsBuilder.setCompression(exportToS3Settings.getCompression());
        }

        if (exportToS3Settings.getRegion() != null) {
            exportToYtSettingsBuilder.setRegion(exportToS3Settings.getRegion());
        }

        if (exportToS3Settings.getDescription() != null) {
            exportToYtSettingsBuilder.setDescription(exportToS3Settings.getDescription());
        }

        exportToS3Settings.getItemList().forEach(item -> exportToYtSettingsBuilder.addItems(
                        YdbExport.ExportToS3Settings.Item.newBuilder()
                                .setSourcePath(item.getSourcePath())
                                .setDestinationPrefix(item.getDestinationPrefix())
                                .build()
                )
        );

        return exportRpc.exportS3(
                YdbExport.ExportToS3Request.newBuilder()
                        .setSettings(exportToYtSettingsBuilder.build())
                        .setOperationParams(
                                OperationUtils.createParams(exportToS3Settings)
                        )
                        .build(),
                OperationUtils.createGrpcRequestSettings(exportToS3Settings)
        ).thenApply(op -> op.transform(ExportToS3Result::new));
    }

    public CompletableFuture<Operation<ExportToYtResult>> exportYt(
            String host,
            String token,
            ExportToYtSettings exportToYtSettings
    ) {
        YdbExport.ExportToYtSettings.Builder exportToYtSettingBuilder = YdbExport.ExportToYtSettings.newBuilder()
                .setHost(host)
                .setToken(token);

        if (exportToYtSettings.getPort() != null) {
            exportToYtSettingBuilder.setPort(exportToYtSettings.getPort());
        }

        if (exportToYtSettings.getNumberOfRetries() != null) {
            exportToYtSettingBuilder.setNumberOfRetries(exportToYtSettings.getNumberOfRetries());
        }

        if (exportToYtSettings.getUseTypeV3() != null) {
            exportToYtSettingBuilder.setUseTypeV3(exportToYtSettings.getUseTypeV3());
        }

        if (exportToYtSettings.getDescription() != null) {
            exportToYtSettingBuilder.setDescription(exportToYtSettings.getDescription());
        }

        exportToYtSettings.getItemList().forEach(item -> exportToYtSettingBuilder.addItems(
                        YdbExport.ExportToYtSettings.Item.newBuilder()
                                .setSourcePath(item.getSourcePath())
                                .setDestinationPath(item.getDestinationPath())
                                .build()
                )
        );

        return exportRpc.exportYt(
                YdbExport.ExportToYtRequest.newBuilder()
                        .setSettings(exportToYtSettingBuilder.build())
                        .setOperationParams(OperationUtils.createParams(exportToYtSettings))
                        .build(),
                OperationUtils.createGrpcRequestSettings(exportToYtSettings)
        ).thenApply(op -> op.transform(ExportToYtResult::new));
    }
}
