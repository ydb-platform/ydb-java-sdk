package tech.ydb.core.grpc;

import tech.ydb.core.rpc.StreamObserver;
import io.grpc.ClientCall;
import io.grpc.Metadata;
import io.grpc.Status;

class AsyncBidiStreamingInAdapter<ReqT, RespT> extends ClientCall.Listener<RespT> {

    private final StreamObserver<RespT> observer;
    private final AsyncBidiStreamingOutAdapter<ReqT, RespT> adapter;

    AsyncBidiStreamingInAdapter(StreamObserver<RespT> observer, AsyncBidiStreamingOutAdapter<ReqT, RespT> adapter) {
        this.observer = observer;
        this.adapter = adapter;
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
        }
    }

    @Override
    public void onReady() {
    }

    void onStart() {
        adapter.requestOne();
    }

}
