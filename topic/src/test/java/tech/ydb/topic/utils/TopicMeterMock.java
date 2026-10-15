package tech.ydb.topic.utils;

import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

import org.junit.Assert;

import tech.ydb.core.metrics.Attr;
import tech.ydb.core.metrics.LongCounter;
import tech.ydb.core.metrics.LongMeasurement;
import tech.ydb.core.metrics.Meter;
import tech.ydb.core.metrics.MetricRegistration;

public class TopicMeterMock implements Meter {
    private final Map<String, AtomicLong> counters = new ConcurrentHashMap<>();
    private final Map<String, Attr[]> attributes = new ConcurrentHashMap<>();
    private final Map<String, Consumer<LongMeasurement>> gauges = new ConcurrentHashMap<>();

    @Override
    public LongCounter createCounter(String name, String unit, String description) {
        AtomicLong counter = new AtomicLong();
        Assert.assertNull("Counter registered twice: " + name, counters.putIfAbsent(name, counter));
        return (value, attrs) -> {
            attributes.put(name, attrs);
            counter.addAndGet(value);
        };
    }

    @Override
    public MetricRegistration registerLongGauge(
            String name, String unit, String description, Consumer<LongMeasurement> callback) {
        Assert.assertNull("Gauge registered twice: " + name, gauges.putIfAbsent(name, callback));
        return () -> gauges.remove(name, callback);
    }

    public long value(String name) {
        AtomicLong counter = counters.get(name);
        Assert.assertNotNull("Counter not registered: " + name, counter);
        return counter.get();
    }

    public long collect(String name) {
        Consumer<LongMeasurement> callback = gauges.get(name);
        Assert.assertNotNull("Gauge not registered: " + name, callback);
        Long[] value = new Long[1];
        callback.accept((observed, attrs) -> {
            attributes.put(name, attrs);
            value[0] = observed;
        });
        Assert.assertNotNull("Gauge did not record: " + name, value[0]);
        return value[0];
    }

    public void assertAttribute(String name, String key, String value) {
        Attr[] attrs = attributes.get(name);
        Assert.assertNotNull("Metric has no attributes: " + name, attrs);
        Assert.assertTrue(name + ": missing " + key + "=" + value,
                Arrays.stream(attrs).anyMatch(attr -> attr.getKey().equals(key) && attr.getValue().equals(value)));
    }

    public void assertNoGauges() {
        Assert.assertTrue("Gauges still registered: " + gauges.keySet(), gauges.isEmpty());
    }
}
