package tech.ydb.core.impl.stream;

import java.util.concurrent.CompletableFuture;

import tech.ydb.core.Status;
import tech.ydb.core.grpc.GrpcReadStream;
import tech.ydb.core.grpc.GrpcReadWriteStream;

/**
 * Empty stream without messages but with status
 * @author Aleksandr Gorshenin
 * @param <R> type of message received
 * @param <W> type of message to be sent to the server
 */
public class EmptyStream<R, W> implements GrpcReadWriteStream<R, W> {
    private final Status status;

    public EmptyStream(Status status) {
        this.status = status;
    }

    @Override
    public String authToken() {
        return null;
    }

    @Override
    public void cancel() {
        // nothing
    }

    @Override
    public CompletableFuture<Status> start(GrpcReadStream.Observer<R> observer) {
        return CompletableFuture.completedFuture(status);
    }

    @Override
    public void sendNext(W message) {
        // nothing
    }

    @Override
    public void close() {
        // nothing
    }
}
