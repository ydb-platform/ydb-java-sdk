package tech.ydb.core.impl;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BooleanSupplier;

import com.google.common.net.HostAndPort;
import io.grpc.CallOptions;
import io.grpc.ClientCall;
import io.grpc.Context;
import io.grpc.Deadline;
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
import tech.ydb.core.grpc.GrpcTransport;
import tech.ydb.core.grpc.GrpcTransportBuilder;
import tech.ydb.core.grpc.YdbHeaders;
import tech.ydb.core.impl.auth.AuthCallOptions;
import tech.ydb.core.impl.call.EmptyStream;
import tech.ydb.core.impl.call.GrpcStatusHandler;
import tech.ydb.core.impl.call.ReadStreamCall;
import tech.ydb.core.impl.call.ReadWriteStreamCall;
import tech.ydb.core.impl.call.UnaryCall;
import tech.ydb.core.impl.pool.EndpointRecord;
import tech.ydb.core.impl.pool.GrpcChannel;
import tech.ydb.core.tracing.Span;

/**
 *
 * @author Aleksandr Gorshenin
 */
public abstract class BaseGrpcTransport implements GrpcTransport {
    private static final Logger logger = LoggerFactory.getLogger(GrpcTransport.class);

    private static final Result<?> SHUTDOWN_RESULT = Result.fail(Status
            .of(StatusCode.CLIENT_CANCELLED)
            .withIssues(Issue.of("Request was not sent: transport is shutting down", Issue.Severity.ERROR)
            ));

    protected static final int DEFAULT_PORT = 2135;

    private final AtomicBoolean isClosed = new AtomicBoolean(false);

    protected final EndpointRecord serverEndpoint;

    protected BaseGrpcTransport(GrpcTransportBuilder builder) {
        this.serverEndpoint = getDiscoveryEndpoint(builder);
    }

    protected BaseGrpcTransport(EndpointRecord serverEndpoint) {
        this.serverEndpoint = serverEndpoint;
    }

    protected abstract AuthCallOptions getAuthCallOptions();

    protected abstract GrpcChannel getChannel(GrpcRequestSettings settings);

    protected void pessimizeEndpoint(EndpointRecord endpoint, String reason) {
        // nothing to pessimize
    }

    protected void shutdown() {
        // nothing to shutdown
    }

    @Override
    public void close() {
        if (isClosed.compareAndSet(false, true)) {
            shutdown();
        }
    }

    private CallOptions prepareCallOptions(GrpcRequestSettings settings) {
        CallOptions options = getAuthCallOptions().getGrpcCallOptions();
        if (settings.getDeadlineAfter() != 0) {
            final long now = System.nanoTime();
            if (now >= settings.getDeadlineAfter()) {
                return null; // DEADLINE
            }
            options = options.withDeadlineAfter(settings.getDeadlineAfter() - now, TimeUnit.NANOSECONDS);
        }
        if (settings.isDeadlineDisabled()) {
            options = options.withDeadline(null);
        }

        Deadline deadline = Context.current().getDeadline();
        if (deadline != null && deadline.isExpired()) {
            return null; // DEADLINE
        }

        return options;
    }

    @Override
    public <ReqT, RespT> CompletableFuture<Result<RespT>> unaryCall(
            MethodDescriptor<ReqT, RespT> method,
            GrpcRequestSettings settings,
            ReqT request
    ) {
        if (isClosed.get()) {
            return CompletableFuture.completedFuture(SHUTDOWN_RESULT.map(o -> null));
        }

        String traceId = settings.getTraceId();
        try {
            GrpcChannel channel = getChannel(settings);
            EndpointRecord endpoint = channel.getEndpoint();
            CallOptions options = prepareCallOptions(settings);
            if (options == null) {
                return CompletableFuture.completedFuture(deadlineExpiredResult(method, settings));
            }

            ClientCall<ReqT, RespT> call = channel.getReadyChannel().newCall(method, options);
            ChannelStatusHandler handler = new ChannelStatusHandler(channel, settings);

            if (logger.isTraceEnabled()) {
                logger.trace("UnaryCall[{}] with method {} and endpoint {} created",
                        traceId, method.getFullMethodName(), endpoint.getHostAndPort());
            }
            Metadata metadata = makeMetadataFromSettings(settings, endpoint);
            return new UnaryCall<>(traceId, endpoint.getHostAndPort(), call, handler).startCall(request, metadata);
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
        try {
            GrpcChannel channel = getChannel(settings);
            EndpointRecord endpoint = channel.getEndpoint();
            CallOptions options = prepareCallOptions(settings);
            if (options == null) {
                return new EmptyStream<>(deadlineExpiredStatus(method, settings));
            }

            ClientCall<ReqT, RespT> call = channel.getReadyChannel().newCall(method, options);
            ChannelStatusHandler handler = new ChannelStatusHandler(channel, settings);

            if (logger.isTraceEnabled()) {
                logger.trace("ReadStreamCall[{}] with method {} and endpoint {} created",
                        traceId, method.getFullMethodName(), endpoint.getHostAndPort()
                );
            }

            Metadata metadata = makeMetadataFromSettings(settings, endpoint);
            GrpcFlowControl flowCtrl = settings.getFlowControl();
            return new ReadStreamCall<>(traceId, endpoint.getHostAndPort(), call, flowCtrl, request, metadata, handler);
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
        try {
            GrpcChannel channel = getChannel(settings);
            EndpointRecord endpoint = channel.getEndpoint();
            CallOptions options = prepareCallOptions(settings);
            if (options == null) {
                return new EmptyStream<>(deadlineExpiredStatus(method, settings));
            }

            ClientCall<ReqT, RespT> call = channel.getReadyChannel().newCall(method, options);
            ChannelStatusHandler hdlr = new ChannelStatusHandler(channel, settings);

            if (logger.isTraceEnabled()) {
                logger.trace("ReadWriteStreamCall[{}] with method {} and endpoint {} created",
                        traceId, method.getFullMethodName(), endpoint.getHostAndPort()
                );
            }

            Metadata metadata = makeMetadataFromSettings(settings, endpoint);
            GrpcFlowControl flowCtrl = settings.getFlowControl();
            return new ReadWriteStreamCall<>(
                    traceId, endpoint.getHostAndPort(), call, flowCtrl, metadata, getAuthCallOptions(), hdlr
            );
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

    private static Status deadlineExpiredStatus(MethodDescriptor<?, ?> method, GrpcRequestSettings settings) {
        String message = "deadline expired before calling method " + method.getFullMethodName() + " with traceId " +
                settings.getTraceId();
        return Status.of(StatusCode.CLIENT_DEADLINE_EXPIRED, Issue.of(message, Issue.Severity.ERROR));
    }

    private Metadata makeMetadataFromSettings(GrpcRequestSettings settings, EndpointRecord endpoint) {
        Metadata metadata = new Metadata();
        String token = getAuthCallOptions().getToken();
        if (token != null) {
            metadata.put(YdbHeaders.AUTH_TICKET, token);
        }
        if (settings.getTraceId() != null) {
            metadata.put(YdbHeaders.TRACE_ID, settings.getTraceId());
        }
        if (settings.getClientCapabilities() != null) {
            settings.getClientCapabilities().forEach(name -> metadata.put(YdbHeaders.YDB_CLIENT_CAPABILITIES, name));
        }

        Span span = settings.getSpan();
        if (span != null) {
            span.setAttribute("db.system.name", "ydb");
            span.setAttribute("db.namespace", getDatabase());
            span.setAttribute("server.address", serverEndpoint.getHost());
            span.setAttribute("server.port", serverEndpoint.getPort());
            span.setAttribute("network.peer.address", endpoint.getHost());
            span.setAttribute("network.peer.port", endpoint.getPort());
            span.setAttribute("ydb.node.id", endpoint.getNodeId());
            span.setAttribute("ydb.node.dc", endpoint.getLocation());

            metadata.put(YdbHeaders.TRACEPARENT, span.getId());
        }

        return metadata;
    }

    private static EndpointRecord getDiscoveryEndpoint(GrpcTransportBuilder builder) {
        URI endpointURI = null;
        try {
            String endpoint = builder.getEndpoint();
            if (endpoint != null) {
                if (endpoint.startsWith("grpc://") || endpoint.startsWith("grpcs://")) {
                    endpointURI = new URI(endpoint);
                } else {
                    endpointURI = new URI(null, endpoint, null, null, null);
                }
            }
            HostAndPort host = builder.getHost();
            if (host != null) {
                endpointURI = new URI(null, null, host.getHost(),
                        host.getPortOrDefault(DEFAULT_PORT), null, null, null);
            }
        } catch (URISyntaxException ex) {
            logger.warn("endpoint parse problem", ex);
        }
        if (endpointURI == null) {
            throw new IllegalArgumentException("Can't create discovery rpc, unreadable "
                    + "endpoint " + builder.getEndpoint() + " and empty host " + builder.getHost());
        }

        if (endpointURI.getPort() < 0) {
            throw new IllegalArgumentException("Can't create discovery rpc, port is not specified for "
                    + "endpoint " + builder.getEndpoint());
        }

        return new EndpointRecord(endpointURI.getHost(), endpointURI.getPort());
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
            // Usually CANCELLED is received when ClientCall is canceled on client side
            if (!status.isOk() && status.getCode() != io.grpc.Status.Code.CANCELLED
                    && status.getCode() != io.grpc.Status.Code.DEADLINE_EXCEEDED
                    && status.getCode() != io.grpc.Status.Code.RESOURCE_EXHAUSTED) {
                pessimizeEndpoint(channel.getEndpoint(), "by grpc code " + status.getCode());
            }

            if (settings.getTrailersHandler() != null && trailers != null) {
                settings.getTrailersHandler().accept(trailers);
            }
        }

        @Override
        public void postComplete() {
            BooleanSupplier hook = settings.getPessimizationHook();
            if (hook != null && hook.getAsBoolean()) {
                pessimizeEndpoint(channel.getEndpoint(), "by pessimization hook");
            }
        }
    }
}
