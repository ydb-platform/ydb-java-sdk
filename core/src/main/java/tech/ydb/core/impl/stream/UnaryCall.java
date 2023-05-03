package tech.ydb.core.impl.stream;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;

import javax.annotation.Nullable;

import io.grpc.ClientCall;
import io.grpc.Metadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tech.ydb.core.Issue;
import tech.ydb.core.Result;
import tech.ydb.core.Status;
import tech.ydb.core.StatusCode;
import tech.ydb.core.grpc.GrpcStatuses;

/**
 *
 * @author Aleksandr Gorshenin
 * @param <ReqT> type of call argument
 * @param <RespT> type of call return
 */
public class UnaryCall<ReqT, RespT> extends ClientCall.Listener<RespT> {
    private static final Logger logger = LoggerFactory.getLogger(UnaryCall.class);

    private static final Status NO_VALUE = Status.of(StatusCode.CLIENT_INTERNAL_ERROR)
            .withIssues(Issue.of("No value received for gRPC unary call", Issue.Severity.ERROR));

    private static final Status MULTIPLY_VALUES = Status.of(StatusCode.CLIENT_INTERNAL_ERROR)
            .withIssues(Issue.of("More than one value received for gRPC unary call", Issue.Severity.ERROR));

    private final ClientCall<ReqT, RespT> call;
    private final BiConsumer<io.grpc.Status, Metadata> statusConsumer;

    private final CompletableFuture<Result<RespT>> future = new CompletableFuture<>();
    private final AtomicReference<RespT> value = new AtomicReference<>();

    public UnaryCall(ClientCall<ReqT, RespT> call, BiConsumer<io.grpc.Status, Metadata> statusConsumer) {
        this.call = call;
        this.statusConsumer = statusConsumer;
    }

    public CompletableFuture<Result<RespT>> startCall(ReqT request, Metadata headers) {
        try {
            call.start(this, headers != null ? headers : new Metadata());
            call.request(1);
            call.sendMessage(request);
            call.halfClose();
        } catch (Exception ex) {
            future.completeExceptionally(ex);
            try {
                call.cancel(ex.getMessage(), ex);
            } catch (Exception ex2) {
                logger.error("Exception encountered while closing the unary call", ex2);
            }
        }

        return future;
    }

    @Override
    public void onMessage(RespT value) {
        if (!this.value.compareAndSet(null, value)) {
            future.complete(Result.fail(MULTIPLY_VALUES));
        }
    }

    @Override
    public void onClose(io.grpc.Status status, @Nullable Metadata trailers) {
        statusConsumer.accept(status, trailers);

        if (status.isOk()) {
            RespT snapshotValue = value.get();

            if (snapshotValue == null) {
                future.complete(Result.fail(NO_VALUE));
            } else {
                future.complete(Result.success(snapshotValue));
            }
        } else {
            future.complete(GrpcStatuses.toResult(status));
        }
    }
}
