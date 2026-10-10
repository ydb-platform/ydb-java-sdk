package tech.ydb.core.tracing;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.SpanId;
import io.opentelemetry.api.trace.TraceId;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.samplers.Sampler;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class OpenTelemetryTracerTest {
    private static Span startSpan(Sampler sampler) {
        SdkTracerProvider provider = SdkTracerProvider.builder().setSampler(sampler).build();
        OpenTelemetry openTelemetry = OpenTelemetrySdk.builder().setTracerProvider(provider).build();
        return OpenTelemetryTracer.fromOpenTelemetry(openTelemetry).startSpan("test", SpanKind.CLIENT);
    }

    private static void assertTraceparent(String traceparent, String expectedFlags) {
        String[] parts = traceparent.split("-");
        assertEquals(traceparent, 4, parts.length);
        assertEquals("00", parts[0]);
        assertTrue(traceparent, TraceId.isValid(parts[1]));
        assertTrue(traceparent, SpanId.isValid(parts[2]));
        assertEquals(traceparent, expectedFlags, parts[3]);
    }

    @Test
    public void sampledSpanIdTest() {
        Span span = startSpan(Sampler.alwaysOn());
        assertTrue(span.isValid());
        assertTraceparent(span.getId(), "01");
        span.end();
    }

    @Test
    public void notSampledSpanIdTest() {
        // the span context of a dropped span is still valid and is propagated to the server
        Span span = startSpan(Sampler.alwaysOff());
        assertTrue(span.isValid());
        assertTraceparent(span.getId(), "00");
        span.end();
    }
}
