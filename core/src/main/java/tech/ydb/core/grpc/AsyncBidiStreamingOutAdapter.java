package tech.ydb.core.grpc;

import tech.ydb.core.rpc.OutStreamObserver;
import io.grpc.ClientCall;

public class AsyncBidiStreamingOutAdapter<ReqT, RespT> implements OutStreamObserver<ReqT> {

    private final ClientCall<ReqT, RespT> call;
    private boolean aborted = false;
    private boolean completed = false;

    public AsyncBidiStreamingOutAdapter(ClientCall<ReqT, RespT> call) {
        this.call = call;
    }

    @Override
    public void onNext(ReqT value) {
        if (aborted) {
            throw new IllegalStateException("The call was already aborted");
        }
        if (completed) {
            throw new IllegalStateException("The call was already completed");
        }
        call.sendMessage(value);
    }

    @Override
    public void onError(Throwable t) {
        call.cancel("The call was cancelled by the client", t);
        aborted = true;
    }

    @Override
    public void onCompleted() {
        call.halfClose();
        completed = true;
    }

    void requestOne() {
        call.request(1);
    }

}
