package tech.ydb.opentelemetry;

import java.util.Objects;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.context.Context;

import tech.ydb.core.tracing.Scope;
import tech.ydb.core.tracing.Span;
import tech.ydb.core.tracing.SpanBuilder;
import tech.ydb.core.tracing.SpanStatusCode;
import tech.ydb.core.tracing.TraceHeaderSetter;
import tech.ydb.core.tracing.Tracer;

/**
 * Adapter from OpenTelemetry API to YDB SDK tracing SPI.
 */
public final class OpenTelemetryTracer implements Tracer {
    private static final String DEFAULT_INSTRUMENTATION_SCOPE = "tech.ydb.sdk";

    private final io.opentelemetry.api.trace.Tracer tracer;

    private OpenTelemetryTracer(io.opentelemetry.api.trace.Tracer tracer) {
        this.tracer = Objects.requireNonNull(tracer, "tracer is null");
    }

    public static OpenTelemetryTracer createGlobal() {
        return fromOpenTelemetry(GlobalOpenTelemetry.get());
    }

    public static OpenTelemetryTracer fromOpenTelemetry(OpenTelemetry openTelemetry) {
        return fromOpenTelemetry(openTelemetry, DEFAULT_INSTRUMENTATION_SCOPE);
    }

    public static OpenTelemetryTracer fromOpenTelemetry(OpenTelemetry openTelemetry, String instrumentationScope) {
        Objects.requireNonNull(openTelemetry, "openTelemetry is null");
        Objects.requireNonNull(instrumentationScope, "instrumentationScope is null");
        return new OpenTelemetryTracer(openTelemetry.getTracer(instrumentationScope));
    }

    public static OpenTelemetryTracer fromTracer(io.opentelemetry.api.trace.Tracer tracer) {
        return new OpenTelemetryTracer(tracer);
    }

    @Override
    public SpanBuilder spanBuilder(String spanName) {
        return new OpenTelemetrySpanBuilder(tracer.spanBuilder(spanName)
                .setSpanKind(io.opentelemetry.api.trace.SpanKind.CLIENT)
        );
    }

    private static final class OpenTelemetrySpanBuilder implements SpanBuilder {
        private final io.opentelemetry.api.trace.SpanBuilder delegate;

        OpenTelemetrySpanBuilder(io.opentelemetry.api.trace.SpanBuilder delegate) {
            this.delegate = delegate;
        }

        @Override
        public SpanBuilder setAttribute(String key, String value) {
            delegate.setAttribute(AttributeKey.stringKey(key), value);
            return this;
        }

        @Override
        public SpanBuilder setAttribute(String key, long value) {
            if (key != null) {
                delegate.setAttribute(AttributeKey.longKey(key), value);
            }
            return this;
        }

        @Override
        public Span startSpan() {
            return new OpenTelemetrySpan(delegate.startSpan());
        }
    }

    private static final class OpenTelemetrySpan implements Span {
        private final io.opentelemetry.api.trace.Span delegate;

        OpenTelemetrySpan(io.opentelemetry.api.trace.Span delegate) {
            this.delegate = delegate;
        }

        @Override
        public Scope makeCurrent() {
            io.opentelemetry.context.Scope scope = delegate.makeCurrent();
            return scope::close;
        }

        @Override
        public Span setAttribute(String key, String value) {
            if (key != null && value != null) {
                delegate.setAttribute(AttributeKey.stringKey(key), value);
            }
            return this;
        }

        @Override
        public Span setAttribute(String key, long value) {
            if (key != null) {
                delegate.setAttribute(AttributeKey.longKey(key), value);
            }
            return this;
        }

        @Override
        public void injectHeaders(TraceHeaderSetter headerSetter) {
            if (headerSetter == null) {
                return;
            }
            Context spanContext = Context.current().with(delegate);
            GlobalOpenTelemetry.getPropagators()
                    .getTextMapPropagator()
                    .inject(spanContext, headerSetter, (carrier, key, value) -> carrier.set(key, value));
        }

        @Override
        public Span recordException(Throwable error) {
            if (error != null) {
                delegate.recordException(error);
            }
            return this;
        }

        @Override
        public void end() {
            delegate.end();
        }
    }
}
