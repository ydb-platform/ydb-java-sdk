package tech.ydb.core.impl.call;

import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

import javax.annotation.Nullable;

import com.google.protobuf.Message;
import com.google.protobuf.TextFormat;
import io.grpc.ClientCall;
import io.grpc.Metadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tech.ydb.core.Status;
import tech.ydb.core.grpc.GrpcReadStream;
import tech.ydb.core.grpc.GrpcStatuses;
import tech.ydb.core.grpc.GrpcTransport;

/**
 *
 * @author Aleksandr Gorshenin
 * @param <ReqT> type of call argument
 * @param <RespT> type of read stream messages
 */
public class ReadStreamCall<ReqT, RespT> extends ClientCall.Listener<RespT> implements GrpcReadStream<RespT> {
    private static final Logger logger = LoggerFactory.getLogger(GrpcTransport.class);

    private final String traceId;
    private final ClientCall<ReqT, RespT> call;
    private final GrpcStatusHandler statusConsumer;
    private final ReqT request;
    private final Metadata headers;

    private final CompletableFuture<Status> statusFuture = new CompletableFuture<>();
    private final AtomicReference<Observer<RespT>> observerReference = new AtomicReference<>();

    public ReadStreamCall(
            String traceId,
            ClientCall<ReqT, RespT> call,
            ReqT request,
            Metadata headers,
            GrpcStatusHandler statusConsumer
    ) {
        this.traceId = traceId;
        this.call = call;
        this.request = request;
        this.headers = headers;
        this.statusConsumer = statusConsumer;
    }

    @Override
    public CompletableFuture<Status> start(Observer<RespT> observer) {
        if (!observerReference.compareAndSet(null, observer)) {
            throw new IllegalStateException("Read stream call is already started");
        }

        synchronized (call) {
            try {
                call.start(this, headers);
                if (logger.isTraceEnabled()) {
                    logger.trace("ReadStreamCall[{}] --> {}", traceId, TextFormat.shortDebugString((Message) request));
                }
                call.sendMessage(request);
                // close stream by client side
                call.halfClose();
                call.request(1);
            } catch (Throwable t) {
                try {
                    call.cancel(null, t);
                } catch (Throwable ex) {
                    logger.error("ReadStreamCall[{}] got exception while canceling", traceId, ex);
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

    @Override
    public void onMessage(RespT message) {
        try {
            if (logger.isTraceEnabled()) {
                logger.trace("ReadStreamCall[{}] <-- {}", traceId, TextFormat.shortDebugString((Message) message));
            }
            observerReference.get().onNext(message);
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
                logger.error("ReadStreamCall[{}] got exception while canceling", traceId, th);
            }
        }
    }

    @Override
    public void onClose(io.grpc.Status status, @Nullable Metadata trailers) {
        if (logger.isTraceEnabled()) {
            logger.trace("ReadStreamCall[{}] closed with status {}", traceId, status);
        }

        statusConsumer.accept(status, trailers);

        if (status.isOk()) {
            statusFuture.complete(Status.SUCCESS);
        } else {
            statusFuture.complete(GrpcStatuses.toStatus(status));
        }
    }
}
