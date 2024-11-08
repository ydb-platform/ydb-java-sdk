package tech.ydb.core.impl.call;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

import javax.annotation.Nullable;

import com.google.protobuf.Message;
import com.google.protobuf.TextFormat;
import io.grpc.ClientCall;
import io.grpc.Metadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tech.ydb.core.Issue;
import tech.ydb.core.Result;
import tech.ydb.core.Status;
import tech.ydb.core.StatusCode;
import tech.ydb.core.grpc.GrpcStatuses;
import tech.ydb.core.grpc.GrpcTransport;

/**
 *
 * @author Aleksandr Gorshenin
 * @param <ReqT> type of call argument
 * @param <RespT> type of call return
 */
public class UnaryCall<ReqT, RespT> extends ClientCall.Listener<RespT> {
    private static final Logger logger = LoggerFactory.getLogger(GrpcTransport.class);

    private static final Status NO_VALUE = Status.of(StatusCode.CLIENT_INTERNAL_ERROR)
            .withIssues(Issue.of("No value received for gRPC unary call", Issue.Severity.ERROR));

    private static final Status MULTIPLY_VALUES = Status.of(StatusCode.CLIENT_INTERNAL_ERROR)
            .withIssues(Issue.of("More than one value received for gRPC unary call", Issue.Severity.ERROR));

    private final String traceId;
    private final ClientCall<ReqT, RespT> call;
    private final GrpcStatusHandler statusConsumer;

    private final CompletableFuture<Result<RespT>> future = new CompletableFuture<>();
    private final AtomicReference<RespT> value = new AtomicReference<>();

    public UnaryCall(String traceId, ClientCall<ReqT, RespT> call, GrpcStatusHandler statusConsumer) {
        this.traceId = traceId;
        this.call = call;
        this.statusConsumer = statusConsumer;
    }

    public CompletableFuture<Result<RespT>> startCall(ReqT request, Metadata headers) {
        try {
            call.start(this, headers);
            if (logger.isTraceEnabled()) {
                logger.trace("UnaryCall[{}] --> {}", traceId, TextFormat.shortDebugString((Message) request));
            }
            call.sendMessage(request);
            call.halfClose();
            call.request(1);
        } catch (Exception ex) {
            future.completeExceptionally(ex);
            try {
                call.cancel(ex.getMessage(), ex);
            } catch (Exception ex2) {
                logger.error("UnaryCall[{}] got exception while canceling", traceId, ex2);
            }
        }

        return future;
    }

    @Override
    public void onMessage(RespT value) {
        if (logger.isTraceEnabled()) {
            logger.trace("UnaryCall[{}] <-- {}", traceId, TextFormat.shortDebugString((Message) value));
        }
        if (!this.value.compareAndSet(null, value)) {
            future.complete(Result.fail(MULTIPLY_VALUES));
        }
    }

    @Override
    public void onClose(io.grpc.Status status, @Nullable Metadata trailers) {
        statusConsumer.accept(status, trailers);
        if (logger.isTraceEnabled()) {
            logger.trace("UnaryCall[{}] closed with status {}", traceId, status);
        }

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
