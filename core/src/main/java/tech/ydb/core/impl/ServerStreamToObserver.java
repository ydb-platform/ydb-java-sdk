package tech.ydb.core.impl;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import javax.annotation.Nullable;

import io.grpc.ClientCall;
import io.grpc.Metadata;

import tech.ydb.core.Status;
import tech.ydb.core.grpc.GrpcReadStream;
import tech.ydb.core.grpc.GrpcStatuses;


/**
 * @author Sergey Polovko
 * @param <ReqT> type of request
 * @param <RespT> type of response
 */
public class ServerStreamToObserver<ReqT, RespT> extends ClientCall.Listener<RespT> {
    private final CompletableFuture<Status> statusFuture;
    private final GrpcReadStream.Observer<RespT> observer;
    private final ClientCall<ReqT, RespT> call;
    private final Consumer<Metadata> trailersHandler;
    private final Consumer<io.grpc.Status> statusHandler;

    public ServerStreamToObserver(GrpcReadStream.Observer<RespT> observer,
            CompletableFuture<Status> future,
            ClientCall<ReqT, RespT> call,
            Consumer<Metadata> trailersHandler,
            Consumer<io.grpc.Status> statusHandler) {
        this.statusFuture = future;
        this.observer = observer;
        this.call = call;
        this.trailersHandler = trailersHandler;
        this.statusHandler = statusHandler;
    }

    @Override
    public void onHeaders(Metadata headers) {
    }

    @Override
    public void onMessage(RespT message) {
        try {
            observer.onNext(message);
            // request delivery of the next inbound message.
            call.request(1);
        } catch (Exception ex) {
            call.cancel(ex.getMessage(), ex);
        }
    }

    @Override
    public void onClose(io.grpc.Status status, @Nullable Metadata trailers) {
        if (trailersHandler != null && trailers != null) {
            trailersHandler.accept(trailers);
        }
        if (statusHandler != null) {
            statusHandler.accept(status);
        }

        if (status.isOk()) {
            statusFuture.complete(Status.SUCCESS);
        } else {
            statusFuture.complete(GrpcStatuses.toStatus(status));
        }
    }

    @Override
    public void onReady() {
    }
}
