package tech.ydb.core.impl.call;

import java.util.concurrent.CompletableFuture;

import tech.ydb.core.Status;
import tech.ydb.core.grpc.GrpcReadStream;

/**
 *
 * @author Aleksandr Gorshenin
 * @param <BaseR> type of origin stream message
 * @param <DestR> new stream message type
 */
@Deprecated
public class ProxyReadStream<BaseR, DestR> implements GrpcReadStream<DestR> {
    public interface MessageFunctor<BaseR, DestR> {
        void apply(BaseR message, CompletableFuture<Status> promise, Observer<DestR> observer);
    }

    private final GrpcReadStream<BaseR> origin;
    private final MessageFunctor<BaseR, DestR> functor;
    private final CompletableFuture<Status> future = new CompletableFuture<>();

    public ProxyReadStream(GrpcReadStream<BaseR> origin, MessageFunctor<BaseR, DestR> functor) {
        this.origin = origin;
        this.functor = functor;
    }

    protected void onClose(Status status, Throwable th) {
        // promise may be completed by functor and in that case this code will be ignored
        if (th != null) {
            future.completeExceptionally(th);
        }
        if (status != null) {
            future.complete(status);
        }
    }

    @Override
    public CompletableFuture<Status> start(Observer<DestR> observer) {
        origin.start(response -> functor.apply(response, future, observer)).whenComplete(this::onClose);
        return future;
    }

    @Override
    public void cancel() {
        origin.cancel();
    }
}
