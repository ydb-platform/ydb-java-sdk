package tech.ydb.core.utils;

import java.time.Instant;

import javax.annotation.Nullable;

import com.google.protobuf.Duration;
import com.google.protobuf.Timestamp;

import tech.ydb.OperationProtos;
import tech.ydb.common.CommonProtos;
import tech.ydb.core.settings.RequestSettings;

/**
 * @author Nikolay Perfilov
 */
public class ProtoUtils {

    private ProtoUtils() { }

    @Nullable
    public static Duration toProto(@Nullable java.time.Duration duration) {
        return duration == null
                ? null
                : Duration.newBuilder()
                    .setSeconds(duration.getSeconds())
                    .setNanos(duration.getNano())
                    .build();
    }

    public static Timestamp toProto(java.time.Instant instant) {
        return Timestamp.newBuilder()
                .setSeconds(instant.getEpochSecond())
                .setNanos(instant.getNano())
                .build();
    }

    @Nullable
    public static java.time.Duration fromProto(@Nullable Duration duration) {
        return duration == null
                ? null
                : java.time.Duration.ofSeconds(duration.getSeconds(), duration.getNanos());
    }

    public static java.time.Instant fromProto(Timestamp timestamp) {
        return Instant.ofEpochSecond(timestamp.getSeconds(), timestamp.getNanos());
    }

    public static OperationProtos.OperationParams fromRequestSettings(RequestSettings<?> requestSettings) {
        OperationProtos.OperationParams.Builder builder = OperationProtos.OperationParams.newBuilder();
        requestSettings.getOperationTimeout()
                .ifPresent(duration -> builder.setOperationTimeout(ProtoUtils.toProto(duration)));
        requestSettings.getCancelAfter().ifPresent(duration -> builder.setCancelAfter(ProtoUtils.toProto(duration)));
        requestSettings.getReportCostInfo().ifPresent(report ->
                builder.setReportCostInfo(report ? CommonProtos.FeatureFlag.Status.ENABLED :
                        CommonProtos.FeatureFlag.Status.DISABLED)
        );
        return builder.build();
    }
}
