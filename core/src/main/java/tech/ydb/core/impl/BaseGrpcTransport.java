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
import tech.ydb.core.grpc.GrpcReadStream;
import tech.ydb.core.grpc.GrpcReadWriteStream;
import tech.ydb.core.grpc.GrpcRequestSettings;
import tech.ydb.core.grpc.GrpcStatuses;
import tech.ydb.core.grpc.GrpcTransport;
import tech.ydb.core.impl.auth.AuthCallOptions;
import tech.ydb.core.impl.operation.OperationManager;
import tech.ydb.core.impl.pool.GrpcChannel;
import tech.ydb.core.impl.stream.EmptyStream;

/**
 * @author Aleksandr Gorshenin
 */
public abstract class BaseGrpcTransport implements GrpcTransport {
    private static final Logger logger = LoggerFactory.getLogger(GrpcTransport.class);

    private static final Result<?> SHUTDOWN_RESULT = Result.fail(Status
            .of(StatusCode.CLIENT_CANCELLED)
            .withIssues(Issue.of("Request was not sent: transport is shutting down", Issue.Severity.ERROR)
            ));

    private final OperationManager operationManager = new OperationManager(this);

    private volatile boolean shutdown = false;

    public abstract AuthCallOptions getAuthCallOptions();

    abstract GrpcChannel getChannel(GrpcRequestSettings settings);

    abstract void updateChannelStatus(GrpcChannel channel, io.grpc.Status status);

    @Override
    public OperationManager getOperationManager() {
        return operationManager;
    }

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

        CallOptions options = getAuthCallOptions().getGrpcCallOptions();
        if (settings.getDeadlineAfter() != 0) {
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
    public <ReqT, RespT> GrpcReadStream<RespT> readStreamCall(
            MethodDescriptor<ReqT, RespT> method,
            GrpcRequestSettings settings,
            ReqT request
    ) {
        if (shutdown) {
            return new EmptyStream<>(SHUTDOWN_RESULT.getStatus());
        }

        CallOptions options = getAuthCallOptions().getGrpcCallOptions();
        if (settings.getDeadlineAfter() != 0) {
            final long now = System.nanoTime();
            if (now >= settings.getDeadlineAfter()) {
                return new EmptyStream<>(GrpcStatuses.toStatus(deadlineExpiredStatus(method)));
            }
            options = options.withDeadlineAfter(settings.getDeadlineAfter() - now, TimeUnit.NANOSECONDS);
        }

        try {
            GrpcChannel channel = getChannel(settings);
            ClientCall<ReqT, RespT> call = channel.getReadyChannel().newCall(method, options);

            return new GrpcReadStream<RespT>() {
                @Override
                public CompletableFuture<Status> start(Observer<RespT> observer) {
                    if (logger.isTraceEnabled()) {
                        logger.trace("Sending stream call to {}, method `{}', request: `{}'",
                                channel.getEndpoint(),
                                method,
                                request);
                    }

                    CompletableFuture<Status> future = new CompletableFuture<>();
                    sendOneRequest(call, request, settings, new ServerStreamToObserver<>(
                            observer, future, call, settings.getTrailersHandler(),
                            status -> updateChannelStatus(channel, status)
                    ));
                    return future;
                }

                @Override
                public void cancel() {
                    call.cancel("Cancelled on user request", new CancellationException());
                }
            };
        } catch (RuntimeException ex) {
            logger.error("server stream call problem {}", ex.getMessage());
            Issue issue = Issue.of(ex.getMessage(), Issue.Severity.ERROR);
            return new EmptyStream<>(Status.of(StatusCode.CLIENT_INTERNAL_ERROR, null, issue));
        }
    }


    @Override
    public <ReqT, RespT> GrpcReadWriteStream<RespT, ReqT> readWriteStreamCall(
            MethodDescriptor<ReqT, RespT> method,
            GrpcRequestSettings settings
    ) {
        if (shutdown) {
            return new EmptyStream<>(SHUTDOWN_RESULT.getStatus());
        }

        CallOptions options = getAuthCallOptions().getGrpcCallOptions();
        if (settings.getDeadlineAfter() != 0) {
            final long now = System.nanoTime();
            if (now >= settings.getDeadlineAfter()) {
                return new EmptyStream<>(GrpcStatuses.toStatus(deadlineExpiredStatus(method)));
            }
            options = options.withDeadlineAfter(settings.getDeadlineAfter() - now, TimeUnit.NANOSECONDS);
        }

        try {
            GrpcChannel channel = getChannel(settings);
            ClientCall<ReqT, RespT> call = channel.getReadyChannel().newCall(method, options);

            return new GrpcReadWriteStream<RespT, ReqT>() {
                @Override
                public CompletableFuture<Status> start(Observer<RespT> observer) {
                    if (logger.isTraceEnabled()) {
                        logger.trace("Start bidirectional stream call to {}, method `{}'",
                                channel.getEndpoint(),
                                method);
                    }

                    CompletableFuture<Status> future = new CompletableFuture<>();

                    Metadata headers = settings.getExtraHeaders() != null ? settings.getExtraHeaders() : new Metadata();
                    call.start(new ServerStreamToObserver<>(
                            observer, future, call, settings.getTrailersHandler(),
                            status -> updateChannelStatus(channel, status)
                    ), headers);
                    call.request(1);

                    return future;
                }

                @Override
                public String authToken() {
                    return getAuthCallOptions().getToken();
                }

                @Override
                public void cancel() {
                    call.cancel("Cancelled on user request", new CancellationException());
                }

                @Override
                public void sendNext(ReqT message) {
                    call.sendMessage(message);
                }

                @Override
                public void close() {
                    call.halfClose();
                }
            };
        } catch (RuntimeException ex) {
            logger.error("server bidirectional stream call problem {}", ex.getMessage());
            Issue issue = Issue.of(ex.getMessage(), Issue.Severity.ERROR);
            return new EmptyStream<>(Status.of(StatusCode.CLIENT_INTERNAL_ERROR, null, issue));
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
