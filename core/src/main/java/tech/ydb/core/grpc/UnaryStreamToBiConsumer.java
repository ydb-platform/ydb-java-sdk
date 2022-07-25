package tech.ydb.core.grpc;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import javax.annotation.Nullable;

import io.grpc.ClientCall;
import io.grpc.Metadata;
import io.grpc.Status;


/**
 * @author Sergey Polovko
 */
public class UnaryStreamToBiConsumer<T> extends ClientCall.Listener<T> {
    private final BiConsumer<T, Status> consumer;
    private final Consumer<Status> errorHandler;
    private final Consumer<Metadata> trailersHandler;
    private final AtomicBoolean accepted = new AtomicBoolean(false);
    private T value;

    public UnaryStreamToBiConsumer(
            BiConsumer<T, Status> consumer,
            Consumer<Status> errorHandler,
            Consumer<Metadata> trailersHandler) {
        this.consumer = consumer;
        this.errorHandler = errorHandler;
        this.trailersHandler = trailersHandler;
    }

    @Override
    public void onMessage(T value) {
        if (this.value != null) {
            accept(null, Status.INTERNAL.withDescription("More than one value received for gRPC unary call"));
        } else {
            this.value = value;
        }
    }

    @Override
    public void onClose(Status status, @Nullable Metadata trailers) {
        if (trailersHandler != null && trailers != null) {
            trailersHandler.accept(trailers);
        }
        if (status.isOk()) {
            if (value == null) {
                accept(null, Status.INTERNAL.withDescription("No value received for gRPC unary call"));
            } else {
                accept(value, status);
            }
        } else {
            accept(null, status);
            if (errorHandler != null) {
                errorHandler.accept(status);
            }
        }
    }

    private void accept(T value, Status status) {
        // protect from double accept
        if (accepted.compareAndSet(false, true)) {
            consumer.accept(value, status);
        }
    }
}
