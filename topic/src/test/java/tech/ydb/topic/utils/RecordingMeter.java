package tech.ydb.topic.utils;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.function.DoubleSupplier;
import java.util.function.LongSupplier;
import java.util.stream.Collectors;

import org.junit.Assert;

import tech.ydb.core.metrics.Attr;
import tech.ydb.core.metrics.DoubleHistogram;
import tech.ydb.core.metrics.DoubleMeasurement;
import tech.ydb.core.metrics.LongCounter;
import tech.ydb.core.metrics.LongMeasurement;
import tech.ydb.core.metrics.Meter;
import tech.ydb.core.metrics.MetricRegistration;

public final class RecordingMeter implements Meter {
    public final Map<String, List<Double>> histograms = new ConcurrentHashMap<>();
    public final Map<String, LongSupplier> gauges = new ConcurrentHashMap<>();
    public final Map<String, DoubleSupplier> doubleGauges = new ConcurrentHashMap<>();
    public final Map<String, String> units = new ConcurrentHashMap<>();
    private final Map<String, AtomicLong> counters = new ConcurrentHashMap<>();
    private final Map<String, Attr[]> attributes = new ConcurrentHashMap<>();

    @Override
    public LongCounter createCounter(String name, String unit, String description) {
        units.put(name, unit);
        AtomicLong counter = counters.computeIfAbsent(name, key -> new AtomicLong());
        return (value, attrs) -> {
            counter.addAndGet(value);
            attributes.put(name, attrs);
        };
    }

    @Override
    public DoubleHistogram createHistogram(String name, String unit, String description) {
        units.put(name, unit);
        List<Double> samples = histograms.computeIfAbsent(name, key -> new CopyOnWriteArrayList<>());
        return (value, attrs) -> {
            samples.add(value);
            attributes.put(name, attrs);
        };
    }

    @Override
    public MetricRegistration registerLongGauge(
            String name, String unit, String description, Consumer<LongMeasurement> callback) {
        units.put(name, unit);
        Assert.assertNull(gauges.put(name, () -> {
            Long[] value = new Long[1];
            callback.accept((observed, attrs) -> {
                value[0] = observed;
                attributes.put(name, attrs);
            });
            return value[0];
        }));
        return () -> gauges.remove(name);
    }

    @Override
    public MetricRegistration registerDoubleGauge(
            String name, String unit, String description, Consumer<DoubleMeasurement> callback) {
        units.put(name, unit);
        Assert.assertNull(doubleGauges.put(name, () -> {
            Double[] value = new Double[1];
            callback.accept((observed, attrs) -> {
                value[0] = observed;
                attributes.put(name, attrs);
            });
            return value[0];
        }));
        return () -> doubleGauges.remove(name);
    }

    public long value(String name) {
        AtomicLong value = counters.get(name);
        return value == null ? 0 : value.get();
    }

    public long collect(String name) {
        return gauges.get(name).getAsLong();
    }

    public double collectDouble(String name) {
        return doubleGauges.get(name).getAsDouble();
    }

    public void assertAttributes(String name, String... pairs) {
        Map<String, String> expected = new HashMap<>();
        for (int i = 0; i < pairs.length; i += 2) {
            expected.put(pairs[i], pairs[i + 1]);
        }
        Assert.assertEquals(expected, Arrays.stream(attributes.get(name))
                .collect(Collectors.toMap(Attr::getKey, Attr::getValue)));
    }
}
