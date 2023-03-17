package tech.ydb.core.grpc;

import java.util.concurrent.CompletableFuture;

import tech.ydb.core.Status;

/**
 *
 * @author Aleksandr Gorshenin
 * @param <R> type of message received
 */
public interface GrpcReadStream<R> {
    interface Observer<R> {
        void onNext(R value);
    }

    CompletableFuture<Status> start(Observer<R> observer);

    void cancel();
}
