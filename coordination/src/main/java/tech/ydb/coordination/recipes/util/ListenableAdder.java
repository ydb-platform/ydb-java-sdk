package tech.ydb.coordination.recipes.util;

import java.util.function.Consumer;

public interface ListenableAdder<T> {
    void addListener(Consumer<T> listener);
    void removeListener(Consumer<T> listener);
}
