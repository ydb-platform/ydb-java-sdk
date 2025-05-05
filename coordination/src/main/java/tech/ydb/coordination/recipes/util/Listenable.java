package tech.ydb.coordination.recipes.util;

import java.util.concurrent.ExecutorService;
import java.util.function.Consumer;

public interface Listenable<T> {
    void addListener(Consumer<T> listener);

    /**
     * Listener call will be processed in executor
     * @param listener
     * @param executor
     */
    void addListener(Consumer<T> listener, ExecutorService executor);

    void removeListener(Consumer<T> listener);
}
