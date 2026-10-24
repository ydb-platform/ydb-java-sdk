package tech.ydb.topic.utils;

import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

import tech.ydb.core.metrics.Attr;
import tech.ydb.core.metrics.DoubleHistogram;
import tech.ydb.core.metrics.LongCounter;
import tech.ydb.core.metrics.MetricRegistration;

import static tech.ydb.topic.utils.InMemoryMeter.attributes;

public class InMemoryMeterTest {
    @Test
    public void assertionsBindInstrumentAndAttributes() {
        InMemoryMeter meter = new InMemoryMeter();
        Map<String, String> first = attributes("topic", "/first");
        Map<String, String> second = attributes("topic", "/second");
        Attr a = Attr.of("topic", "/first");
        Attr b = Attr.of("topic", "/second");
        LongCounter counter = meter.createCounter("counter", "{message}", "");
        counter.add(2, a);
        counter.add(3, b);
        meter.assertCounter("counter", 2, first);
        meter.assertCounter("counter", 3, second);
        meter.assertCounterSeries("counter", 2);
        Assert.assertThrows(AssertionError.class, () -> meter.assertCounter("counter", 3, first));
        Assert.assertThrows(AssertionError.class, () -> meter.assertCounter("missing", 2, first));
        Assert.assertThrows(AssertionError.class, () -> meter.assertNoCounterSamples("counter"));
        DoubleHistogram histogram = meter.createHistogram("histogram", "s", "");
        histogram.record(0.25, a);
        histogram.record(0.5, a);
        histogram.record(0.75, b);
        meter.assertHistogram("histogram", "s", 2, first);
        meter.assertHistogram("histogram", "s", 1, second);
        Assert.assertThrows(AssertionError.class, () -> meter.assertHistogram("histogram", "s", 1, first));
        Assert.assertThrows(AssertionError.class, () -> meter.assertNoHistogramSamples("histogram"));
        MetricRegistration one = meter.registerLongGauge("gauge", "By", "", m -> m.record(2, a));
        MetricRegistration two = meter.registerLongGauge("gauge", "By", "", m -> m.record(3, b));
        MetricRegistration doubles = meter.registerDoubleGauge("double", "s", "", m -> {
            m.record(2.5, a);
            m.record(3.5, b);
        });
        try {
            meter.assertLongGauge("gauge", 2, first);
            meter.assertLongGauge("gauge", 3, second);
            Assert.assertThrows(AssertionError.class, () -> meter.assertLongGauge("gauge", 3, first));
            meter.assertDoubleGauge("double", 2.5, first);
            meter.assertDoubleGauge("double", 3.5, second);
            Assert.assertThrows(AssertionError.class, () -> meter.assertDoubleGauge("double", 3.5, first));
            one.close();
            meter.assertLongGauge("gauge", 3, second);
            Assert.assertThrows(AssertionError.class, () -> meter.assertLongGauge("gauge", 2, first));
        } finally {
            one.close();
            two.close();
            doubles.close();
        }
        meter.assertNoGauges();
    }
}
