package tech.ydb.core.utils;

import java.util.Optional;

/**
 *
 * @author Aleksandr Gorshenin
 * @param <R> type of value
 */
public class UpdatableOptional<R> {
    private volatile R value = null;

    public void update(R value) {
        this.value = value;
    }

    public R orElse(R other) {
        return Optional.ofNullable(value).orElse(other);
    }

    public R get() {
        return value;
    }
}
