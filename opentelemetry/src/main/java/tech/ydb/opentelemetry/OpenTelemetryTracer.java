package tech.ydb.opentelemetry;

import java.util.Objects;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.AttributeKey;

import tech.ydb.core.tracing.Span;
import tech.ydb.core.tracing.SpanKind;
import tech.ydb.core.tracing.Tracer;

public final class OpenTelemetryTracer implements Tracer {
    private static final String DEFAULT_SCOPE = "tech.ydb.sdk";

    private final io.opentelemetry.api.trace.Tracer tracer;

    private OpenTelemetryTracer(io.opentelemetry.api.trace.Tracer tracer) {
        this.tracer = Objects.requireNonNull(tracer, "tracer is null");
    }

    public static OpenTelemetryTracer createGlobal() {
        return fromOpenTelemetry(GlobalOpenTelemetry.get());
    }

    public static OpenTelemetryTracer fromOpenTelemetry(OpenTelemetry openTelemetry) {
        return fromOpenTelemetry(openTelemetry, DEFAULT_SCOPE);
    }

    public static OpenTelemetryTracer fromOpenTelemetry(OpenTelemetry openTelemetry, String scopeName) {
        Objects.requireNonNull(openTelemetry, "openTelemetry is null");
        Objects.requireNonNull(scopeName, "scopeName is null");
        return new OpenTelemetryTracer(openTelemetry.getTracer(scopeName));
    }

    @Override
    public Span startSpan(String spanName, SpanKind spanKind) {
        io.opentelemetry.api.trace.Span span = tracer.spanBuilder(spanName)
                .setSpanKind(mapSpanKind(spanKind))
                .startSpan();
        return new OtelSpan(span);
    }

    private static io.opentelemetry.api.trace.SpanKind mapSpanKind(SpanKind kind) {
        if (kind == SpanKind.CLIENT) {
            return io.opentelemetry.api.trace.SpanKind.CLIENT;
        }
        return io.opentelemetry.api.trace.SpanKind.INTERNAL;
    }

    private static final class OtelSpan implements Span {
        private final io.opentelemetry.api.trace.Span span;

        private OtelSpan(io.opentelemetry.api.trace.Span span) {
            this.span = span;
        }

        @Override
        public String getId() {
            return span.getSpanContext().getSpanId();
        }

        @Override
        public void setAttribute(String key, String value) {
            if (key != null && value != null) {
                span.setAttribute(AttributeKey.stringKey(key), value);
            }
        }

        @Override
        public void setAttribute(String key, long value) {
            if (key != null) {
                span.setAttribute(AttributeKey.longKey(key), value);
            }
        }

        @Override
        public Span recordException(Throwable error) {
            if (error != null) {
                span.recordException(error);
            }
            return this;
        }

        @Override
        public void end() {
            span.end();
        }
    }
}
