package tech.ydb.core.metrics;

import java.io.IOException;
import java.util.Collection;
import java.util.concurrent.atomic.AtomicLong;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.metrics.SdkMeterProvider;
import io.opentelemetry.sdk.metrics.data.DoublePointData;
import io.opentelemetry.sdk.metrics.data.HistogramPointData;
import io.opentelemetry.sdk.metrics.data.LongPointData;
import io.opentelemetry.sdk.metrics.data.MetricData;
import io.opentelemetry.sdk.testing.exporter.InMemoryMetricReader;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class OpenTelemetryMeterTest {
    private static final AttributeKey<String> POOL = AttributeKey.stringKey("pool.name");
    private static final AttributeKey<String> STATE = AttributeKey.stringKey("state");

    private InMemoryMetricReader reader;
    private SdkMeterProvider provider;
    private OpenTelemetryMeter meter;

    @Before
    public void setup() {
        reader = InMemoryMetricReader.create();
        provider = SdkMeterProvider.builder().registerMetricReader(reader).build();
        OpenTelemetry openTelemetry = OpenTelemetrySdk.builder().setMeterProvider(provider).build();
        meter = OpenTelemetryMeter.fromOpenTelemetry(openTelemetry);
    }

    @After
    public void tearDown() throws IOException {
        provider.close();
        reader.close();
    }

    @Test
    public void counterReportsValueAndAttributes() {
        LongCounter counter = meter.createCounter("ydb.test.counter", "{session}", "test counter");
        counter.add(3L, Attr.of("pool.name", "my-pool"));
        counter.add(2L, Attr.of("pool.name", "my-pool"));

        MetricData metric = single("ydb.test.counter");
        Assert.assertEquals("{session}", metric.getUnit());
        Assert.assertEquals("test counter", metric.getDescription());

        LongPointData point = singleLongPoint(metric.getLongSumData().getPoints());
        Assert.assertEquals(5L, point.getValue());
        Assert.assertEquals("my-pool", point.getAttributes().get(POOL));
    }

    @Test
    public void histogramReportsValueAndAttributes() {
        DoubleHistogram histogram = meter.createHistogram("ydb.test.histogram", "s", "test histogram");
        histogram.record(0.5d, Attr.of("pool.name", "my-pool"));

        MetricData metric = single("ydb.test.histogram");
        Assert.assertEquals("s", metric.getUnit());
        Assert.assertEquals("test histogram", metric.getDescription());

        Collection<HistogramPointData> points = metric.getHistogramData().getPoints();
        Assert.assertEquals(1, points.size());
        HistogramPointData point = points.iterator().next();
        Assert.assertEquals(1L, point.getCount());
        Assert.assertEquals(0.5d, point.getSum(), 0.0001d);
        Assert.assertEquals("my-pool", point.getAttributes().get(POOL));
    }

    @Test
    public void gaugeInvokesCallbackOnCollect() {
        AtomicLong value = new AtomicLong(7L);
        MetricRegistration registration = meter.registerLongGauge("ydb.test.gauge", "{session}", "test gauge",
                m -> m.record(value.get(), Attr.of("pool.name", "my-pool"), Attr.of("state", "idle")));

        MetricData metric = single("ydb.test.gauge");
        Assert.assertEquals("{session}", metric.getUnit());
        Assert.assertEquals("test gauge", metric.getDescription());

        LongPointData point = singleLongPoint(metric.getLongGaugeData().getPoints());
        Assert.assertEquals(7L, point.getValue());
        Assert.assertEquals("my-pool", point.getAttributes().get(POOL));
        Assert.assertEquals("idle", point.getAttributes().get(STATE));

        value.set(11L);
        LongPointData updated = singleLongPoint(single("ydb.test.gauge").getLongGaugeData().getPoints());
        Assert.assertEquals(11L, updated.getValue());
        registration.close();
        registration.close();
        Assert.assertTrue(reader.collectAllMetrics().isEmpty());
    }

    @Test
    public void doubleGaugeReportsFractionalValuesAndCloses() {
        double[] value = {0.125};
        MetricRegistration registration = meter.registerDoubleGauge("ydb.test.age", "s", "test age",
                m -> m.record(value[0], Attr.of("pool.name", "my-pool")));
        MetricData metric = single("ydb.test.age");
        Assert.assertEquals("s", metric.getUnit());
        Assert.assertEquals("test age", metric.getDescription());
        Assert.assertEquals(1, metric.getDoubleGaugeData().getPoints().size());
        DoublePointData point = metric.getDoubleGaugeData().getPoints().iterator().next();
        Assert.assertEquals(0.125, point.getValue(), 0);
        Assert.assertEquals("my-pool", point.getAttributes().get(POOL));
        value[0] = 1.5;
        Assert.assertEquals(1.5, single("ydb.test.age").getDoubleGaugeData().getPoints()
                .iterator().next().getValue(), 0);
        registration.close();
        registration.close();
        Assert.assertTrue(reader.collectAllMetrics().isEmpty());
        Meter.NOOP.registerDoubleGauge("noop", "s", null, m -> Assert.fail("NOOP collected")).close();
    }

    @Test
    public void emptyAttributesAreSupported() {
        LongCounter counter = meter.createCounter("ydb.test.noattrs", null, null);
        counter.add(1L);

        LongPointData point = singleLongPoint(single("ydb.test.noattrs").getLongSumData().getPoints());
        Assert.assertEquals(1L, point.getValue());
        Assert.assertEquals(Attributes.empty(), point.getAttributes());

        MetricRegistration registration = meter.registerDoubleGauge("ydb.test.noattrs.double", null, null,
                m -> m.record(1.5));
        DoublePointData doublePoint = single("ydb.test.noattrs.double").getDoubleGaugeData()
                .getPoints().iterator().next();
        Assert.assertEquals(1.5, doublePoint.getValue(), 0);
        Assert.assertEquals(Attributes.empty(), doublePoint.getAttributes());
        registration.close();
    }

    private MetricData single(String name) {
        MetricData found = null;
        for (MetricData metric : reader.collectAllMetrics()) {
            if (name.equals(metric.getName())) {
                found = metric;
            }
        }
        Assert.assertNotNull(name + " metric not found", found);
        return found;
    }

    private static LongPointData singleLongPoint(Collection<LongPointData> points) {
        Assert.assertEquals(1, points.size());
        return points.iterator().next();
    }
}
