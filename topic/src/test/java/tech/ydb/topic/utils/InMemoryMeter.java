package tech.ydb.topic.utils;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

import org.junit.Assert;

import tech.ydb.core.metrics.Attr;
import tech.ydb.core.metrics.DoubleHistogram;
import tech.ydb.core.metrics.DoubleMeasurement;
import tech.ydb.core.metrics.LongCounter;
import tech.ydb.core.metrics.LongMeasurement;
import tech.ydb.core.metrics.Meter;
import tech.ydb.core.metrics.MetricRegistration;

public final class InMemoryMeter implements Meter {
    private final Map<String, Map<Map<String, String>, AtomicLong>> counters = new ConcurrentHashMap<>();
    private final Map<String, Map<Map<String, String>, List<Double>>> histograms = new ConcurrentHashMap<>();
    private final Map<String, List<Consumer<LongMeasurement>>> longGauges = new ConcurrentHashMap<>();
    private final Map<String, List<Consumer<DoubleMeasurement>>> doubleGauges = new ConcurrentHashMap<>();
    private final Map<String, String> units = new ConcurrentHashMap<>();

    @Override
    public LongCounter createCounter(String name, String unit, String description) {
        units.put(name, unit);
        Map<Map<String, String>, AtomicLong> values = counters.computeIfAbsent(name, key -> new ConcurrentHashMap<>());
        return (value, attrs) -> values.computeIfAbsent(key(attrs), key -> new AtomicLong()).addAndGet(value);
    }

    @Override
    public DoubleHistogram createHistogram(String name, String unit, String description) {
        units.put(name, unit);
        Map<Map<String, String>, List<Double>> values = histograms.computeIfAbsent(name,
                key -> new ConcurrentHashMap<>());
        return (value, attrs) -> values.computeIfAbsent(key(attrs), key -> new CopyOnWriteArrayList<>()).add(value);
    }

    @Override
    public MetricRegistration registerLongGauge(
            String name, String unit, String description, Consumer<LongMeasurement> callback) {
        units.put(name, unit);
        List<Consumer<LongMeasurement>> callbacks = longGauges.computeIfAbsent(name,
                key -> new CopyOnWriteArrayList<>());
        callbacks.add(callback);
        return () -> callbacks.remove(callback);
    }

    @Override
    public MetricRegistration registerDoubleGauge(
            String name, String unit, String description, Consumer<DoubleMeasurement> callback) {
        units.put(name, unit);
        List<Consumer<DoubleMeasurement>> callbacks = doubleGauges.computeIfAbsent(name,
                key -> new CopyOnWriteArrayList<>());
        callbacks.add(callback);
        return () -> callbacks.remove(callback);
    }

    public void assertCounter(String name, long expected, Map<String, String> attributes) {
        Map<Map<String, String>, AtomicLong> values = counters.get(name);
        Assert.assertNotNull(name, values);
        AtomicLong value = values.get(attributes);
        Assert.assertEquals(name + " " + attributes, expected, value == null ? 0 : value.get());
    }

    public void assertCounterSeries(String name, int expected) {
        Assert.assertNotNull(name, counters.get(name));
        Assert.assertEquals(name, expected, counters.get(name).size());
    }

    public void assertNoCounterSamples(String name) {
        Assert.assertNotNull(name, counters.get(name));
        Assert.assertTrue(name, counters.get(name).isEmpty());
    }

    public void assertHistogram(String name, String unit, int expected, Map<String, String> attributes) {
        assertUnit(name, unit);
        Map<Map<String, String>, List<Double>> values = histograms.get(name);
        Assert.assertNotNull(name, values);
        List<Double> samples = values.get(attributes);
        Assert.assertEquals(name + " " + attributes, expected, samples == null ? 0 : samples.size());
    }

    public void assertNoHistogramSamples(String name) {
        Assert.assertNotNull(name, histograms.get(name));
        Assert.assertTrue(name, histograms.get(name).isEmpty());
    }

    public void assertLongGauge(String name, long expected, Map<String, String> attributes) {
        List<Consumer<LongMeasurement>> callbacks = longGauges.get(name);
        Assert.assertNotNull(name, callbacks);
        Map<Map<String, String>, Long> values = new HashMap<>();
        for (Consumer<LongMeasurement> callback : callbacks) {
            callback.accept((value, attrs) -> Assert.assertNull(name + " duplicate series",
                    values.put(key(attrs), value)));
        }
        Assert.assertTrue(name + " " + attributes, values.containsKey(attributes));
        Assert.assertEquals(name + " " + attributes, expected, values.get(attributes).longValue());
    }

    public void assertDoubleGauge(String name, double expected, Map<String, String> attributes) {
        Assert.assertEquals(name + " " + attributes, expected, doubleGaugeValue(name, attributes), 0);
    }

    public void assertPositiveDoubleGauge(String name, Map<String, String> attributes) {
        Assert.assertTrue(name + " " + attributes, doubleGaugeValue(name, attributes) > 0);
    }

    public void assertUnit(String name, String expected) {
        Assert.assertEquals(name, expected, units.get(name));
    }

    public void assertNoGauges() {
        Assert.assertTrue(longGauges.toString(), longGauges.values().stream().allMatch(List::isEmpty));
        Assert.assertTrue(doubleGauges.toString(), doubleGauges.values().stream().allMatch(List::isEmpty));
    }

    private double doubleGaugeValue(String name, Map<String, String> attributes) {
        List<Consumer<DoubleMeasurement>> callbacks = doubleGauges.get(name);
        Assert.assertNotNull(name, callbacks);
        Map<Map<String, String>, Double> values = new HashMap<>();
        for (Consumer<DoubleMeasurement> callback : callbacks) {
            callback.accept((value, attrs) -> Assert.assertNull(name + " duplicate series",
                    values.put(key(attrs), value)));
        }
        Assert.assertTrue(name + " " + attributes, values.containsKey(attributes));
        return values.get(attributes);
    }

    public static Map<String, String> attributes(String... pairs) {
        Map<String, String> result = new HashMap<>();
        for (int i = 0; i < pairs.length; i += 2) {
            result.put(pairs[i], pairs[i + 1]);
        }
        return Collections.unmodifiableMap(result);
    }

    private static Map<String, String> key(Attr[] attrs) {
        Map<String, String> result = new HashMap<>();
        for (Attr attr : attrs) {
            result.put(attr.getKey(), attr.getValue());
        }
        return Collections.unmodifiableMap(result);
    }
}
