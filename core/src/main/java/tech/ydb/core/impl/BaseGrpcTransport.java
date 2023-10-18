package tech.ydb.core.impl;

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
import tech.ydb.core.impl.call.EmptyStream;
import tech.ydb.core.impl.call.GrpcStatusHandler;
import tech.ydb.core.impl.call.ReadStreamCall;
import tech.ydb.core.impl.call.ReadWriteStreamCall;
import tech.ydb.core.impl.call.UnaryCall;
import tech.ydb.core.impl.pool.GrpcChannel;
import tech.ydb.core.utils.Async;

/**
 *
 * @author Aleksandr Gorshenin
 */
public abstract class BaseGrpcTransport implements GrpcTransport {
    private static final Logger logger = LoggerFactory.getLogger(GrpcTransport.class);

    private static final Result<?> SHUTDOWN_RESULT =  Result.fail(Status
            .of(StatusCode.CLIENT_CANCELLED)
            .withIssues(Issue.of("Request was not sent: transport is shutting down", Issue.Severity.ERROR)
    ));

    protected volatile boolean shutdown;

    abstract AuthCallOptions getAuthCallOptions();
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

        CallOptions options = getAuthCallOptions().getGrpcCallOptions();
        if (settings.getDeadlineAfter() != 0) {
            final long now = System.nanoTime();
            if (now >= settings.getDeadlineAfter()) {
                return CompletableFuture.completedFuture(deadlineExpiredResult(method));
            }
            options = options.withDeadlineAfter(settings.getDeadlineAfter() - now, TimeUnit.NANOSECONDS);
        }

        try {
            GrpcChannel channel = getChannel(settings);
            ClientCall<ReqT, RespT> call = channel.getReadyChannel().newCall(method, options);
            ChannelStatusHandler handler = new ChannelStatusHandler(channel, settings);

            if (logger.isTraceEnabled()) {
                logger.trace("Sending request to {}, method `{}', request: `{}'",
                        channel.getEndpoint(),
                        method,
                        request);
            }

            return new UnaryCall<>(call, handler).startCall(request, settings.getExtraHeaders());
        } catch (RuntimeException ex) {
            logger.error("unary call problem {}", ex.getMessage());
            return Async.failedFuture(ex);
        }
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
            ChannelStatusHandler handler = new ChannelStatusHandler(channel, settings);

            if (logger.isTraceEnabled()) {
                logger.trace("Creating stream call to {}, method `{}', request: `{}'",
                        channel.getEndpoint(),
                        method,
                        request);
            }

            return new ReadStreamCall<>(call, request, settings.getExtraHeaders(), handler);
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
            ChannelStatusHandler handler = new ChannelStatusHandler(channel, settings);

            if (logger.isTraceEnabled()) {
                logger.trace("Creating bidirectional stream call to {}, method `{}'",
                        channel.getEndpoint(),
                        method);
            }

            return new ReadWriteStreamCall<>(call, settings.getExtraHeaders(), getAuthCallOptions(), handler);
        } catch (RuntimeException ex) {
            logger.error("server bidirectional stream call problem {}", ex.getMessage());
            Issue issue = Issue.of(ex.getMessage(), Issue.Severity.ERROR);
            return new EmptyStream<>(Status.of(StatusCode.CLIENT_INTERNAL_ERROR, null, issue));
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

    private class ChannelStatusHandler implements GrpcStatusHandler {
        private final GrpcChannel channel;
        private final GrpcRequestSettings settings;

        ChannelStatusHandler(GrpcChannel channel, GrpcRequestSettings settings) {
            this.channel = channel;
            this.settings = settings;
        }

        @Override
        public void accept(io.grpc.Status status, Metadata trailers) {
            updateChannelStatus(channel, status);
            if (settings.getTrailersHandler() != null && trailers != null) {
                settings.getTrailersHandler().accept(trailers);
            }
        }
    }
}
