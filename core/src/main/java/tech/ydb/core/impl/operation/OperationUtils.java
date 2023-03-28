package tech.ydb.core.impl.operation;

import com.google.common.annotations.VisibleForTesting;

import tech.ydb.OperationProtos;
import tech.ydb.common.CommonProtos;
import tech.ydb.core.Issue;
import tech.ydb.core.Status;
import tech.ydb.core.StatusCode;
import tech.ydb.core.settings.OperationSettings;
import tech.ydb.core.utils.ProtobufUtils;


/**
 * @author Sergey Polovko
 * @author Kirill Kurdyukov
 */
public final class OperationUtils {

    private OperationUtils() { }

    @VisibleForTesting
    static Status status(OperationProtos.Operation operation) {
        StatusCode code = StatusCode.fromProto(operation.getStatus());
        Double consumedRu = null;
        if (operation.hasCostInfo()) {
            consumedRu = operation.getCostInfo().getConsumedUnits();
        }

        return Status.of(
                code,
                consumedRu,
                Issue.fromPb(operation.getIssuesList())
        );
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

        return builder.build();
    }
}
