package tech.ydb.core.grpc;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import javax.net.ssl.SSLException;

import com.google.common.base.Strings;
import com.google.common.net.HostAndPort;
import com.google.common.util.concurrent.MoreExecutors;
import tech.ydb.core.Issue;
import tech.ydb.core.Result;
import tech.ydb.core.StatusCode;
import tech.ydb.core.auth.AuthProvider;
import tech.ydb.core.auth.NopAuthProvider;
import tech.ydb.core.rpc.OperationTray;
import tech.ydb.core.rpc.OutStreamObserver;
import tech.ydb.core.rpc.RpcTransport;
import tech.ydb.core.rpc.RpcTransportBuilder;
import tech.ydb.core.rpc.StreamControl;
import tech.ydb.core.rpc.StreamObserver;
import tech.ydb.core.ssl.YandexTrustManagerFactory;
import com.yandex.yql.proto.IssueSeverity.TSeverityIds.ESeverityId;
import io.grpc.CallOptions;
import io.grpc.Channel;
import io.grpc.ClientCall;
import io.grpc.ClientInterceptor;
import io.grpc.ClientInterceptors;
import io.grpc.LoadBalancer;
import io.grpc.LoadBalancerProvider;
import io.grpc.LoadBalancerRegistry;
import io.grpc.ManagedChannel;
import io.grpc.Metadata;
import io.grpc.MethodDescriptor;
import io.grpc.Status;
import io.grpc.netty.GrpcSslContexts;
import io.grpc.netty.NegotiationType;
import io.grpc.netty.NettyChannelBuilder;
import io.grpc.stub.MetadataUtils;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.ChannelOption;
import io.netty.handler.ssl.SslContext;
import io.netty.util.internal.StringUtil;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.Collections.singletonList;
import static java.util.concurrent.CompletableFuture.completedFuture;


/**
 * @author Sergey Polovko
 * @author Evgeniy Pshenitsin
 */
public class GrpcTransport implements RpcTransport {

    public static final int DEFAULT_PORT = 2135;

    private static final Logger logger = Logger.getLogger(GrpcTransport.class.getName());

    private final ManagedChannel realChannel;
    private final Channel channel;
    private final CallOptions callOptions;
    private final String database;
    private final GrpcOperationTray operationTray;
    private final long defautlReadTimeoutMillis;

    private GrpcTransport(Builder builder) {
        this.realChannel = createChannel(builder);
        this.channel = interceptChannel(realChannel, builder);
        this.callOptions = createCallOptions(builder);
        this.defautlReadTimeoutMillis = builder.getReadTimeoutMillis();
        this.database = Strings.nullToEmpty(builder.getDatabase());
        this.operationTray = new GrpcOperationTray(this);
    }

    public static Builder forHost(String host, int port) {
        return new Builder(null, null, singletonList(HostAndPort.fromParts(host, port)));
    }

    public static Builder forHosts(HostAndPort... hosts) {
        checkNotNull(hosts, "hosts is null");
        checkArgument(hosts.length > 0, "empty hosts array");
        return new Builder(null, null, Arrays.asList(hosts));
    }

    public static Builder forHosts(List<HostAndPort> hosts) {
        checkNotNull(hosts, "hosts is null");
        checkArgument(!hosts.isEmpty(), "empty hosts list");
        return new Builder(null, null, hosts);
    }

    public static Builder forEndpoint(String endpoint, String database) {
        checkNotNull(endpoint, "endpoint is null");
        checkNotNull(database, "database is null");
        return new Builder(endpoint, database, null);
    }

    public <ReqT, RespT> CompletableFuture<Result<RespT>> unaryCall(
            MethodDescriptor<ReqT, RespT> method,
            ReqT request,
            long deadlineAfter) {
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
            long deadlineAfter) {
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
            long deadlineAfter) {
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

    public <ReqT, RespT> StreamControl serverStreamCall(
            MethodDescriptor<ReqT, RespT> method,
            ReqT request,
            StreamObserver<RespT> observer,
            long deadlineAfter) {
        CallOptions callOptions = this.callOptions;
        if (deadlineAfter > 0) {
            final long now = System.nanoTime();
            if (now >= deadlineAfter) {
                observer.onError(GrpcStatuses.toStatus(deadlineExpiredStatus(method)));
                return () -> {};
            }
            callOptions = this.callOptions.withDeadlineAfter(deadlineAfter - now, TimeUnit.NANOSECONDS);
        } else if (defautlReadTimeoutMillis > 0) {
            callOptions = this.callOptions.withDeadlineAfter(defautlReadTimeoutMillis, TimeUnit.MILLISECONDS);
        }

        ClientCall<ReqT, RespT> call = channel.newCall(method, callOptions);
        sendOneRequest(call, request, new ServerStreamToObserver<>(observer, call));
        return () -> {
            call.cancel("Cancelled on user request", new CancellationException());
        };
    }

    public <ReqT, RespT> OutStreamObserver<ReqT> bidirectionalStreamCall(
            MethodDescriptor<ReqT, RespT> method,
            StreamObserver<RespT> observer,
            long deadlineAfter) {
        CallOptions callOptions = this.callOptions;
        if (deadlineAfter > 0) {
            final long now = System.nanoTime();
            if (now >= deadlineAfter) {
                observer.onError(GrpcStatuses.toStatus(deadlineExpiredStatus(method)));
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
            callOptions = this.callOptions.withDeadlineAfter(deadlineAfter - now, TimeUnit.NANOSECONDS);
        } else if (defautlReadTimeoutMillis > 0) {
            callOptions = this.callOptions.withDeadlineAfter(defautlReadTimeoutMillis, TimeUnit.MILLISECONDS);
        }
        ClientCall<ReqT, RespT> call = channel.newCall(method, callOptions);
        return asyncBidiStreamingCall(call, observer);
    }

    private static <ReqT, RespT> void sendOneRequest(
            ClientCall<ReqT, RespT> call,
            ReqT request,
            ClientCall.Listener<RespT> listener) {
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

    private static <ReqT, RespT> OutStreamObserver<ReqT> asyncBidiStreamingCall(
            ClientCall<ReqT, RespT> call,
            StreamObserver<RespT> responseObserver) {
        AsyncBidiStreamingOutAdapter<ReqT, RespT> adapter
                = new AsyncBidiStreamingOutAdapter<>(call);
        AsyncBidiStreamingInAdapter<ReqT, RespT> responseListener
                = new AsyncBidiStreamingInAdapter<>(responseObserver, adapter);
        call.start(responseListener, new Metadata());
        responseListener.onStart();
        return adapter;
    }

    @Override
    public String getDatabase() {
        return database;
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

    private static ManagedChannel createChannel(Builder builder) {
        String endpoint = builder.getEndpoint();
        String database = builder.getDatabase();
        List<HostAndPort> hosts = builder.getHosts();
        assert (endpoint == null) || ((endpoint != null) && (database != null));
        assert (endpoint == null) != (hosts == null);

        final String localDc = builder.getLocalDc();

        String defaultPolicy = "round_robin";

        if (!StringUtil.isNullOrEmpty(localDc)) {
            defaultPolicy = "prefer_nearest";
            final LoadBalancerRegistry lbr = LoadBalancerRegistry.getDefaultRegistry();

            lbr.register(new LoadBalancerProvider() {
                @Override
                public boolean isAvailable() {
                    return true;
                }

                @Override
                public int getPriority() {
                    return 10;
                }

                ;

                @Override
                public String getPolicyName() {
                    return "prefer_nearest";
                }

                @Override
                public LoadBalancer newLoadBalancer(LoadBalancer.Helper helper) {
                    return new PreferNearestLoadBalancer(helper, localDc);
                }
            });
        }

        final NettyChannelBuilder channelBuilder;
        if (endpoint != null) {
            channelBuilder = NettyChannelBuilder.forTarget(YdbNameResolver.makeTarget(endpoint, database))
                    .nameResolverFactory(YdbNameResolver.newFactory(
                            builder.getAuthProvider(),
                            builder.cert,
                            builder.useTLS,
                            builder.endpointsDiscoveryPeriod,
                            builder.getCallExecutor(),
                            builder.getChannelInitializer()))
                    .defaultLoadBalancingPolicy(defaultPolicy);
        } else if (hosts.size() > 1) {
            channelBuilder = NettyChannelBuilder.forTarget(HostsNameResolver.makeTarget(hosts))
                    .nameResolverFactory(HostsNameResolver.newFactory(hosts, builder.getCallExecutor()))
                    .defaultLoadBalancingPolicy(defaultPolicy);
        } else {
            channelBuilder = NettyChannelBuilder.forAddress(
                    hosts.get(0).getHost(),
                    hosts.get(0).getPortOrDefault(DEFAULT_PORT));
        }

        if (builder.useTLS) {
            if (builder.cert != null) {
                channelBuilder
                        .negotiationType(NegotiationType.TLS)
                        .sslContext(createSslContext(builder.cert));
            } else {
                channelBuilder
                        .negotiationType(NegotiationType.TLS)
                        .sslContext(createSslContext());
            }
        } else {
            channelBuilder.negotiationType(NegotiationType.PLAINTEXT);
        }

        channelBuilder
                .maxInboundMessageSize(64 << 20) // 64 MiB
                .withOption(ChannelOption.ALLOCATOR, ByteBufAllocator.DEFAULT);

        builder.getChannelInitializer().accept(channelBuilder);
        return channelBuilder.build();
    }

    private static SslContext createSslContext() {
        try {
            return GrpcSslContexts.forClient()
                    .trustManager(new YandexTrustManagerFactory(""))
                    .build();
        } catch (SSLException e) {
            throw new RuntimeException("cannot create ssl context", e);
        }
    }

    private static SslContext createSslContext(byte[] cert) {
        try {
            return GrpcSslContexts.forClient()
                    .trustManager(new ByteArrayInputStream(cert))
                    .build();
        } catch (SSLException e) {
            throw new RuntimeException("cannot create ssl context", e);
        }
    }

    private static Channel interceptChannel(ManagedChannel realChannel, Builder builder) {
        if (builder.getDatabase() == null) {
            return realChannel;
        }

        Metadata extraHeaders = new Metadata();
        extraHeaders.put(YdbHeaders.DATABASE, builder.getDatabase());
        extraHeaders.put(YdbHeaders.BUILD_INFO, builder.getVersionString());
        ClientInterceptor interceptor = MetadataUtils.newAttachHeadersInterceptor(extraHeaders);
        return ClientInterceptors.intercept(realChannel, interceptor);
    }

    private static CallOptions createCallOptions(Builder builder) {
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

    private static <T> Result<T> deadlineExpiredResult(MethodDescriptor<?, T> method) {
        String message = "deadline expired before calling method " + method.getFullMethodName();
        return Result.fail(StatusCode.CLIENT_DEADLINE_EXCEEDED, Issue.of(message, ESeverityId.S_ERROR));
    }

    private static Status deadlineExpiredStatus(MethodDescriptor<?, ?> method) {
        String message = "deadline expired before calling method " + method.getFullMethodName();
        return Status.DEADLINE_EXCEEDED.withDescription(message);
    }

    /**
     * BUILDER
     */
    @ParametersAreNonnullByDefault
    public static final class Builder extends RpcTransportBuilder<GrpcTransport, Builder> {
        private final String endpoint;
        private String database;
        private final List<HostAndPort> hosts;
        private byte[] cert = null;
        private boolean useTLS = false;
        private Consumer<NettyChannelBuilder> channelInitializer = (cb) -> {
        };
        private String localDc;
        private Duration endpointsDiscoveryPeriod = Duration.ofSeconds(15);

        private Builder(@Nullable String endpoint, @Nullable String database, @Nullable List<HostAndPort> hosts) {
            this.endpoint = endpoint;
            this.database = database;
            this.hosts = hosts;
        }

        @Nullable
        public List<HostAndPort> getHosts() {
            return hosts;
        }

        @Nullable
        public String getEndpoint() {
            return endpoint;
        }

        public Duration getEndpointsDiscoveryPeriod() {
            return endpointsDiscoveryPeriod;
        }

        @Nullable
        public String getDatabase() {
            return database;
        }

        public Builder withDataBase(String dataBase) {
            this.database = dataBase;
            return this;
        }

        public String getVersionString() {
            Properties prop = new Properties();

            try {
                InputStream in = getClass().getResourceAsStream("/version.properties");
                prop.load(in);

                String arcadiaSourceUrl = prop.getProperty("ArcadiaSourceUrl");
                String arcadiaLastChangeNum = prop.getProperty("ArcadiaLastChangeNum");
                String version = prop.getProperty("version");

                if (version != null) {
                    return version;
                } else {
                    return arcadiaSourceUrl + "@" + arcadiaLastChangeNum;
                }

            } catch (Exception ex) {
            }

            return "unknown-version";
        }

        public Consumer<NettyChannelBuilder> getChannelInitializer() {
            return channelInitializer;
        }

        public String getLocalDc() {
            return localDc;
        }

        public Builder withChannelInitializer(Consumer<NettyChannelBuilder> channelInitializer) {
            this.channelInitializer = checkNotNull(channelInitializer, "channelInitializer is null");
            return this;
        }

        public Builder withLocalDataCenter(String dc) {
            this.localDc = dc;
            return this;
        }

        public Builder withEndpointsDiscoveryPeriod(Duration period) {
            this.endpointsDiscoveryPeriod = period;
            return this;
        }

        public Builder withSecureConnection(byte[] cert) {
            this.cert = cert.clone();
            this.useTLS = true;
            return this;
        }

        public Builder withSecureConnection() {
            this.useTLS = true;
            return this;
        }

        @Override
        public GrpcTransport build() {
            return new GrpcTransport(this);
        }
    }
}
