package tech.ydb.core.operation;

import tech.ydb.OperationProtos;
import tech.ydb.common.CommonProtos;
import tech.ydb.core.grpc.GrpcRequestSettings;
import tech.ydb.core.settings.BaseRequestSettings;
import tech.ydb.core.settings.OperationSettings;
import tech.ydb.core.utils.ProtobufUtils;

/**
 * @author Kirill Kurdyukov
 */
public class OperationUtils {

    private OperationUtils() {
    }

    public static OperationProtos.OperationParams createParams(OperationSettings settings) {
        OperationProtos.OperationParams.Builder builder = OperationProtos.OperationParams.newBuilder();

        if (settings.getOperationTimeout() != null) {
            builder.setOperationTimeout(ProtobufUtils.durationToProto(settings.getOperationTimeout()));
        }
        if (settings.getCancelTimeout() != null) {
            builder.setOperationTimeout(ProtobufUtils.durationToProto(settings.getCancelTimeout()));
        }
        if (settings.getReportCostInfo() != null) {
            if (settings.getReportCostInfo()) {
                builder.setReportCostInfo(CommonProtos.FeatureFlag.Status.ENABLED);
            } else {
                builder.setReportCostInfo(CommonProtos.FeatureFlag.Status.DISABLED);
            }
        }

        builder.setOperationMode(settings.getMode().toProto());

        return builder.build();
    }

    public static GrpcRequestSettings createGrpcRequestSettings(BaseRequestSettings settings) {
        return GrpcRequestSettings.newBuilder()
                .withDeadline(settings.getRequestTimeout())
                .build();
    }
}
