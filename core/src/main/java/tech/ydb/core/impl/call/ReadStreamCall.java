package tech.ydb.core.impl.call;

import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;

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
    private final ReentrantLock callLock = new ReentrantLock();
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

        callLock.lock();

        try {
            call.start(this, headers);
            call.request(1);
            if (logger.isTraceEnabled()) {
                logger.trace("ReadStreamCall[{}] --> {}", traceId, TextFormat.shortDebugString((Message) request));
            }
            call.sendMessage(request);
            // close stream by client side
            call.halfClose();
        } catch (Throwable t) {
            try {
                call.cancel(null, t);
            } catch (Throwable ex) {
                logger.error("ReadStreamCall[{}] got exception while canceling", traceId, ex);
            }

            statusFuture.completeExceptionally(t);
        } finally {
            callLock.unlock();
        }

        return statusFuture;
    }

    @Override
    public void cancel() {
        callLock.lock();

        try {
            call.cancel("Cancelled on user request", new CancellationException());
        } finally {
            callLock.unlock();
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
            callLock.lock();

            try {
                call.request(1);
            } finally {
                callLock.unlock();
            }
        } catch (Exception ex) {
            statusFuture.completeExceptionally(ex);
            callLock.lock();

            try {
                call.cancel("Canceled by exception from observer", ex);
            } catch (Throwable th) {
                logger.error("ReadStreamCall[{}] got exception while canceling", traceId, th);
            } finally {
                callLock.unlock();
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
