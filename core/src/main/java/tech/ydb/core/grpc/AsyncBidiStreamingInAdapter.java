package tech.ydb.core.grpc;

import java.util.function.Consumer;

import tech.ydb.core.rpc.StreamObserver;
import io.grpc.ClientCall;
import io.grpc.Metadata;
import io.grpc.Status;

public class AsyncBidiStreamingInAdapter<ReqT, RespT> extends ClientCall.Listener<RespT> {

    private final StreamObserver<RespT> observer;
    private final AsyncBidiStreamingOutAdapter<ReqT, RespT> adapter;
    private final Consumer<Status> errorHandler;

    public AsyncBidiStreamingInAdapter(StreamObserver<RespT> observer,
                                       AsyncBidiStreamingOutAdapter<ReqT, RespT> adapter
                                       ) {
        this.observer = observer;
        this.adapter = adapter;
        this.errorHandler = null;
    }

    public AsyncBidiStreamingInAdapter(StreamObserver<RespT> observer,
                                       AsyncBidiStreamingOutAdapter<ReqT, RespT> adapter,
                                       Consumer<Status> errorHandler) {
        this.observer = observer;
        this.adapter = adapter;
        this.errorHandler = errorHandler;
    }

    @Override
    public void onHeaders(Metadata headers) {
    }

    @Override
    public void onMessage(RespT message) {
        observer.onNext(message);
        adapter.requestOne();
    }

    @Override
    public void onClose(Status status, Metadata trailers) {
        if (status.isOk()) {
            observer.onCompleted();
        } else {
            observer.onError(GrpcStatuses.toStatus(status));
            if (errorHandler != null) {
                errorHandler.accept(status);
            }
        }
    }

    @Override
    public void onReady() {
    }

    public void onStart() {
        adapter.requestOne();
    }

}
