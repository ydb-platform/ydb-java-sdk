package tech.ydb.core.utils;

import java.time.Duration;
import java.util.Optional;

import tech.ydb.core.grpc.GrpcRequestSettings;
import tech.ydb.core.settings.RequestSettings;

/**
 * @author Nikolay Perfilov
 */
public class GrpcUtils {

    private GrpcUtils() { }

    public static GrpcRequestSettings.Builder makeGrpcRequestSettingsBuilder(RequestSettings<?> settings) {
        return GrpcRequestSettings.newBuilder()
                .withDeadlineAfter(calcDeadlineAfter(settings));
    }

    private static long calcDeadlineAfter(RequestSettings<?> settings) {
        Optional<Duration> timeout = settings.getTimeout();
        return timeout.isPresent() ? System.nanoTime() + timeout.get().toNanos() : 0;
    }
}
