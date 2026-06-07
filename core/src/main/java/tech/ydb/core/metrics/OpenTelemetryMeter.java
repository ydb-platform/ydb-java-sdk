package tech.ydb.core.metrics;

import java.util.Objects;
import java.util.function.Consumer;

import io.grpc.ExperimentalApi;
import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.common.AttributesBuilder;
import io.opentelemetry.api.metrics.DoubleHistogramBuilder;
import io.opentelemetry.api.metrics.LongCounterBuilder;
import io.opentelemetry.api.metrics.LongGaugeBuilder;

/**
 * OpenTelemetry-backed implementation of {@link Meter}.
 */
@ExperimentalApi("YDB Meter is experimental and API may change without notice")
public final class OpenTelemetryMeter implements Meter {
    private static final String DEFAULT_SCOPE = "tech.ydb.sdk";

    private final io.opentelemetry.api.metrics.Meter meter;

    private OpenTelemetryMeter(io.opentelemetry.api.metrics.Meter meter) {
        this.meter = Objects.requireNonNull(meter, "meter is null");
    }

    public static OpenTelemetryMeter fromOpenTelemetry(OpenTelemetry openTelemetry) {
        Objects.requireNonNull(openTelemetry, "openTelemetry is null");
        return new OpenTelemetryMeter(openTelemetry.getMeter(DEFAULT_SCOPE));
    }

    public static OpenTelemetryMeter createGlobal() {
        return fromOpenTelemetry(GlobalOpenTelemetry.get());
    }

    @Override
    public LongCounter createCounter(String name, String unit, String description) {
        LongCounterBuilder builder = meter.counterBuilder(name);
        if (unit != null) {
            builder.setUnit(unit);
        }
        if (description != null) {
            builder.setDescription(description);
        }
        io.opentelemetry.api.metrics.LongCounter counter = builder.build();
        return (value, attrs) -> counter.add(value, attributesOf(attrs));
    }

    @Override
    public DoubleHistogram createHistogram(String name, String unit, String description) {
        DoubleHistogramBuilder builder = meter.histogramBuilder(name);
        if (unit != null) {
            builder.setUnit(unit);
        }
        if (description != null) {
            builder.setDescription(description);
        }
        io.opentelemetry.api.metrics.DoubleHistogram histogram = builder.build();
        return (value, attrs) -> histogram.record(value, attributesOf(attrs));
    }

    @Override
    public void createLongGauge(String name, String unit, String description, Consumer<LongMeasurement> callback) {
        LongGaugeBuilder builder = meter.gaugeBuilder(name).ofLongs();
        if (unit != null) {
            builder.setUnit(unit);
        }
        if (description != null) {
            builder.setDescription(description);
        }
        builder.buildWithCallback(otelMeasurement ->
                callback.accept((value, attrs) -> otelMeasurement.record(value, attributesOf(attrs))));
    }

    private static Attributes attributesOf(Attr[] attrs) {
        if (attrs == null || attrs.length == 0) {
            return Attributes.empty();
        }
        AttributesBuilder builder = Attributes.builder();
        for (Attr attr : attrs) {
            builder.put(attr.getKey(), attr.getValue());
        }
        return builder.build();
    }
}
