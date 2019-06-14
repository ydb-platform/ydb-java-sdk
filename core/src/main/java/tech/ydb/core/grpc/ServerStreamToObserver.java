package tech.ydb.core.grpc;

import javax.annotation.Nullable;

import tech.ydb.core.rpc.StreamObserver;
import io.grpc.ClientCall;
import io.grpc.Metadata;
import io.grpc.Status;
import io.grpc.stub.ClientCallStreamObserver;


/**
 * @author Sergey Polovko
 */
final class ServerStreamToObserver<ReqT, RespT> extends ClientCall.Listener<RespT> {

    private final StreamObserver<RespT> observer;
    private final CallToStreamObserverAdapter<ReqT> adapter;

    ServerStreamToObserver(StreamObserver<RespT> observer, ClientCall<ReqT, RespT> call) {
        this.observer = observer;
        this.adapter = new CallToStreamObserverAdapter<>(call);
    }

    @Override
    public void onHeaders(Metadata headers) {
    }

    @Override
    public void onMessage(RespT message) {
        observer.onNext(message);
        // request delivery of the next inbound message.
        adapter.request(1);
    }

    @Override
    public void onClose(Status status, @Nullable Metadata trailers) {
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
