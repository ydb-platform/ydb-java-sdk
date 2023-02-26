package tech.ydb.core.impl;

import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import io.grpc.CallOptions;
import io.grpc.ClientCall;
import io.grpc.Metadata;
import io.grpc.MethodDescriptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tech.ydb.core.Issue;
import tech.ydb.core.Result;
import tech.ydb.core.Status;
import tech.ydb.core.StatusCode;
import tech.ydb.core.grpc.AsyncBidiStreamingInAdapter;
import tech.ydb.core.grpc.AsyncBidiStreamingOutAdapter;
import tech.ydb.core.grpc.GrpcRequestSettings;
import tech.ydb.core.grpc.GrpcStatuses;
import tech.ydb.core.grpc.GrpcTransport;
import tech.ydb.core.impl.pool.GrpcChannel;
import tech.ydb.core.grpc.ServerStreamToObserver;
import tech.ydb.core.grpc.UnaryStreamToFuture;
import tech.ydb.core.rpc.OutStreamObserver;
import tech.ydb.core.rpc.StreamControl;
import tech.ydb.core.rpc.StreamObserver;

/**
 *
 * @author Aleksandr Gorshenin
 */
public abstract class BaseGrpcTrasnsport implements GrpcTransport {
    private static final Logger logger = LoggerFactory.getLogger(GrpcTransport.class);

    private static final Result<?> SHUTDOWN_RESULT =  Result.fail(Status
            .of(StatusCode.CLIENT_CANCELLED)
            .withIssues(Issue.of("Request was not sent: transport is shutting down", Issue.Severity.ERROR)
    ));

    private volatile boolean shutdown = false;

    public abstract CallOptions getCallOptions();
    abstract GrpcChannel getChannel(GrpcRequestSettings settings);
    abstract void updateChannelStatus(GrpcChannel channel, io.grpc.Status status);

    @Override
    public void close() {
        this.shutdown = true;
    }

    @Override
    public <ReqT, RespT> CompletableFuture<Result<RespT>> unaryCall(
            MethodDescriptor<ReqT, RespT> method,
            GrpcRequestSettings settings,
            ReqT request
    ) {
        if (shutdown) {
            return CompletableFuture.completedFuture(SHUTDOWN_RESULT.map(null));
        }

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
        if (shutdown) {
            observer.onError(SHUTDOWN_RESULT.getStatus());
            return () -> { };
        }

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
            observer.onError(Status.of(StatusCode.CLIENT_INTERNAL_ERROR, null, issue));
            return () -> { };
        }
    }

    protected <ReqT> OutStreamObserver<ReqT> makeEmptyObserverStub() {
        return new OutStreamObserver<ReqT>() {
            @Override
            public void onNext(ReqT value) {
            }

            @Override
            public void onError(Throwable t) {
            }

            @Override
            public void onCompleted() {
            }
        };
    }

    @Override
    public <ReqT, RespT> OutStreamObserver<ReqT> bidirectionalStreamCall(
            MethodDescriptor<ReqT, RespT> method,
            StreamObserver<RespT> observer,
            GrpcRequestSettings settings) {
        CallOptions options = getCallOptions();
        if (settings.getDeadlineAfter() > 0) {
            final long now = System.nanoTime();
            if (now >= settings.getDeadlineAfter()) {
                observer.onError(GrpcStatuses.toStatus(deadlineExpiredStatus(method)));
                return makeEmptyObserverStub();
            }
            options = options.withDeadlineAfter(settings.getDeadlineAfter() - now, TimeUnit.NANOSECONDS);
        } else if (defaultReadTimeoutMillis > 0) {
            options = options.withDeadlineAfter(defaultReadTimeoutMillis, TimeUnit.MILLISECONDS);
        }

        try {
            CheckableChannel channel = getChannel(settings);
            ClientCall<ReqT, RespT> call = channel.grpcChannel().newCall(method, options);
            if (logger.isTraceEnabled()) {
                logger.trace("Starting bidirectional stream to {}, method `{}'",
                        channel.endpoint(),
                        method);
            }
            AsyncBidiStreamingOutAdapter<ReqT, RespT> adapter
                    = new AsyncBidiStreamingOutAdapter<>(call);
            AsyncBidiStreamingInAdapter<ReqT, RespT> responseListener = new AsyncBidiStreamingInAdapter<>(
                    observer,
                    adapter,
                    channel::updateGrpcStatus,
                    settings.getTrailersHandler()
            );
            Metadata extra = settings.getExtraHeaders();
            call.start(responseListener, extra != null ? extra : new Metadata());
            responseListener.onStart();
            return adapter;
        } catch (RuntimeException ex) {
            logger.error("server bidirectional stream call problem {}", ex.getMessage());
            Issue issue = Issue.of(ex.getMessage(), Issue.Severity.ERROR);
            observer.onError(tech.ydb.core.Status.of(StatusCode.CLIENT_INTERNAL_ERROR, null, issue));
            return makeEmptyObserverStub();
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
            listener.onClose(io.grpc.Status.INTERNAL.withCause(t), null);
        }
    }

    private static <T> Result<T> deadlineExpiredResult(MethodDescriptor<?, T> method) {
        String message = "deadline expired before calling method " + method.getFullMethodName();
        return Result.fail(Status.of(
                StatusCode.CLIENT_DEADLINE_EXPIRED, null, Issue.of(message, Issue.Severity.ERROR)
        ));
    }

    private static io.grpc.Status deadlineExpiredStatus(MethodDescriptor<?, ?> method) {
        String message = "deadline expired before calling method " + method.getFullMethodName();
        return io.grpc.Status.DEADLINE_EXCEEDED.withDescription(message);
    }

}
