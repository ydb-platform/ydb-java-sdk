package tech.ydb.core.impl.call;

import java.util.ArrayDeque;
import java.util.Queue;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

import javax.annotation.Nullable;

import io.grpc.ClientCall;
import io.grpc.Metadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tech.ydb.core.Status;
import tech.ydb.core.grpc.GrpcReadWriteStream;
import tech.ydb.core.grpc.GrpcStatuses;
import tech.ydb.core.impl.auth.AuthCallOptions;

/**
 *
 * @author Aleksandr Gorshenin
 * @param <R> type of message received
 * @param <W> type of message to be sent to the server
 */
public class ReadWriteStreamCall<R, W> extends ClientCall.Listener<R> implements GrpcReadWriteStream<R, W> {
    private static final Logger logger = LoggerFactory.getLogger(ReadStreamCall.class);

    private final ClientCall<W, R> call;
    private final GrpcStatusHandler statusConsumer;
    private final Metadata headers;
    private final AuthCallOptions callOptions;

    private final CompletableFuture<Status> statusFuture = new CompletableFuture<>();
    private final AtomicReference<Observer<R>> observerReference = new AtomicReference<>();
    private final Queue<W> messagesQueue = new ArrayDeque<>();

    public ReadWriteStreamCall(
            ClientCall<W, R> call,
            Metadata headers,
            AuthCallOptions callOptions,
            GrpcStatusHandler statusConsumer
    ) {
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

        synchronized (call) {
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
            }
        }

        return statusFuture;
    }

    @Override
    public void sendNext(W message) {
        synchronized (call) {
            if (flush()) {
                call.sendMessage(message);
            } else {
                messagesQueue.add(message);
            }
        }
    }

    private boolean flush() {
        while (call.isReady()) {
            W next = messagesQueue.poll();
            if (next == null) { // queue is empty, call is ready to send messages
                return true;
            }

            call.sendMessage(next);
        }
        // call is not ready
        return false;
    }

    @Override
    public void cancel() {
        synchronized (call) {
            call.cancel("Cancelled on user request", new CancellationException());
        }
    }

    @Override
    public void onMessage(R message) {
        try {
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
                logger.error("Exception encountered while canceling the read write stream call", th);
            }
        }
    }

    @Override
    public void onReady() {
        synchronized (call) {
            flush();
        }
    }

    @Override
    public void close() {
        synchronized (call) {
            call.halfClose();
        }
    }

    @Override
    public void onClose(io.grpc.Status status, @Nullable Metadata trailers) {
        statusConsumer.accept(status, trailers);

        if (status.isOk()) {
            statusFuture.complete(Status.SUCCESS);
        } else {
            statusFuture.complete(GrpcStatuses.toStatus(status));
        }
    }
}

