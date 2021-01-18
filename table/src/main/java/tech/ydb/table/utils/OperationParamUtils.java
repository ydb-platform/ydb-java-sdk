package tech.ydb.table.utils;

import com.google.protobuf.Duration;
import tech.ydb.OperationProtos;
import tech.ydb.table.settings.RequestSettings;

public class OperationParamUtils {

    private OperationParamUtils() {
    }

    public static OperationProtos.OperationParams fromRequestSettings(RequestSettings<?> requestSettings) {
        OperationProtos.OperationParams.Builder builder = OperationProtos.OperationParams.newBuilder();
        requestSettings.getOperationTimeout().ifPresent(duration -> builder.setOperationTimeout(toProto(duration)));
        requestSettings.getCancelAfter().ifPresent(duration -> builder.setCancelAfter(toProto(duration)));
        return builder.build();
    }

    private static Duration toProto(java.time.Duration duration) {
        return Duration.newBuilder()
                .setSeconds(duration.getSeconds())
                .setNanos(duration.getNano())
                .build();
    }
}
