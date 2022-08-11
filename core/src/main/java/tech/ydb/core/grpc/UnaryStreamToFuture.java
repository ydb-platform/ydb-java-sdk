package tech.ydb.core.grpc;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import javax.annotation.Nullable;

import tech.ydb.core.Issue;
import tech.ydb.core.Result;
import tech.ydb.core.Status;
import tech.ydb.core.StatusCode;

import io.grpc.ClientCall;
import io.grpc.Metadata;


/**
 * @author Sergey Polovko
 */
public class UnaryStreamToFuture<T> extends ClientCall.Listener<T> {
    private final static Status NO_VALUE = Status.of(
            StatusCode.CLIENT_INTERNAL_ERROR, null,
            Issue.of("No value received for gRPC unary call", Issue.Severity.ERROR)
    );
    
    private final static Status MULTIPLY_VALUES = Status.of(
            StatusCode.CLIENT_INTERNAL_ERROR, null,
            Issue.of("More than one value received for gRPC unary call", Issue.Severity.ERROR)
    );

    private final CompletableFuture<Result<T>> responseFuture;
    private final Consumer<Metadata> trailersHandler;
    private final Consumer<io.grpc.Status> statusHandler;
    private T value;

    public UnaryStreamToFuture(CompletableFuture<Result<T>> responseFuture,
            Consumer<Metadata> trailersHandler,
            Consumer<io.grpc.Status> statusHandler) {
        this.responseFuture = responseFuture;
        this.trailersHandler = trailersHandler;
        this.statusHandler = statusHandler;
    }

    @Override
    public void onMessage(T value) {
        if (this.value != null) {
            responseFuture.complete(Result.fail(MULTIPLY_VALUES));
        }
        this.value = value;
    }

    @Override
    public void onClose(io.grpc.Status status, @Nullable Metadata trailers) {
        if (trailersHandler != null && trailers != null) {
            trailersHandler.accept(trailers);
        }
        if (statusHandler != null) {
            statusHandler.accept(status);
        }

        if (status.isOk()) {
            if (value == null) {
                responseFuture.complete(Result.fail(NO_VALUE));
            } else {
                responseFuture.complete(Result.success(value));
            }
        } else {
            responseFuture.complete(GrpcStatuses.toResult(status));
        }
    }
}
