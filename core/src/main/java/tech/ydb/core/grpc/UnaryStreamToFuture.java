package tech.ydb.core.grpc;

import java.util.concurrent.CompletableFuture;

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
    private T value;

    public UnaryStreamToFuture(CompletableFuture<Result<T>> responseFuture) {
        this.responseFuture = responseFuture;
    }

    @Override
    public void onMessage(T value) {
        if (this.value != null) {
            Issue issue = Issue.of("More than one value received for gRPC unary call", Issue.Severity.ERROR);
            responseFuture.complete(Result.fail(StatusCode.CLIENT_INTERNAL_ERROR, issue));
        }
        this.value = value;
    }

    @Override
    public void onClose(Status status, @Nullable Metadata trailers) {
        if (status.isOk()) {
            if (value == null) {
                Issue issue = Issue.of("No value received for gRPC unary call", Issue.Severity.ERROR);
                responseFuture.complete(Result.fail(StatusCode.CLIENT_INTERNAL_ERROR, issue));
            } else {
                responseFuture.complete(Result.success(value));
            }
        } else {
            responseFuture.complete(GrpcStatuses.toResult(status));
        }
    }
}
