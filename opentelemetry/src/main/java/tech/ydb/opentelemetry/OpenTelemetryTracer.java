package tech.ydb.opentelemetry;

import java.util.Objects;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.StatusCode;

import tech.ydb.core.Status;
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
            return "00-" + span.getSpanContext().getTraceId() + "-" + span.getSpanContext().getSpanId() + "01";
        }

        @Override
        public void setAttribute(String key, String value) {
            span.setAttribute(key, value);
        }

        @Override
        public void setAttribute(String key, long value) {
            span.setAttribute(key, value);
        }

        @Override
        public void setError(Status status) {
            span.setAttribute("db.response.status_code", status.getCode().toString());
            span.setAttribute("error.type", status.getCode().isTransportError() ? "transport_error" : "ydb_error");

            span.setStatus(StatusCode.ERROR, status.toString());
        }

        @Override
        public void setError(Throwable error) {
            span.setAttribute("error.type", error.getClass().getName());
            span.setStatus(StatusCode.ERROR, error.getMessage());
        }

        @Override
        public void end() {
            span.end();
        }
    }
}
