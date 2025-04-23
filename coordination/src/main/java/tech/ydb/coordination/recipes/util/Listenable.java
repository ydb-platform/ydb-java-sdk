package tech.ydb.coordination.recipes.util;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.function.Consumer;
import java.util.function.Function;

public interface Listenable<T> {
    void addListener(Consumer<T> listener);

    /**
     * Listener call will be processed in executor
     * @param listener
     * @param executor
     */
    void addListener(Consumer<T> listener, ExecutorService executor);

    void removeListener(Consumer<T> listener);

    void clearListeners();

    default CompletableFuture<T> waitUntil(Function<T, Boolean> condition) {
        CompletableFuture<T> future = new CompletableFuture<>();
        Consumer<T> listener = new Consumer<T>() {
            @Override
            public void accept(T data) {
                if (future.isDone()) return;

                if (Thread.currentThread().isInterrupted()) {
                    future.completeExceptionally(new InterruptedException());
                    return;
                }

                if (condition.apply(data)) {
                    future.complete(data);
                }
            }
        };

        future.whenComplete((result, ex) -> removeListener(listener));
        addListener(listener);

        return future;
    }
}
