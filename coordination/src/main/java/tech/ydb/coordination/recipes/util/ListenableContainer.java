package tech.ydb.coordination.recipes.util;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ListenableContainer<T> implements Listenable<T> {
    private static final Logger logger = LoggerFactory.getLogger(ListenableContainer.class);

    private final Map<Consumer<T>, Consumer<T>> listenersMapping = new ConcurrentHashMap<>();

    public void notifyListeners(T data) {
        listenersMapping.values().forEach(listener -> {
            try {
                listener.accept(data);
            } catch (Exception ex) {
                logger.error("Listener threw exception", ex);
            }
        });
    }

    @Override
    public void addListener(Consumer listener) {
        if (listenersMapping.containsKey(listener)) {
            return;
        }

        listenersMapping.put(listener, listener);
    }

    @Override
    public void addListener(Consumer listener, ExecutorService executor) {
        if (listenersMapping.containsKey(listener)) {
            return;
        }

        Consumer<T> wrapper = new Consumer<T>() {
            @Override
            public void accept(T data) {
                executor.submit(() -> listener.accept(data));
            }
        };
        listenersMapping.put(listener, wrapper);
    }

    @Override
    public void removeListener(Consumer listener) {
        listenersMapping.remove(listener);
    }
}
