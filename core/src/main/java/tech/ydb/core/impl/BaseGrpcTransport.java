package tech.ydb.core.impl;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

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
import tech.ydb.core.UnexpectedResultException;
import tech.ydb.core.grpc.GrpcFlowControl;
import tech.ydb.core.grpc.GrpcReadStream;
import tech.ydb.core.grpc.GrpcReadWriteStream;
import tech.ydb.core.grpc.GrpcRequestSettings;
import tech.ydb.core.grpc.GrpcStatuses;
import tech.ydb.core.grpc.GrpcTransport;
import tech.ydb.core.grpc.YdbHeaders;
import tech.ydb.core.impl.auth.AuthCallOptions;
import tech.ydb.core.impl.call.EmptyStream;
import tech.ydb.core.impl.call.GrpcStatusHandler;
import tech.ydb.core.impl.call.ReadStreamCall;
import tech.ydb.core.impl.call.ReadWriteStreamCall;
import tech.ydb.core.impl.call.UnaryCall;
import tech.ydb.core.impl.pool.GrpcChannel;

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

    private final AtomicBoolean isClosed = new AtomicBoolean(false);

    protected abstract AuthCallOptions getAuthCallOptions();
    protected abstract GrpcChannel getChannel(GrpcRequestSettings settings);
    protected abstract void updateChannelStatus(GrpcChannel channel, io.grpc.Status status);

    protected void shutdown() {
        // nothing to shutdown
    }

    @Override
    public void close() {
        if (isClosed.compareAndSet(false, true)) {
            shutdown();
        }
    }

    @Override
    public <ReqT, RespT> CompletableFuture<Result<RespT>> unaryCall(
            MethodDescriptor<ReqT, RespT> method,
            GrpcRequestSettings settings,
            ReqT request
    ) {
        if (isClosed.get()) {
            return CompletableFuture.completedFuture(SHUTDOWN_RESULT.map(null));
        }

        String traceId = settings.getTraceId();
        CallOptions options = getAuthCallOptions().getGrpcCallOptions();
        if (settings.getDeadlineAfter() != 0) {
            final long now = System.nanoTime();
            if (now >= settings.getDeadlineAfter()) {
                return CompletableFuture.completedFuture(deadlineExpiredResult(method, settings));
            }
            options = options.withDeadlineAfter(settings.getDeadlineAfter() - now, TimeUnit.NANOSECONDS);
        }

        try {
            GrpcChannel channel = getChannel(settings);
            ClientCall<ReqT, RespT> call = channel.getReadyChannel().newCall(method, options);
            ChannelStatusHandler handler = new ChannelStatusHandler(channel, settings);

            if (logger.isTraceEnabled()) {
                logger.trace("UnaryCall[{}] with method {} and endpoint {} created",
                        traceId, method.getFullMethodName(), channel.getEndpoint().getHostAndPort()
                );
            }

            return new UnaryCall<>(traceId, call, handler).startCall(request, makeMetadataFromSettings(settings));
        } catch (UnexpectedResultException ex) {
            logger.warn("UnaryCall[{}] got unexpected status {}", traceId, ex.getStatus());
            return CompletableFuture.completedFuture(Result.fail(ex));
        } catch (RuntimeException ex) {
            String message = ex.getMessage() != null ? ex.getMessage() : ex.toString();
            logger.warn("UnaryCall[{}] got problem {}", traceId, message);
            return CompletableFuture.completedFuture(Result.error(message, ex));
        }
    }

    @Override
    public <ReqT, RespT> GrpcReadStream<RespT> readStreamCall(
            MethodDescriptor<ReqT, RespT> method,
            GrpcRequestSettings settings,
            ReqT request
        ) {
        if (isClosed.get()) {
            return new EmptyStream<>(SHUTDOWN_RESULT.getStatus());
        }

        String traceId = settings.getTraceId();
        CallOptions options = getAuthCallOptions().getGrpcCallOptions();
        if (settings.getDeadlineAfter() != 0) {
            final long now = System.nanoTime();
            if (now >= settings.getDeadlineAfter()) {
                return new EmptyStream<>(GrpcStatuses.toStatus(deadlineExpiredStatus(method, settings)));
            }
            options = options.withDeadlineAfter(settings.getDeadlineAfter() - now, TimeUnit.NANOSECONDS);
        }

        try {
            GrpcChannel channel = getChannel(settings);
            ClientCall<ReqT, RespT> call = channel.getReadyChannel().newCall(method, options);
            ChannelStatusHandler handler = new ChannelStatusHandler(channel, settings);

            if (logger.isTraceEnabled()) {
                logger.trace("ReadStreamCall[{}] with method {} and endpoint {} created",
                        traceId, method.getFullMethodName(), channel.getEndpoint().getHostAndPort()
                );
            }

            Metadata metadata = makeMetadataFromSettings(settings);
            GrpcFlowControl flowCtrl = settings.getFlowControl();
            return new ReadStreamCall<>(traceId, call, flowCtrl, request, metadata, handler);
        } catch (UnexpectedResultException ex) {
            logger.warn("ReadStreamCall[{}] got unexpected status {}", traceId, ex.getStatus());
            return new EmptyStream<>(ex.getStatus());
        } catch (RuntimeException ex) {
            String message = ex.getMessage() != null ? ex.getMessage() : ex.toString();
            logger.warn("ReadStreamCall[{}] got problem {}", traceId, message);
            Issue issue = Issue.of(message, Issue.Severity.ERROR);
            return new EmptyStream<>(Status.of(StatusCode.CLIENT_INTERNAL_ERROR, ex, issue));
        }
    }


    @Override
    public <ReqT, RespT> GrpcReadWriteStream<RespT, ReqT> readWriteStreamCall(
            MethodDescriptor<ReqT, RespT> method,
            GrpcRequestSettings settings
        ) {
        if (isClosed.get()) {
            return new EmptyStream<>(SHUTDOWN_RESULT.getStatus());
        }

        String traceId = settings.getTraceId();
        CallOptions options = getAuthCallOptions().getGrpcCallOptions();
        if (settings.getDeadlineAfter() != 0) {
            final long now = System.nanoTime();
            if (now >= settings.getDeadlineAfter()) {
                return new EmptyStream<>(GrpcStatuses.toStatus(deadlineExpiredStatus(method, settings)));
            }
            options = options.withDeadlineAfter(settings.getDeadlineAfter() - now, TimeUnit.NANOSECONDS);
        }

        try {
            GrpcChannel channel = getChannel(settings);
            ClientCall<ReqT, RespT> call = channel.getReadyChannel().newCall(method, options);
            ChannelStatusHandler handler = new ChannelStatusHandler(channel, settings);

            if (logger.isTraceEnabled()) {
                logger.trace("ReadWriteStreamCall[{}] with method {} and endpoint {} created",
                        traceId, method.getFullMethodName(), channel.getEndpoint().getHostAndPort()
                );
            }

            Metadata metadata = makeMetadataFromSettings(settings);
            GrpcFlowControl flowCtrl = settings.getFlowControl();
            return new ReadWriteStreamCall<>(traceId, call, flowCtrl, metadata, getAuthCallOptions(), handler);
        } catch (UnexpectedResultException ex) {
            logger.warn("ReadWriteStreamCall[{}] got unexpected status {}", traceId, ex.getStatus());
            return new EmptyStream<>(ex.getStatus());
        } catch (RuntimeException ex) {
            String message = ex.getMessage() != null ? ex.getMessage() : ex.toString();
            logger.warn("ReadWriteStreamCall[{}] got problem {}", traceId, message);
            Issue issue = Issue.of(message, Issue.Severity.ERROR);
            return new EmptyStream<>(Status.of(StatusCode.CLIENT_INTERNAL_ERROR, ex, issue));
        }
    }

    private static <T> Result<T> deadlineExpiredResult(MethodDescriptor<?, T> method, GrpcRequestSettings settings) {
        String message = "deadline expired before calling method " + method.getFullMethodName() + " with traceId " +
                settings.getTraceId();
        return Result.fail(Status.of(StatusCode.CLIENT_DEADLINE_EXPIRED, Issue.of(message, Issue.Severity.ERROR)));
    }

    private static io.grpc.Status deadlineExpiredStatus(MethodDescriptor<?, ?> method, GrpcRequestSettings settings) {
        String message = "deadline expired before calling method " + method.getFullMethodName() + " with traceId " +
                settings.getTraceId();
        return io.grpc.Status.DEADLINE_EXCEEDED.withDescription(message);
    }

    private Metadata makeMetadataFromSettings(GrpcRequestSettings settings) {
        Metadata metadata = new Metadata();
        if (settings.getTraceId() != null) {
            metadata.put(YdbHeaders.TRACE_ID, settings.getTraceId());
        }
        if (settings.getClientCapabilities() != null) {
            settings.getClientCapabilities().forEach(name -> metadata.put(YdbHeaders.YDB_CLIENT_CAPABILITIES, name));
        }
        return metadata;
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
