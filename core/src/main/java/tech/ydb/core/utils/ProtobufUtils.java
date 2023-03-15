package tech.ydb.core.utils;

import java.time.Instant;

import javax.annotation.Nullable;

import com.google.protobuf.Duration;
import com.google.protobuf.Timestamp;

/**
 * @author Nikolay Perfilov
 */
public class ProtobufUtils {

    private ProtobufUtils() { }

    @Nullable
    public static Duration toDuration(@Nullable java.time.Duration duration) {
        return duration == null
                ? null
                : Duration.newBuilder()
                    .setSeconds(duration.getSeconds())
                    .setNanos(duration.getNano())
                    .build();
    }

    public static Timestamp toTimestamp(java.time.Instant instant) {
        return Timestamp.newBuilder()
                .setSeconds(instant.getEpochSecond())
                .setNanos(instant.getNano())
                .build();
    }

    @Nullable
    public static java.time.Duration fromDuration(@Nullable Duration duration) {
        return duration == null
                ? null
                : java.time.Duration.ofSeconds(duration.getSeconds(), duration.getNanos());
    }

    public static java.time.Instant fromTimestamp(Timestamp timestamp) {
        return Instant.ofEpochSecond(timestamp.getSeconds(), timestamp.getNanos());
    }
}
