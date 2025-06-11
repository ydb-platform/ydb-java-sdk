package tech.ydb.core.impl.call;

import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import javax.annotation.Nullable;

import com.google.protobuf.Message;
import com.google.protobuf.TextFormat;
import io.grpc.ClientCall;
import io.grpc.Metadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tech.ydb.core.Status;
import tech.ydb.core.grpc.GrpcFlowControl;
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
    private final Lock callLock = new ReentrantLock();
    private final GrpcStatusHandler statusConsumer;
    private final ReqT request;
    private final Metadata headers;
    private final GrpcFlowControl.Call flow;

    private final CompletableFuture<Status> statusFuture = new CompletableFuture<>();

    private Observer<RespT> consumer;

    public ReadStreamCall(String traceId, ClientCall<ReqT, RespT> call, GrpcFlowControl flowCtrl, ReqT req,
            Metadata headers, GrpcStatusHandler statusHandler) {
        this.traceId = traceId;
        this.call = call;
        this.request = req;
        this.headers = headers;
        this.statusConsumer = statusHandler;
        this.flow = flowCtrl.newCall(this::nextRequest);
    }

    @Override
    public CompletableFuture<Status> start(Observer<RespT> observer) {
        callLock.lock();
        try {
            if (consumer != null) {
                throw new IllegalStateException("Read stream call is already started");
            }
            if (observer == null) {
                throw new IllegalArgumentException("Observer must be not empty");
            }

            consumer = observer;
            call.start(this, headers);
            if (logger.isTraceEnabled()) {
                logger.trace("ReadStreamCall[{}] --> {}", traceId, TextFormat.shortDebugString((Message) request));
            }
            call.sendMessage(request);
            // close stream by client side
            call.halfClose();
            // init flow
            flow.onStart();
        } catch (Throwable th) {
            statusFuture.completeExceptionally(th);

            try {
                call.cancel(null, th);
            } catch (Throwable ex) {
                logger.error("ReadStreamCall[{}] got exception while canceling", traceId, ex);
            }
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

    private void nextRequest(int count) {
        // request delivery of the next inbound message.
        callLock.lock();
        try {
            call.request(count);
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
            consumer.onNext(message);
            flow.onMessageReaded();
        } catch (Exception ex) {
            statusFuture.completeExceptionally(ex);

            try {
                callLock.lock();
                try {
                    call.cancel("Canceled by exception from observer", ex);
                } finally {
                    callLock.unlock();
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
