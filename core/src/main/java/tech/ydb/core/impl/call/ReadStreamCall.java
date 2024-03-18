package tech.ydb.core.impl.call;

import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;

import javax.annotation.Nullable;

import io.grpc.ClientCall;
import io.grpc.Metadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tech.ydb.core.Status;
import tech.ydb.core.grpc.GrpcReadStream;
import tech.ydb.core.grpc.GrpcStatuses;

/**
 *
 * @author Aleksandr Gorshenin
 * @param <ReqT> type of call argument
 * @param <RespT> type of read stream messages
 */
public class ReadStreamCall<ReqT, RespT> implements GrpcReadStream<RespT> {
    private static final Logger logger = LoggerFactory.getLogger(ReadStreamCall.class);

    private final ClientCall<ReqT, RespT> call;
    private final GrpcStatusHandler statusHandler;
    private final ReqT request;
    private final Metadata headers;

    public ReadStreamCall(ClientCall<ReqT, RespT> call, ReqT req, Metadata headers, GrpcStatusHandler statusHandler) {
        this.call = call;
        this.request = req;
        this.headers = headers != null ? headers : new Metadata();
        this.statusHandler = statusHandler;
    }

    @Override
    public CompletableFuture<Status> start(Observer<RespT> observer) {
        final CompletableFuture<Status> statusFuture = new CompletableFuture<>();

        synchronized (call) {
            try {
                call.start(new ClientCall.Listener<RespT>() {
                    @Override
                    public void onMessage(RespT message) {
                        try {
                            if (observer != null) {
                                observer.onNext(message);
                            }
                            // request delivery of the next inbound message.
                            synchronized (call) {
                                call.request(1);
                            }
                        } catch (Exception ex) {
                            statusFuture.completeExceptionally(ex);

                            try {
                                synchronized (call) {
                                    call.cancel("Canceled by exception from observer", ex);
                                }
                            } catch (Throwable th) {
                                logger.error("Exception encountered while canceling the read stream call", th);
                            }
                        }
                    }

                    @Override
                    public void onClose(io.grpc.Status status, @Nullable Metadata trailers) {
                        statusHandler.accept(status, trailers);
                        if (status.isOk()) {
                            statusFuture.complete(Status.SUCCESS);
                        } else {
                            statusFuture.complete(GrpcStatuses.toStatus(status));
                        }
                    }
                }, headers);
                call.request(1);
                call.sendMessage(request);
                // close stream by client side
                call.halfClose();
            } catch (Throwable t) {
                try {
                    call.cancel(null, t);
                } catch (Throwable ex) {
                    logger.error("Exception encountered while closing the unary call", ex);
                }

                statusFuture.completeExceptionally(t);
            }
        }

        return statusFuture;
    }

    @Override
    public void cancel() {
        synchronized (call) {
            call.cancel("Cancelled on user request", new CancellationException());
        }
    }
}
