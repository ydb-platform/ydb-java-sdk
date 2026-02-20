package tech.ydb.observability;

import tech.ydb.core.Status;

public interface TracingSpan {
    TracingSpan setAttribute(String key, String value);

    TracingSpan setAttribute(String key, long value);

    TracingSpan setAttribute(String key, boolean value);

    TracingSpan setStatus(Status status);

    TracingSpan recordException(Throwable error);

    void end();
}
