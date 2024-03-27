package tech.ydb.core.operation;

import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

import javax.annotation.Nullable;

import tech.ydb.core.Result;
import tech.ydb.core.Status;
import tech.ydb.core.settings.OperationSettings;
import tech.ydb.core.utils.ProtobufUtils;
import tech.ydb.proto.OperationProtos;
import tech.ydb.proto.common.CommonProtos;

/**
 * @author Kirill Kurdyukov
 * @author Aleksandr Gorshenin
 * @param <T> type of the operation result
 */
public interface Operation<T> {

    @Nullable
    String getId();

    boolean isReady();

    @Nullable
    T getValue();

    CompletableFuture<Status> cancel();
    CompletableFuture<Status> forget();

    CompletableFuture<Result<Boolean>> fetch();

    <R> Operation<R> transform(Function<T, R> mapper);

    static OperationProtos.OperationParams buildParams(OperationSettings settings) {
        OperationProtos.OperationParams.Builder builder = OperationProtos.OperationParams.newBuilder();

        if (settings.getOperationTimeout() != null) {
            builder.setOperationTimeout(ProtobufUtils.durationToProto(settings.getOperationTimeout()));
        }
        if (settings.getCancelTimeout() != null) {
            builder.setCancelAfter(ProtobufUtils.durationToProto(settings.getCancelTimeout()));
        }
        if (settings.getReportCostInfo() != null) {
            if (settings.getReportCostInfo()) {
                builder.setReportCostInfo(CommonProtos.FeatureFlag.Status.ENABLED);
            } else {
                builder.setReportCostInfo(CommonProtos.FeatureFlag.Status.DISABLED);
            }
        }

        if (settings.isAsyncMode()) {
            builder.setOperationMode(OperationProtos.OperationParams.OperationMode.ASYNC);
        } else {
            builder.setOperationMode(OperationProtos.OperationParams.OperationMode.SYNC);
        }

        return builder.build();
    }
}
