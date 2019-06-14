package tech.ydb.core.grpc;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.common.net.HostAndPort;
import com.google.common.util.concurrent.MoreExecutors;
import tech.ydb.core.Issue;
import tech.ydb.core.Result;
import tech.ydb.core.StatusCode;
import tech.ydb.core.auth.AuthProvider;
import tech.ydb.core.auth.NopAuthProvider;
import tech.ydb.core.rpc.OperationTray;
import tech.ydb.core.rpc.RpcTransport;
import tech.ydb.core.rpc.StreamObserver;
import com.yandex.yql.proto.IssueSeverity.TSeverityIds.ESeverityId;
import io.grpc.CallOptions;
import io.grpc.Channel;
import io.grpc.ClientCall;
import io.grpc.ClientInterceptor;
import io.grpc.ClientInterceptors;
import io.grpc.ManagedChannel;
import io.grpc.Metadata;
import io.grpc.MethodDescriptor;
import io.grpc.Status;
import io.grpc.netty.NegotiationType;
import io.grpc.netty.NettyChannelBuilder;
import io.grpc.stub.MetadataUtils;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.ChannelOption;

import static java.util.concurrent.CompletableFuture.completedFuture;


/**
 * @author Sergey Polovko
 */
public class GrpcTransport implements RpcTransport {

    public static final int DEFAULT_PORT = 2135;

    private static final Logger logger = Logger.getLogger(GrpcTransport.class.getName());

    private final ManagedChannel realChannel;
    private final Channel channel;
    private final CallOptions callOptions;
    private final GrpcOperationTray operationTray;
    private final long defautlReadTimeoutMillis;

    GrpcTransport(GrpcTransportBuilder builder) {
        this.realChannel = createChannel(builder);
        this.channel = interceptChannel(realChannel, builder);
        this.callOptions = createCallOptions(builder);
        this.defautlReadTimeoutMillis = builder.getReadTimeoutMillis();
        this.operationTray = new GrpcOperationTray(this);
    }

    private static ManagedChannel createChannel(GrpcTransportBuilder builder) {
        String endpoint = builder.getEndpoint();
        String database = builder.getDatabase();
        List<HostAndPort> hosts = builder.getHosts();
        assert (endpoint != null) == (database != null);
        assert (endpoint == null) != (hosts == null);

        final NettyChannelBuilder channelBuilder;
        if (endpoint != null) {
            channelBuilder = NettyChannelBuilder.forTarget(YdbNameResolver.makeTarget(endpoint, database))
                    .nameResolverFactory(YdbNameResolver.newFactory(builder.getAuthProvider()))
                    .defaultLoadBalancingPolicy("round_robin");
        } else if (hosts.size() > 1) {
            channelBuilder = NettyChannelBuilder.forTarget(HostsNameResolver.makeTarget(hosts))
                .nameResolverFactory(HostsNameResolver.newFactory(hosts))
                .defaultLoadBalancingPolicy("round_robin");
        } else {
            channelBuilder = NettyChannelBuilder.forAddress(new InetSocketAddress(
                hosts.get(0).getHost(),
                hosts.get(0).getPortOrDefault(DEFAULT_PORT)));
        }

        channelBuilder
            .negotiationType(NegotiationType.PLAINTEXT)
            .maxInboundMessageSize(64 << 20) // 64 MiB
            .withOption(ChannelOption.ALLOCATOR, ByteBufAllocator.DEFAULT);

        builder.getChannelInitializer().accept(channelBuilder);
        return channelBuilder.build();
    }

    private static Channel interceptChannel(ManagedChannel realChannel, GrpcTransportBuilder builder) {
        if (builder.getDatabase() == null) {
            return realChannel;
        }

        Metadata extraHeaders = new Metadata();
        extraHeaders.put(YdbHeaders.DATABASE, builder.getDatabase());
        ClientInterceptor interceptor = MetadataUtils.newAttachHeadersInterceptor(extraHeaders);
        return ClientInterceptors.intercept(realChannel, interceptor);
    }

    private static CallOptions createCallOptions(GrpcTransportBuilder builder) {
        CallOptions callOptions = CallOptions.DEFAULT;
        AuthProvider authProvider = builder.getAuthProvider();
        if (authProvider != NopAuthProvider.INSTANCE) {
            callOptions = callOptions.withCallCredentials(new YdbCallCredentials(authProvider));
        }
        if (builder.getCallExecutor() != MoreExecutors.directExecutor()) {
            callOptions = callOptions.withExecutor(builder.getCallExecutor());
        }
        return callOptions;
    }

    public <ReqT, RespT> CompletableFuture<Result<RespT>> unaryCall(
        MethodDescriptor<ReqT, RespT> method,
        ReqT request,
        long deadlineAfter)
    {
        CallOptions callOptions = this.callOptions;
        if (deadlineAfter > 0) {
            final long now = System.nanoTime();
            if (now >= deadlineAfter) {
                return completedFuture(deadlineExpiredResult(method));
            }
            callOptions = this.callOptions.withDeadlineAfter(deadlineAfter - now, TimeUnit.NANOSECONDS);
        } else if (defautlReadTimeoutMillis > 0) {
            callOptions = this.callOptions.withDeadlineAfter(defautlReadTimeoutMillis, TimeUnit.MILLISECONDS);
        }

        CompletableFuture<Result<RespT>> promise = new CompletableFuture<>();
        ClientCall<ReqT, RespT> call = channel.newCall(method, callOptions);
        sendOneRequest(call, request, new UnaryStreamToFuture<>(promise));
        return promise;
    }

    public <ReqT, RespT> void unaryCall(
        MethodDescriptor<ReqT, RespT> method,
        ReqT request,
        Consumer<Result<RespT>> consumer,
        long deadlineAfter)
    {
        CallOptions callOptions = this.callOptions;
        if (deadlineAfter > 0) {
            final long now = System.nanoTime();
            if (now >= deadlineAfter) {
                consumer.accept(deadlineExpiredResult(method));
                return;
            }
            callOptions = this.callOptions.withDeadlineAfter(deadlineAfter - now, TimeUnit.NANOSECONDS);
        } else if (defautlReadTimeoutMillis > 0) {
            callOptions = this.callOptions.withDeadlineAfter(defautlReadTimeoutMillis, TimeUnit.MILLISECONDS);
        }

        ClientCall<ReqT, RespT> call = channel.newCall(method, callOptions);
        sendOneRequest(call, request, new UnaryStreamToConsumer<>(consumer));
    }

    public <ReqT, RespT> void unaryCall(
        MethodDescriptor<ReqT, RespT> method,
        ReqT request,
        BiConsumer<RespT, Status> consumer,
        long deadlineAfter)
    {
        CallOptions callOptions = this.callOptions;
        if (deadlineAfter > 0) {
            final long now = System.nanoTime();
            if (now >= deadlineAfter) {
                consumer.accept(null, deadlineExpiredStatus(method));
                return;
            }
            callOptions = this.callOptions.withDeadlineAfter(deadlineAfter - now, TimeUnit.NANOSECONDS);
        } else if (defautlReadTimeoutMillis > 0) {
            callOptions = this.callOptions.withDeadlineAfter(defautlReadTimeoutMillis, TimeUnit.MILLISECONDS);
        }

        ClientCall<ReqT, RespT> call = channel.newCall(method, callOptions);
        sendOneRequest(call, request, new UnaryStreamToBiConsumer<>(consumer));
    }

    public <ReqT, RespT> void serverStreamCall(
        MethodDescriptor<ReqT, RespT> method,
        ReqT request,
        StreamObserver<RespT> observer,
        long deadlineAfter)
    {
        CallOptions callOptions = this.callOptions;
        if (deadlineAfter > 0) {
            final long now = System.nanoTime();
            if (now >= deadlineAfter) {
                observer.onError(GrpcStatuses.toStatus(deadlineExpiredStatus(method)));
                return;
            }
            callOptions = this.callOptions.withDeadlineAfter(deadlineAfter - now, TimeUnit.NANOSECONDS);
        } else if (defautlReadTimeoutMillis > 0) {
            callOptions = this.callOptions.withDeadlineAfter(defautlReadTimeoutMillis, TimeUnit.MILLISECONDS);
        }

        ClientCall<ReqT, RespT> call = channel.newCall(method, callOptions);
        sendOneRequest(call, request, new ServerStreamToObserver<>(observer, call));
    }

    private static <ReqT, RespT> void sendOneRequest(
        ClientCall<ReqT, RespT> call,
        ReqT request,
        ClientCall.Listener<RespT> listener)
    {
        try {
            call.start(listener, new Metadata());
            call.request(1);
            call.sendMessage(request);
            call.halfClose();
        } catch (Throwable t) {
            try {
                call.cancel(null, t);
            } catch (Throwable ex) {
                logger.log(Level.SEVERE, "Exception encountered while closing the call", ex);
            }
            listener.onClose(Status.INTERNAL.withCause(t), null);
        }
    }

    @Override
    public OperationTray getOperationTray() {
        return operationTray;
    }

    @Override
    public void close() {
        operationTray.close();
        realChannel.shutdown();
    }

    private static <T> Result<T> deadlineExpiredResult(MethodDescriptor<?, T> method) {
        String message = "deadline expired before calling method " + method.getFullMethodName();
        return Result.fail(StatusCode.CLIENT_DEADLINE_EXCEEDED, Issue.of(message, ESeverityId.S_ERROR));
    }

    private static Status deadlineExpiredStatus(MethodDescriptor<?, ?> method) {
        String message = "deadline expired before calling method " + method.getFullMethodName();
        return Status.DEADLINE_EXCEEDED.withDescription(message);
    }
}
