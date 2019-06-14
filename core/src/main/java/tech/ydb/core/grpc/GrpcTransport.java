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
import io.grpc.util.RoundRobinLoadBalancerFactory;
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

    GrpcTransport(GrpcTransportBuilder builder) {
        if (builder.endpoint != null) {
            this.realChannel = createChannel(builder.endpoint, builder.database, builder.authProvider);

            Metadata extraHeaders = new Metadata();
            extraHeaders.put(YdbHeaders.DATABASE, builder.database);
            ClientInterceptor interceptor = MetadataUtils.newAttachHeadersInterceptor(extraHeaders);
            this.channel = ClientInterceptors.intercept(realChannel, interceptor);
        } else if (builder.hosts.size() == 1) {
            this.channel = this.realChannel = createChannel(builder.hosts.get(0));
        } else {
            this.channel = this.realChannel = createChannel(builder.hosts);
        }

        if (builder.authProvider != NopAuthProvider.INSTANCE) {
            this.callOptions = builder.callOptions
                .withCallCredentials(new YdbCallCredentials(builder.authProvider));
        } else {
            this.callOptions = builder.callOptions;
        }

        this.operationTray = new GrpcOperationTray(this);
    }

    private static ManagedChannel createChannel(HostAndPort host) {
        int port = host.getPortOrDefault(DEFAULT_PORT);
        return NettyChannelBuilder.forAddress(new InetSocketAddress(host.getHost(), port))
            .negotiationType(NegotiationType.PLAINTEXT)
            .maxInboundMessageSize(64 << 20) // 64 MiB
            .withOption(ChannelOption.ALLOCATOR, ByteBufAllocator.DEFAULT)
            .build();
    }

    private static ManagedChannel createChannel(List<HostAndPort> hosts) {
        return NettyChannelBuilder.forTarget(HostsNameResolver.makeTarget(hosts))
            .negotiationType(NegotiationType.PLAINTEXT)
            .maxInboundMessageSize(64 << 20) // 64 MiB
            .withOption(ChannelOption.ALLOCATOR, ByteBufAllocator.DEFAULT)
            .nameResolverFactory(HostsNameResolver.newFactory(hosts))
            .loadBalancerFactory(RoundRobinLoadBalancerFactory.getInstance())
            .build();
    }

    private static ManagedChannel createChannel(String endpoint, String database, AuthProvider authProvider) {
        return NettyChannelBuilder.forTarget(YdbNameResolver.makeTarget(endpoint, database))
            .negotiationType(NegotiationType.PLAINTEXT)
            .maxInboundMessageSize(64 << 20) // 64 MiB
            .withOption(ChannelOption.ALLOCATOR, ByteBufAllocator.DEFAULT)
            .nameResolverFactory(YdbNameResolver.newFactory(authProvider))
            .loadBalancerFactory(RoundRobinLoadBalancerFactory.getInstance())
            .build();
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
