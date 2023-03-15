package tech.ydb.core.impl.stream;

import java.util.concurrent.CompletableFuture;

import tech.ydb.core.Status;
import tech.ydb.core.grpc.GrpcReadStream;

/**
 *
 * @author Aleksandr Gorshenin
 * @param <BaseR> type of origin stream message
 * @param <DestR> new stream message type
 */
public class ProxyReadStream<BaseR, DestR> implements GrpcReadStream<DestR> {
    public interface MessageFunctor<BaseR, DestR> {
        void apply(BaseR message, CompletableFuture<Status> promise, Observer<DestR> observer);
    }

    private final GrpcReadStream<BaseR> origin;
    private final MessageFunctor<BaseR, DestR> functor;

    public ProxyReadStream(GrpcReadStream<BaseR> origin, MessageFunctor<BaseR, DestR> functor) {
        this.origin = origin;
        this.functor = functor;
    }

    @Override
    public CompletableFuture<Status> start(Observer<DestR> observer) {
        final CompletableFuture<Status> promise = new CompletableFuture<>();

        origin.start(response -> functor.apply(response, promise, observer)).whenComplete((status, th) -> {
            // promise may be completed by functor and in that case this code will be ignored
            if (th != null) {
                promise.completeExceptionally(th);
            }
            if (status != null) {
                promise.complete(status);
            }
        });

        return promise;
    }

    @Override
    public void cancel() {
        origin.cancel();
    }
}
