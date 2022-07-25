package tech.ydb.core.grpc;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import tech.ydb.core.Issue;
import tech.ydb.core.Result;
import tech.ydb.core.StatusCode;
import io.grpc.ClientCall;
import io.grpc.Metadata;
import io.grpc.Status;


/**
 * @author Sergey Polovko
 */
public class UnaryStreamToConsumer<T> extends ClientCall.Listener<T> {

    private final Consumer<Result<T>> consumer;
    private final Consumer<Status> errorHandler;
    private final Consumer<Metadata> trailersHandler;
    private final AtomicBoolean accepted = new AtomicBoolean(false);

    private T value;

    public UnaryStreamToConsumer(
            Consumer<Result<T>> consumer,
            Consumer<Status> errorHandler,
            Consumer<Metadata> trailersHandler) {
        this.consumer = consumer;
        this.errorHandler = errorHandler;
        this.trailersHandler = trailersHandler;
    }

    @Override
    public void onMessage(T value) {
        if (this.value != null) {
            Issue issue = Issue.of("More than one value received for gRPC unary call", Issue.Severity.ERROR);
            accept(Result.fail(StatusCode.CLIENT_INTERNAL_ERROR, issue));
        } else {
            this.value = value;
        }
    }

    @Override
    public void onClose(Status status, Metadata trailers) {
        if (trailersHandler != null && trailers != null) {
            trailersHandler.accept(trailers);
        }
        if (status.isOk()) {
            if (value == null) {
                Issue issue = Issue.of("No value received for gRPC unary call", Issue.Severity.ERROR);
                accept(Result.fail(StatusCode.CLIENT_INTERNAL_ERROR, issue));
            } else {
                accept(Result.success(value));
            }
        } else {
            accept(GrpcStatuses.toResult(status));
            if (errorHandler != null) {
                errorHandler.accept(status);
            }
        }
    }

    void accept(Result<T> result) {
        // protect from double accept
        if (accepted.compareAndSet(false, true)) {
            consumer.accept(result);
        }
    }
}
