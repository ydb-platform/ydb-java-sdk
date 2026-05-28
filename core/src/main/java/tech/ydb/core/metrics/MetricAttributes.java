package tech.ydb.core.metrics;

import io.grpc.ExperimentalApi;

/**
 * Attribute keys shared by SDK metrics across modules (query, topic, ...).
 */
@ExperimentalApi("YDB Meter is experimental and API may change without notice")
public final class MetricAttributes {
    public static final String DATABASE = "database";
    public static final String ENDPOINT = "endpoint";
    public static final String OPERATION_NAME = "operation.name";
    public static final String STATUS_CODE = "status_code";

    private MetricAttributes() {
    }
}
