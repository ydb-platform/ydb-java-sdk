package tech.ydb.core.grpc.impl;

import com.google.common.util.concurrent.MoreExecutors;
import io.grpc.CallOptions;
import io.grpc.Channel;
import io.grpc.ClientCall;
import io.grpc.Metadata;
import io.grpc.MethodDescriptor;
import io.grpc.Status;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.ydb.core.Issue;
import tech.ydb.core.Result;
import tech.ydb.core.StatusCode;
import tech.ydb.core.auth.AuthProvider;
import tech.ydb.core.auth.NopAuthProvider;
import tech.ydb.core.grpc.GrpcRequestSettings;
import tech.ydb.core.grpc.GrpcStatuses;
import tech.ydb.core.grpc.GrpcTransport;
import tech.ydb.core.grpc.ServerStreamToObserver;
import tech.ydb.core.grpc.UnaryStreamToFuture;
import tech.ydb.core.grpc.YdbCallCredentials;
import tech.ydb.core.rpc.StreamControl;
import tech.ydb.core.rpc.StreamObserver;

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

    private final CallOptions callOptions;
    private final long defaultReadTimeoutMillis;
    
    protected BaseGrpcTrasnsport(AuthProvider authProvider, Executor executor, long readTimeoutMillis) {
        this.callOptions = createCallOptions(authProvider, executor);
        this.defaultReadTimeoutMillis = readTimeoutMillis;
    }
    
    private static CallOptions createCallOptions(AuthProvider authProvider, Executor executor) {
        CallOptions callOptions = CallOptions.DEFAULT;
        if (authProvider != null && authProvider != NopAuthProvider.INSTANCE) {
            callOptions = callOptions.withCallCredentials(new YdbCallCredentials(authProvider));
        }
        if (executor != null && executor != MoreExecutors.directExecutor()) {
            callOptions = callOptions.withExecutor(executor);
        }
        return callOptions;
    }
    
    protected abstract CheckableChannel getChannel(GrpcRequestSettings settings);

    @Override
    public <ReqT, RespT> CompletableFuture<Result<RespT>> unaryCall(
            MethodDescriptor<ReqT, RespT> method,
            ReqT request,
            GrpcRequestSettings settings) {
        CallOptions options = this.callOptions;
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
        return makeUnaryCall(method, request, options, settings, promise);
    }

    @Override
    public <ReqT, RespT> StreamControl serverStreamCall(
            MethodDescriptor<ReqT, RespT> method,
            ReqT request,
            StreamObserver<RespT> observer,
            GrpcRequestSettings settings) {
        CallOptions options = this.callOptions;
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

        return makeServerStreamCall(method, request, options, settings, observer);
    }
    
    private <ReqT, RespT> CompletableFuture<Result<RespT>> makeUnaryCall(
            MethodDescriptor<ReqT, RespT> method,
            ReqT request,
            CallOptions callOptions,
            GrpcRequestSettings settings,
            CompletableFuture<Result<RespT>> promise) {
        CheckableChannel channel = getChannel(settings);
        ClientCall<ReqT, RespT> call = channel.grpcChannel().newCall(method, callOptions);
        if (logger.isDebugEnabled()) {
            logger.debug("Sending request to {}, method `{}', request: `{}'", 
                    channel.endpoint(),
                    method,
                    request);
        }
        sendOneRequest(call, request, settings, new UnaryStreamToFuture<>(
                promise, settings.getTrailersHandler(), channel::updateGrpcStatus
        ));
        return promise;
    }

    private <ReqT, RespT> StreamControl makeServerStreamCall(
            MethodDescriptor<ReqT, RespT> method,
            ReqT request,
            CallOptions callOptions,
            GrpcRequestSettings settings,
            StreamObserver<RespT> observer) {
        CheckableChannel channel = getChannel(settings);
        ClientCall<ReqT, RespT> call = channel.grpcChannel().newCall(method, callOptions);
        if (logger.isDebugEnabled()) {
            logger.debug("Sending stream call to {}, method `{}', request: `{}'",
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
        return Result.fail(StatusCode.CLIENT_DEADLINE_EXPIRED, Issue.of(message, Issue.Severity.ERROR));
    }

    private static Status deadlineExpiredStatus(MethodDescriptor<?, ?> method) {
        String message = "deadline expired before calling method " + method.getFullMethodName();
        return Status.DEADLINE_EXCEEDED.withDescription(message);
    }

}
