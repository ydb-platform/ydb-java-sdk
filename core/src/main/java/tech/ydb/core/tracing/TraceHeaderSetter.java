package tech.ydb.core.tracing;

@FunctionalInterface
public interface TraceHeaderSetter {
    void set(String key, String value);
}
