package tech.ydb.core.impl;

import java.util.function.Consumer;

import javax.annotation.Nullable;

import io.grpc.ClientCall;
import io.grpc.Metadata;
import io.grpc.Status;

import tech.ydb.core.grpc.GrpcStatuses;
import tech.ydb.core.rpc.StreamObserver;


/**
 * @author Sergey Polovko
 * @param <ReqT> type of request
 * @param <RespT> type of response
 */
public class ServerStreamToObserver<ReqT, RespT> extends ClientCall.Listener<RespT> {

    private final StreamObserver<RespT> observer;
    private final ClientCall<ReqT, RespT> call;
    private final Consumer<Metadata> trailersHandler;
    private final Consumer<Status> statusHandler;

    public ServerStreamToObserver(StreamObserver<RespT> observer,
            ClientCall<ReqT, RespT> call,
            Consumer<Metadata> trailersHandler,
            Consumer<Status> statusHandler) {
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
    public void onClose(Status status, @Nullable Metadata trailers) {
        if (trailersHandler != null && trailers != null) {
            trailersHandler.accept(trailers);
        }
        if (statusHandler != null) {
            statusHandler.accept(status);
        }

        if (status.isOk()) {
            observer.onCompleted();
        } else {
            observer.onError(GrpcStatuses.toStatus(status));
        }
    }

    @Override
    public void onReady() {
    }
}
