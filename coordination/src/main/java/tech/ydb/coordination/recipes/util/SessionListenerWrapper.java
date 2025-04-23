package tech.ydb.coordination.recipes.util;

import tech.ydb.coordination.CoordinationSession;
import tech.ydb.coordination.CoordinationSession.State;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.function.Consumer;

public class SessionListenerWrapper implements Listenable<State> {
    private final CoordinationSession session;
    /**
     * key - original (external) consumer, value - consumer wrapper or original consumer depending on executor
     */
    private final Map<Consumer<State>, Consumer<State>> listenersMapping = new HashMap<>();

    public SessionListenerWrapper(CoordinationSession session) {
        this.session = session;
    }

    @Override
    public void addListener(Consumer<State> listener) {
        if (listenersMapping.containsKey(listener)) {
            return;
        }

        listenersMapping.put(listener, listener);
        session.addStateListener(listener);
    }

    @Override
    public void addListener(Consumer<State> listener, ExecutorService executor) {
        if (listenersMapping.containsKey(listener)) {
            return;
        }

        Consumer<State> wrapper = state -> executor.submit(() -> listener.accept(state));
        listenersMapping.put(listener, wrapper);
        session.addStateListener(wrapper);
    }

    @Override
    public void removeListener(Consumer<State> listener) {
        Consumer<State> removed = listenersMapping.remove(listener);
        session.removeStateListener(removed);
    }

    @Override
    public void clearListeners() {
        listenersMapping.keySet().forEach(this::removeListener);
        listenersMapping.clear();
    }
}
