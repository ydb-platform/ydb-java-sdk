package tech.ydb.core.grpc;

import java.util.concurrent.CompletableFuture;

import tech.ydb.core.Status;

/**
 *
 * @author Aleksandr Gorshenin
 * @param <R> type of message received
 */
public interface GrpcReadStream<R> {
    @FunctionalInterface
    interface Observer<R> {
        void onNext(R value);
    }

    /**
     * Start a stream, using {@code observer} for processing response messages.
     * Returns future with the stream finish status.
     *
     * @param observer receives response messages
     * @return future with the stream finish status
     * @throws IllegalStateException if a method (including {@code start()}) on this class has been
     *                               called.
     */
    CompletableFuture<Status> start(Observer<R> observer);

    /**
     * Prevent any further processing for this {@code GrpcReadStream}. No further messages may be sent or
     * will be received. The server is informed of cancellations, but may not stop processing the
     * call. The future for the stream finish status will be completed with CANCELLED code
     */
    void cancel();
}
