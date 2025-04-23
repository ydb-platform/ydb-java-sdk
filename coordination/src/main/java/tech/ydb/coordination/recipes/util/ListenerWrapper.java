package tech.ydb.coordination.recipes.util;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.function.Consumer;

public class ListenerWrapper<T> implements Listenable<T> {
    private final ListenableAdder<T> listenableAdder;
    /**
     * key - original (external) consumer, value - consumer wrapper or original consumer depending on executor
     */
    private final Map<Consumer<T>, Consumer<T>> listenersMapping = new HashMap<>();

    public ListenerWrapper(ListenableAdder<T> listenableAdder) {
        this.listenableAdder = listenableAdder;
    }

    @Override
    public void addListener(Consumer<T> listener) {
        if (listenersMapping.containsKey(listener)) {
            return;
        }

        listenersMapping.put(listener, listener);
        listenableAdder.addListener(listener);
    }

    @Override
    public void addListener(Consumer<T> listener, ExecutorService executor) {
        if (listenersMapping.containsKey(listener)) {
            return;
        }

        Consumer<T> wrapper = T -> executor.submit(() -> listener.accept(T));
        listenersMapping.put(listener, wrapper);
        listenableAdder.addListener(wrapper);
    }

    @Override
    public void removeListener(Consumer<T> listener) {
        Consumer<T> removed = listenersMapping.remove(listener);
        listenableAdder.removeListener(removed);
    }

    @Override
    public void clearListeners() {
        listenersMapping.keySet().forEach(this::removeListener);
        listenersMapping.clear();
    }
}
