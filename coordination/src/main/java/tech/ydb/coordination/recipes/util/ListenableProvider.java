package tech.ydb.coordination.recipes.util;


import java.util.concurrent.ExecutorService;
import java.util.function.Consumer;

public interface ListenableProvider<T> extends Listenable<T> {
    Listenable<T> getListenable();

    @Override
    default void addListener(Consumer<T> listener) {
        getListenable().addListener(listener);
    }

    @Override
    default void addListener(Consumer<T> listener, ExecutorService executor) {
        getListenable().addListener(listener, executor);
    }

    @Override
    default void removeListener(Consumer<T> listener) {
        getListenable().removeListener(listener);
    }

    @Override
    default void clearListeners() {
        getListenable().clearListeners();
    }
}
