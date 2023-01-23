package tech.ydb.core.impl;

import java.util.function.Consumer;

import javax.annotation.Nullable;

import io.grpc.ClientCall;
import io.grpc.Metadata;
import io.grpc.Status;
import io.grpc.stub.ClientCallStreamObserver;

import tech.ydb.core.grpc.GrpcStatuses;
import tech.ydb.core.rpc.StreamObserver;


/**
 * @author Sergey Polovko
 * @param <ReqT> type of request
 * @param <RespT> type of response
 */
public class ServerStreamToObserver<ReqT, RespT> extends ClientCall.Listener<RespT> {

    private final StreamObserver<RespT> observer;
    private final CallToStreamObserverAdapter<ReqT> adapter;
    private final Consumer<Metadata> trailersHandler;
    private final Consumer<Status> statusHandler;

    public ServerStreamToObserver(StreamObserver<RespT> observer,
            ClientCall<ReqT, RespT> call,
            Consumer<Metadata> trailersHandler,
            Consumer<Status> statusHandler) {
        this.observer = observer;
        this.adapter = new CallToStreamObserverAdapter<>(call);
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
            adapter.request(1);
        } catch (Exception ex) {
            adapter.cancel(ex.getMessage(), ex);
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

    private static final class CallToStreamObserverAdapter<T> extends ClientCallStreamObserver<T> {
        private final ClientCall<T, ?> call;

        CallToStreamObserverAdapter(ClientCall<T, ?> call) {
            this.call = call;
        }

        @Override
        public void onNext(T value) {
            call.sendMessage(value);
        }

        @Override
        public void onError(Throwable t) {
            call.cancel("Cancelled by client with StreamObserver.onError()", t);
        }

        @Override
        public void onCompleted() {
            call.halfClose();
        }

        @Override
        public boolean isReady() {
            return call.isReady();
        }

        @Override
        public void setOnReadyHandler(Runnable onReadyHandler) {
            throw new IllegalStateException("Cannot alter onReadyHandler after call started");
        }

        @Override
        public void disableAutoInboundFlowControl() {
            throw new IllegalStateException("Cannot disable auto flow control call started");
        }

        @Override
        public void request(int count) {
            call.request(count);
        }

        @Override
        public void setMessageCompression(boolean enable) {
            call.setMessageCompression(enable);
        }

        @Override
        public void cancel(@Nullable String message, @Nullable Throwable cause) {
            call.cancel(message, cause);
        }
    }
}
