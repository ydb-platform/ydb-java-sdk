package tech.ydb.core.tracing;

public interface SpanBuilder {

    SpanBuilder setAttribute(String key, String value);

    SpanBuilder setAttribute(String key, long value);

    Span startSpan();
}
