package tech.ydb.core.impl;

import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import io.grpc.CallOptions;
import io.grpc.ClientCall;
import io.grpc.Metadata;
import io.grpc.MethodDescriptor;
import io.grpc.Status;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tech.ydb.core.Issue;
import tech.ydb.core.Result;
import tech.ydb.core.StatusCode;
import tech.ydb.core.grpc.GrpcRequestSettings;
import tech.ydb.core.grpc.GrpcStatuses;
import tech.ydb.core.grpc.GrpcTransport;
import tech.ydb.core.impl.pool.GrpcChannel;
import tech.ydb.core.rpc.StreamControl;
import tech.ydb.core.rpc.StreamObserver;

/**
 *
 * @author Aleksandr Gorshenin
 */
public abstract class BaseGrpcTrasnsport implements GrpcTransport {
    private static final Logger logger = LoggerFactory.getLogger(GrpcTransport.class);

    public abstract CallOptions getCallOptions();
    abstract GrpcChannel getChannel(GrpcRequestSettings settings);
    abstract void updateChannelStatus(GrpcChannel channel, Status status);

    @Override
    public <ReqT, RespT> CompletableFuture<Result<RespT>> unaryCall(
            MethodDescriptor<ReqT, RespT> method,
            GrpcRequestSettings settings,
            ReqT request
    ) {
        CallOptions options = getCallOptions();
        if (settings.getDeadlineAfter() > 0) {
            final long now = System.nanoTime();
            if (now >= settings.getDeadlineAfter()) {
                return CompletableFuture.completedFuture(deadlineExpiredResult(method));
            }
            options = options.withDeadlineAfter(settings.getDeadlineAfter() - now, TimeUnit.NANOSECONDS);
        }

        CompletableFuture<Result<RespT>> promise = new CompletableFuture<>();
        try {
            GrpcChannel channel = getChannel(settings);

            ClientCall<ReqT, RespT> call = channel.getReadyChannel().newCall(method, options);
            if (logger.isTraceEnabled()) {
                logger.trace("Sending request to {}, method `{}', request: `{}'",
                        channel.getEndpoint(),
                        method,
                        request);
            }
            sendOneRequest(call, request, settings, new UnaryStreamToFuture<>(
                    promise, settings.getTrailersHandler(), status -> updateChannelStatus(channel, status)
            ));
        } catch (RuntimeException ex) {
            logger.error("unary call problem {}", ex.getMessage());
            promise.completeExceptionally(ex);
        }
        return promise;
    }

    @Override
    public <ReqT, RespT> StreamControl serverStreamCall(
            MethodDescriptor<ReqT, RespT> method,
            GrpcRequestSettings settings,
            ReqT request,
            StreamObserver<RespT> observer
        ) {
        CallOptions options = getCallOptions();
        if (settings.getDeadlineAfter() > 0) {
            final long now = System.nanoTime();
            if (now >= settings.getDeadlineAfter()) {
                observer.onError(GrpcStatuses.toStatus(deadlineExpiredStatus(method)));
                return () -> { };
            }
            options = options.withDeadlineAfter(settings.getDeadlineAfter() - now, TimeUnit.NANOSECONDS);
        }

        try {
            GrpcChannel channel = getChannel(settings);
            ClientCall<ReqT, RespT> call = channel.getReadyChannel().newCall(method, options);
            if (logger.isTraceEnabled()) {
                logger.trace("Sending stream call to {}, method `{}', request: `{}'",
                        channel.getEndpoint(),
                        method,
                        request);
            }
            sendOneRequest(call, request, settings, new ServerStreamToObserver<>(
                    observer, call, settings.getTrailersHandler(), status -> updateChannelStatus(channel, status)
            ));

            return () -> {
                call.cancel("Cancelled on user request", new CancellationException());
            };
        } catch (RuntimeException ex) {
            logger.error("server stream call problem {}", ex.getMessage());
            Issue issue = Issue.of(ex.getMessage(), Issue.Severity.ERROR);
            observer.onError(tech.ydb.core.Status.of(StatusCode.CLIENT_INTERNAL_ERROR, null, issue));
            return () -> { };
        }
    }

    private static <ReqT, RespT> void sendOneRequest(
            ClientCall<ReqT, RespT> call,
            ReqT request,
            GrpcRequestSettings settings,
            ClientCall.Listener<RespT> listener) {
        try {
            Metadata headers = settings.getExtraHeaders();
            call.start(listener, headers != null ? headers : new Metadata());
            call.request(1);
            call.sendMessage(request);
            call.halfClose();
        } catch (Throwable t) {
            try {
                call.cancel(null, t);
            } catch (Throwable ex) {
                logger.error("Exception encountered while closing the call", ex);
            }
            listener.onClose(Status.INTERNAL.withCause(t), null);
        }
    }

    private static <T> Result<T> deadlineExpiredResult(MethodDescriptor<?, T> method) {
        String message = "deadline expired before calling method " + method.getFullMethodName();
        return Result.fail(tech.ydb.core.Status.of(
                StatusCode.CLIENT_DEADLINE_EXPIRED, null, Issue.of(message, Issue.Severity.ERROR)));
    }

    private static Status deadlineExpiredStatus(MethodDescriptor<?, ?> method) {
        String message = "deadline expired before calling method " + method.getFullMethodName();
        return Status.DEADLINE_EXCEEDED.withDescription(message);
    }

}
