package tech.ydb.core.tracing;

import java.util.Objects;

import javax.annotation.Nullable;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.context.Context;

import tech.ydb.core.Status;
import tech.ydb.core.UnexpectedResultException;

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
        Context parentContext = Context.current();
        io.opentelemetry.api.trace.Span span = tracer.spanBuilder(spanName)
                .setParent(parentContext)
                .setSpanKind(mapSpanKind(spanKind))
                .startSpan();
        return new OtelSpan(span, parentContext);
    }

    @Override
    public Span currentSpan() {
        io.opentelemetry.api.trace.Span current = io.opentelemetry.api.trace.Span.current();
        if (!current.getSpanContext().isValid()) {
            return Span.NOOP;
        }
        return new OtelSpan(current, Context.current());
    }

    private static io.opentelemetry.api.trace.SpanKind mapSpanKind(SpanKind kind) {
        if (kind == SpanKind.CLIENT) {
            return io.opentelemetry.api.trace.SpanKind.CLIENT;
        }
        return io.opentelemetry.api.trace.SpanKind.INTERNAL;
    }

    private static final class OtelSpan implements Span {
        private final io.opentelemetry.api.trace.Span span;
        private final Context capturedContext;

        private OtelSpan(io.opentelemetry.api.trace.Span span, Context capturedContext) {
            this.span = span;
            this.capturedContext = capturedContext;
        }

        @Override
        public String getId() {
            return "00-" + span.getSpanContext().getTraceId() + "-" + span.getSpanContext().getSpanId() + "-01";
        }

        @Override
        public boolean isValid() {
            return span.getSpanContext().isValid();
        }

        @Override
        public void setAttribute(String key, @Nullable String value) {
            span.setAttribute(key, value);
        }

        @Override
        public void setAttribute(String key, long value) {
            span.setAttribute(key, value);
        }

        @Override
        public void setStatus(@Nullable Status status, @Nullable Throwable error) {
            if (status != null) {
                if (status.isSuccess()) {
                    span.setStatus(StatusCode.OK);
                } else {
                    tech.ydb.core.StatusCode code = status.getCode();
                    span.setAttribute("db.response.status_code", code.toString());
                    span.setAttribute("error.type", code.isTransportError() ? "transport_error" : "ydb_error");
                    span.setStatus(StatusCode.ERROR, status.toString());
                }
            }
            if (error != null) {
                if (error instanceof UnexpectedResultException) {
                    tech.ydb.core.StatusCode code = ((UnexpectedResultException) error).getStatus().getCode();
                    span.setAttribute("db.response.status_code", code.toString());
                    span.setAttribute("error.type", code.isTransportError() ? "transport_error" : "ydb_error");
                } else {
                    span.setAttribute("error.type", error.getClass().getName());
                }
                span.setStatus(StatusCode.ERROR, error.getMessage());
            }
        }

        @Override
        public Scope makeCurrent() {
            io.opentelemetry.context.Scope scope = span.makeCurrent();
            return scope::close;
        }

        @Override
        public Scope restoreContext() {
            io.opentelemetry.context.Scope scope = capturedContext.makeCurrent();
            return scope::close;
        }

        @Override
        public void end() {
            span.end();
        }
    }
}
