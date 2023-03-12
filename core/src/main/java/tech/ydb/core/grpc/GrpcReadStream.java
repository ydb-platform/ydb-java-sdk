package tech.ydb.core.grpc;

import java.util.concurrent.CompletableFuture;

import tech.ydb.core.Status;

/**
 *
 * @author Aleksandr Gorshenin
 * @param <V> type of message received
 */
public interface GrpcReadStream<V> {
    interface Observer<V> {
        void onNext(V value);
    }

    CompletableFuture<Status> start(Observer<V> observer);

    void cancel();
}
