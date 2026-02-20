package tech.ydb.core.tracing;

public interface Span {
    Scope makeCurrent();

    void injectHeaders(TraceHeaderSetter headerSetter);

    Span setAttribute(String key, String value);

    Span setAttribute(String key, long value);

    Span recordException(Throwable error);

    void end();
}
