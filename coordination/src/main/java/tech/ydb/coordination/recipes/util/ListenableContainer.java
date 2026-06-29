package tech.ydb.coordination.recipes.util;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Thread-safe container for managing and notifying listeners.
 *
 * @param <T> the type of event data that listeners will receive
 */
public class ListenableContainer<T> implements Listenable<T> {
    private static final Logger logger = LoggerFactory.getLogger(ListenableContainer.class);

    // Maps original listeners to potentially wrapped listeners
    private final Map<Consumer<T>, Consumer<T>> listenersMapping = new ConcurrentHashMap<>();

    /**
     * Notifies all registered listeners with the provided data.
     * Exceptions thrown by listeners are caught and logged.
     *
     * @param data the data to send to listeners
     * @throws NullPointerException if data is null
     */
    public void notifyListeners(T data) {
        Objects.requireNonNull(data, "Data cannot be null");

        listenersMapping.values().forEach(listener -> {
            try {
                listener.accept(data);
            } catch (Exception ex) {
                logger.error("Listener threw exception during notification", ex);
            }
        });
    }

    @Override
    public void addListener(Consumer<T> listener) {
        Objects.requireNonNull(listener, "Listener cannot be null");

        if (listenersMapping.containsKey(listener)) {
            logger.debug("Listener already registered, skipping");
            return;
        }

        listenersMapping.put(listener, listener);
    }

    @Override
    public void addListener(Consumer<T> listener, ExecutorService executor) {
        Objects.requireNonNull(listener, "Listener cannot be null");
        Objects.requireNonNull(executor, "Executor cannot be null");

        if (listenersMapping.containsKey(listener)) {
            logger.debug("Listener already registered, skipping");
            return;
        }

        Consumer<T> wrapper = data -> {
            try {
                executor.submit(() -> {
                    try {
                        listener.accept(data);
                    } catch (Exception ex) {
                        logger.error("Asynchronous listener threw exception", ex);
                    }
                });
            } catch (Exception ex) {
                logger.error("Failed to submit listener task to executor", ex);
            }
        };

        listenersMapping.put(listener, wrapper);
    }

    @Override
    public void removeListener(Consumer<T> listener) {
        Objects.requireNonNull(listener, "Listener cannot be null");

        listenersMapping.remove(listener);
    }
}
