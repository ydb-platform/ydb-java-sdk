package tech.ydb.core.tracing;

public interface Tracer {
    SpanBuilder spanBuilder(String spanName);
}
