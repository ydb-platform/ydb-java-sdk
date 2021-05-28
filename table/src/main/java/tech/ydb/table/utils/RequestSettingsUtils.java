package tech.ydb.table.utils;

import java.time.Duration;
import java.util.Optional;

import tech.ydb.table.settings.RequestSettings;

public class RequestSettingsUtils {
    public static long calculateDeadlineAfter(RequestSettings<?> settings) {
        Optional<Duration> clientTimeout = settings.getTimeout();
        if (clientTimeout.isPresent()) {
            if (clientTimeout.get().equals(Duration.ZERO)) {
                return 0;
            }
            return System.nanoTime() + clientTimeout.get().toNanos();
        }
        return settings.getDeadlineAfter();
    }
}
