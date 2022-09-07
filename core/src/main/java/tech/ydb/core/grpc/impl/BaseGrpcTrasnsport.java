package tech.ydb.core.grpc.impl;

import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import tech.ydb.core.Issue;
import tech.ydb.core.Result;
import tech.ydb.core.StatusCode;
import tech.ydb.core.grpc.GrpcRequestSettings;
import tech.ydb.core.grpc.GrpcStatuses;
import tech.ydb.core.grpc.GrpcTransport;
import tech.ydb.core.grpc.ServerStreamToObserver;
import tech.ydb.core.grpc.UnaryStreamToFuture;
import tech.ydb.core.rpc.StreamControl;
import tech.ydb.core.rpc.StreamObserver;

import io.grpc.CallOptions;
import io.grpc.Channel;
import io.grpc.ClientCall;
import io.grpc.Metadata;
import io.grpc.MethodDescriptor;
import io.grpc.Status;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Aleksandr Gorshenin
 */
public abstract class BaseGrpcTrasnsport implements GrpcTransport {
    private static final Logger logger = LoggerFactory.getLogger(GrpcTransport.class);
    
    protected interface CheckableChannel {
        Channel grpcChannel();
        String endpoint();
        void updateGrpcStatus(Status status);
    }

    private final long defaultReadTimeoutMillis;
    
    protected BaseGrpcTrasnsport(long readTimeoutMillis) {
        this.defaultReadTimeoutMillis = readTimeoutMillis;
    }
    
    long getDefaultReadTimeoutMillis() {
        return this.defaultReadTimeoutMillis;
    } 
    
    protected abstract CallOptions getCallOptions();
    protected abstract CheckableChannel getChannel(GrpcRequestSettings settings);

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
        } else if (defaultReadTimeoutMillis > 0) {
            options = options.withDeadlineAfter(defaultReadTimeoutMillis, TimeUnit.MILLISECONDS);
        }

        CompletableFuture<Result<RespT>> promise = new CompletableFuture<>();
        try {
            CheckableChannel channel = getChannel(settings);
            ClientCall<ReqT, RespT> call = channel.grpcChannel().newCall(method, options);
            if (logger.isTraceEnabled()) {
                logger.trace("Sending request to {}, method `{}', request: `{}'", 
                        channel.endpoint(),
                        method,
                        request);
            }
            sendOneRequest(call, request, settings, new UnaryStreamToFuture<>(
                    promise, settings.getTrailersHandler(), channel::updateGrpcStatus
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
                return () -> {};
            }
            options = options.withDeadlineAfter(settings.getDeadlineAfter() - now, TimeUnit.NANOSECONDS);
        } else if (defaultReadTimeoutMillis > 0) {
            options = options.withDeadlineAfter(defaultReadTimeoutMillis, TimeUnit.MILLISECONDS);
        }

        try {
            CheckableChannel channel = getChannel(settings);
            ClientCall<ReqT, RespT> call = channel.grpcChannel().newCall(method, options);
            if (logger.isTraceEnabled()) {
                logger.trace("Sending stream call to {}, method `{}', request: `{}'",
                        channel.endpoint(),
                        method,
                        request);
            }
            sendOneRequest(call, request, settings, new ServerStreamToObserver<>(
                    observer, call, settings.getTrailersHandler(), channel::updateGrpcStatus
            ));

            return () -> {
                call.cancel("Cancelled on user request", new CancellationException());
            };
        } catch (RuntimeException ex) {
            logger.error("server stream call problem {}", ex.getMessage());
            Issue issue = Issue.of(ex.getMessage(), Issue.Severity.ERROR);
            observer.onError(tech.ydb.core.Status.of(StatusCode.CLIENT_INTERNAL_ERROR, null, issue));
            return () -> {};
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
                logger.error
                        ("Exception encountered while closing the call", ex);
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
