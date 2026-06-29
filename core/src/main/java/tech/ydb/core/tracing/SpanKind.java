package tech.ydb.core.tracing;

import io.grpc.ExperimentalApi;

/**
 * Minimal span kind model.
 *
 * <p>We only need to distinguish:
 * <ul>
 *   <li>{@link #CLIENT}  - a real outbound RPC/DB call</li>
 *   <li>{@link #INTERNAL} - local orchestration/wrapper span (e.g. Retry)</li>
 * </ul>
 *
 * <p>OTel mapping (in otel-impl): SpanKind.CLIENT / SpanKind.INTERNAL.
 */
@ExperimentalApi("YDB Tracer is experimental and API may change without notice")
public enum SpanKind {
    INTERNAL,
    CLIENT
}
