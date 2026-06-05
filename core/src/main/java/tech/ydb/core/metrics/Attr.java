package tech.ydb.core.metrics;

import java.util.Objects;

/**
 * Single immutable attribute (key + value) attached to a metric measurement.
 */
public final class Attr {
    private final String key;
    private final String value;

    private Attr(String key, String value) {
        this.key = Objects.requireNonNull(key, "key is null");
        this.value = Objects.requireNonNull(value, "value is null");
    }

    public static Attr of(String key, String value) {
        return new Attr(key, value);
    }

    public String getKey() {
        return key;
    }

    public String getValue() {
        return value;
    }
}
