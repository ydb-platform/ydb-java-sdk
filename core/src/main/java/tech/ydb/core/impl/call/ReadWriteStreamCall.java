package tech.ydb.core.impl.call;

import java.util.ArrayDeque;
import java.util.Queue;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;
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
import tech.ydb.core.grpc.GrpcReadWriteStream;
import tech.ydb.core.grpc.GrpcStatuses;
import tech.ydb.core.grpc.GrpcTransport;
import tech.ydb.core.impl.auth.AuthCallOptions;

/**
 *
 * @author Aleksandr Gorshenin
 * @param <R> type of message received
 * @param <W> type of message to be sent to the server
 */
public class ReadWriteStreamCall<R, W> extends ClientCall.Listener<R> implements GrpcReadWriteStream<R, W> {
    // GrpcTransport's logger is used intentionally
    private static final Logger logger = LoggerFactory.getLogger(GrpcTransport.class);

    private final String traceId;
    private final ClientCall<W, R> call;
    private final Lock callLock = new ReentrantLock();
    private final GrpcStatusHandler statusConsumer;
    private final Metadata headers;
    private final AuthCallOptions callOptions;

    private final CompletableFuture<Status> statusFuture = new CompletableFuture<>();
    private final AtomicReference<Observer<R>> observerReference = new AtomicReference<>();
    private final Queue<W> messagesQueue = new ArrayDeque<>();

    public ReadWriteStreamCall(
            String traceId,
            ClientCall<W, R> call,
            Metadata headers,
            AuthCallOptions callOptions,
            GrpcStatusHandler statusConsumer
    ) {
        this.traceId = traceId;
        this.call = call;
        this.headers = headers;
        this.statusConsumer = statusConsumer;
        this.callOptions = callOptions;
    }

    @Override
    public String authToken() {
        return callOptions.getToken();
    }

    @Override
    public CompletableFuture<Status> start(Observer<R> observer) {
        if (!observerReference.compareAndSet(null, observer)) {
            throw new IllegalStateException("Read stream call is already started");
        }

        callLock.lock();

            try {
                call.start(this, headers);
                call.request(1);
            } catch (Throwable t) {
                try {
                    call.cancel(null, t);
                } catch (Throwable ex) {
                    logger.error("Exception encountered while closing the unary call", ex);
                }

                statusFuture.completeExceptionally(t);
            } finally {

                callLock.unlock();
            }

        return statusFuture;
    }

    @Override
    public void sendNext(W message) {
        callLock.lock();
        try {
            if (flush()) {
                if (logger.isTraceEnabled()) {
                    String msg = TextFormat.shortDebugString((Message) message);
                    logger.trace("ReadWriteStreamCall[{}] --> {}", traceId, msg);
                }
                call.sendMessage(message);
            } else {
                messagesQueue.add(message);
            }
        } finally {
            callLock.unlock();
        }
    }

    private boolean flush() {
        while (call.isReady()) {
            W next = messagesQueue.poll();
            if (next == null) { // queue is empty, call is ready to send messages
                return true;
            }

            if (logger.isTraceEnabled()) {
                String msg = TextFormat.shortDebugString((Message) next);
                logger.trace("ReadWriteStreamCall[{}] --> {}", traceId, msg);
            }
            call.sendMessage(next);
        }
        // call is not ready
        return false;
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
    public void onMessage(R message) {
        try {
            if (logger.isTraceEnabled()) {
                logger.trace("ReadWriteStreamCall[{}] <-- {}", traceId, TextFormat.shortDebugString((Message) message));
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

            try {
                callLock.lock();
                try {
                    call.cancel("Canceled by exception from observer", ex);
                } finally {
                    callLock.unlock();
                }
            } catch (Throwable th) {
                logger.error("Exception encountered while canceling the read write stream call", th);
            }
        }
    }

    @Override
    public void onReady() {
        callLock.lock();
        try {
            flush();
        } finally {
            callLock.unlock();
        }
    }

    @Override
    public void close() {
        callLock.lock();
        try {
            call.halfClose();
        } finally {
            callLock.unlock();
        }
    }

    @Override
    public void onClose(io.grpc.Status status, @Nullable Metadata trailers) {
        if (logger.isTraceEnabled()) {
            logger.trace("ReadWriteStreamCall[{}] closed with status {}", traceId, status);
        }
        statusConsumer.accept(status, trailers);

        if (status.isOk()) {
            statusFuture.complete(Status.SUCCESS);
        } else {
            statusFuture.complete(GrpcStatuses.toStatus(status));
        }
    }
}
