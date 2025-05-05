package tech.ydb.coordination.recipes.util;

import java.util.concurrent.ExecutorService;
import java.util.function.Consumer;

/**
 * Generic interface for objects that allow adding and removing listeners for events of type T.
 *
 * @param <T> the type of event data that listeners will receive
 */
public interface Listenable<T> {
    /**
     * Adds a listener that will be notified synchronously when the event occurs.
     *
     * @param listener the listener to add, must not be null
     * @throws NullPointerException if listener is null
     */
    void addListener(Consumer<T> listener);

    /**
     * Adds a listener that will be notified asynchronously using the provided executor.
     *
     * @param listener the listener to add, must not be null
     * @param executor the executor to use for asynchronous notification, must not be null
     * @throws NullPointerException if listener or executor is null
     */
    void addListener(Consumer<T> listener, ExecutorService executor);

    /**
     * Removes the specified listener.
     *
     * @param listener the listener to remove
     */
    void removeListener(Consumer<T> listener);
}
