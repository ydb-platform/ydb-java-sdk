package tech.ydb.topic.impl;

import java.util.Arrays;

import tech.ydb.core.StatusCode;
import tech.ydb.core.metrics.Attr;

public final class TopicMetricsUtils {
    private TopicMetricsUtils() {
    }

    public static Attr[] sessionErrorAttributes(Attr[] commonAttributes, StatusCode code, boolean retry) {
        Attr[] attributes = Arrays.copyOf(commonAttributes, commonAttributes.length + 3);
        attributes[commonAttributes.length] = Attr.of("retry_decision", retry ? "retry" : "stop");
        attributes[commonAttributes.length + 1] = Attr.of("status_code", code.name());
        String errorType = code == StatusCode.SUCCESS || code == StatusCode.UNUSED_STATUS
                ? "session_closed" : code.isTransportError() ? "transport_error" : "ydb_error";
        attributes[commonAttributes.length + 2] = Attr.of("error.type", errorType);
        return attributes;
    }
}
