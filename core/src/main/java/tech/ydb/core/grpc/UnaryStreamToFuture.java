package tech.ydb.core.grpc;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import javax.annotation.Nullable;

import tech.ydb.core.Issue;
import tech.ydb.core.Result;
import tech.ydb.core.StatusCode;
import io.grpc.ClientCall;
import io.grpc.Metadata;
import io.grpc.Status;


/**
 * @author Sergey Polovko
 */
public class UnaryStreamToFuture<T> extends ClientCall.Listener<T> {

    private final CompletableFuture<Result<T>> responseFuture;
    private final Consumer<Metadata> trailersHandler;
    private final Consumer<Status> statusHandler;
    private T value;

    public UnaryStreamToFuture(CompletableFuture<Result<T>> responseFuture,
            Consumer<Metadata> trailersHandler,
            Consumer<Status> statusHandler) {
        this.responseFuture = responseFuture;
        this.trailersHandler = trailersHandler;
        this.statusHandler = statusHandler;
    }

    @Override
    public void onMessage(T value) {
        if (this.value != null) {
            Issue issue = Issue.of("More than one value received for gRPC unary call", Issue.Severity.ERROR);
            responseFuture.complete(Result.fail(tech.ydb.core.Status.of(StatusCode.CLIENT_INTERNAL_ERROR, issue)));
        }
        this.value = value;
    }

    @Override
    public void onClose(Status status, @Nullable Metadata trailers) {
        if (trailersHandler != null && trailers != null) {
            trailersHandler.accept(trailers);
        }
        if (statusHandler != null) {
            statusHandler.accept(status);
        }

        if (status.isOk()) {
            if (value == null) {
                Issue issue = Issue.of("No value received for gRPC unary call", Issue.Severity.ERROR);
                responseFuture.complete(Result.fail(tech.ydb.core.Status.of(StatusCode.CLIENT_INTERNAL_ERROR, issue)));
            } else {
                responseFuture.complete(Result.success(value));
            }
        } else {
            responseFuture.complete(GrpcStatuses.toResult(status));
        }
    }
}
