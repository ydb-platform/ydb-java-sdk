package tech.ydb.core.grpc.impl;

import com.google.common.base.Strings;
import java.time.Duration;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import com.google.common.net.HostAndPort;
import com.google.common.util.concurrent.MoreExecutors;
import tech.ydb.core.Result;
import tech.ydb.core.grpc.BalancingSettings;
import tech.ydb.core.grpc.ChannelSettings;
import tech.ydb.core.grpc.DiscoveryMode;
import tech.ydb.core.grpc.EndpointInfo;
import tech.ydb.core.grpc.GrpcDiscoveryRpc;
import tech.ydb.core.grpc.GrpcRequestSettings;
import tech.ydb.core.grpc.GrpcTransport;
import tech.ydb.core.grpc.ServerStreamToObserver;
import tech.ydb.core.grpc.UnaryStreamToFuture;
import tech.ydb.core.rpc.StreamControl;
import tech.ydb.core.rpc.StreamObserver;
import tech.ydb.core.utils.Async;
import io.grpc.CallOptions;
import io.grpc.Channel;
import io.grpc.ClientCall;
import io.grpc.ClientInterceptor;
import io.grpc.ClientInterceptors;
import io.grpc.ConnectivityState;
import io.grpc.ManagedChannel;
import io.grpc.Metadata;
import io.grpc.MethodDescriptor;
import io.grpc.Status;
import io.grpc.stub.MetadataUtils;
import io.netty.util.Timeout;
import io.netty.util.TimerTask;
import java.util.EnumSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.ydb.core.Issue;
import tech.ydb.core.StatusCode;
import tech.ydb.core.auth.AuthProvider;
import tech.ydb.core.auth.NopAuthProvider;
import tech.ydb.core.grpc.GrpcOperationTray;
import tech.ydb.core.grpc.GrpcStatuses;
import tech.ydb.core.grpc.GrpcTransportBuilder;
import tech.ydb.core.grpc.YdbCallCredentials;
import tech.ydb.core.grpc.YdbHeaders;
import tech.ydb.core.rpc.OperationTray;

/**
 * @author Nikolay Perfilov
 */
public class YdbTransportImpl implements GrpcTransport {
    private static final int DEFAULT_PORT = 2135;
    private static final long WAIT_FOR_CLOSING_MS = 1000;
    private static final long DISCOVERY_TIMEOUT_SECONDS = 10;

    // Interval between discovery requests when everything is ok
    private static final long DISCOVERY_PERIOD_NORMAL_SECONDS = 60;
    // Interval between discovery requests when pessimization threshold is exceeded
    private static final long DISCOVERY_PERIOD_MIN_SECONDS = 5;
    // Maximum percent of endpoints pessimized by transport errors to start recheck
    private static final long DISCOVERY_PESSIMIZATION_THRESHOLD = 50;

    protected static final EnumSet<ConnectivityState> TEMPORARY_STATES = EnumSet.of(
        ConnectivityState.IDLE,
        ConnectivityState.CONNECTING
    );

    private static final Logger logger = LoggerFactory.getLogger(YdbTransportImpl.class);

    private final GrpcDiscoveryRpc discoveryRpc;
    private final EndpointPool endpointPool;
    private final GrpcChannelPool channelPool;
    private final PeriodicDiscoveryTask periodicDiscoveryTask = new PeriodicDiscoveryTask();
    private final CallOptions callOptions;
    private final String database;
    private final GrpcOperationTray operationTray;
    private final long defaultReadTimeoutMillis;
    private final DiscoveryMode discoveryMode;
    private volatile boolean shutdown = false;

    public YdbTransportImpl(GrpcTransportBuilder builder) {
        this.callOptions = createCallOptions(builder);
        this.defaultReadTimeoutMillis = builder.getReadTimeoutMillis();
        this.database = Strings.nullToEmpty(builder.getDatabase());
        this.operationTray = new GrpcOperationTray(this);
        this.discoveryMode = builder.getDiscoveryMode();

        this.discoveryRpc = createDiscoveryRpc(builder);

        BalancingSettings balancingSettings = builder.getBalancingSettings();
        if (balancingSettings == null) {
            if (builder.getLocalDc() == null) {
                balancingSettings = new BalancingSettings();
            } else {
                balancingSettings = BalancingSettings.fromLocation(builder.getLocalDc());
            }
        }
        logger.debug("creating YDB transport with {}", balancingSettings);

        this.endpointPool = new EndpointPool(
            () -> discoveryRpc.listEndpoints(
                database,
                GrpcRequestSettings.newBuilder()
                        .withDeadlineAfter(System.nanoTime() + Duration.ofSeconds(DISCOVERY_TIMEOUT_SECONDS).toNanos())
                        .build()
            ),
            balancingSettings
        );

        periodicDiscoveryTask.start();

        channelPool = new GrpcChannelPool(ChannelSettings.fromBuilder(builder), endpointPool.getRecords());
    }

    private GrpcDiscoveryRpc createDiscoveryRpc(GrpcTransportBuilder builder) {
        String endpoint = builder.getEndpoint();
        if (endpoint == null || builder.getDatabase() == null) {
            throw new IllegalArgumentException(
                    "YDB transport implementation does not support multiple hosts settings (GrpcTransport.forHosts). " +
                            "Use GrpcTransport.forEndpoint instead." +
                            " Or use Grpc transport implementation (TransportImplType.GRPC_TRANSPORT_IMPL)");
        }
        HostAndPort hostAndPort = HostAndPort.fromString(endpoint);
        GrpcTransportBuilder transportBuilder = GrpcTransport
                .forHost(hostAndPort.getHost(), hostAndPort.getPortOrDefault(DEFAULT_PORT))
                .withAuthProvider(builder.getAuthProvider())
                .withCallExecutor(builder.getCallExecutor())
                .withDataBase(builder.getDatabase())
                .withChannelInitializer(builder.getChannelInitializer())
                .withDiscoveryMode(DiscoveryMode.SYNC);

        if (builder.getUseTls()) {
            if (builder.getCert() != null) {
                transportBuilder.withSecureConnection(builder.getCert());
            } else {
                transportBuilder.withSecureConnection();
            }
        }
        GrpcTransport transport = transportBuilder.build();
        return new GrpcDiscoveryRpc(transport);
    }

    private void checkEndpointResponse(Result<?> result, String endpoint) {
        if (result.getCode().isTransportError()) {
            endpointPool.pessimizeEndpoint(endpoint);
        }
    }

    private void checkEndpointResponse(Status grpcStatus, String endpoint) {
        if (!grpcStatus.isOk()) {
            endpointPool.pessimizeEndpoint(endpoint);
        }
    }

    @Override
    public String getEndpointByNodeId(int nodeId) {
        return endpointPool.getEndpointByNodeId(nodeId);
    }
    
    @Override
    public <ReqT, RespT> CompletableFuture<Result<RespT>> unaryCall(
            MethodDescriptor<ReqT, RespT> method,
            ReqT request,
            GrpcRequestSettings settings) {
        CallOptions callOptions = this.callOptions;
        if (settings.getDeadlineAfter() > 0) {
            final long now = System.nanoTime();
            if (now >= settings.getDeadlineAfter()) {
                return CompletableFuture.completedFuture(deadlineExpiredResult(method));
            }
            callOptions = this.callOptions.withDeadlineAfter(settings.getDeadlineAfter() - now, TimeUnit.NANOSECONDS);
        } else if (defaultReadTimeoutMillis > 0) {
            callOptions = this.callOptions.withDeadlineAfter(defaultReadTimeoutMillis, TimeUnit.MILLISECONDS);
        }

        CompletableFuture<Result<RespT>> promise = new CompletableFuture<>();

        if (!shutdown) {
            return makeUnaryCall(method, request, callOptions, settings, promise);
        } else {
            promise.complete(cancelResultDueToShutdown());
        }
        return promise;
    }

    @Override
    public <ReqT, RespT> StreamControl serverStreamCall(
            MethodDescriptor<ReqT, RespT> method,
            ReqT request,
            StreamObserver<RespT> observer,
            GrpcRequestSettings settings) {
        CallOptions callOptions = this.callOptions;
        if (settings.getDeadlineAfter() > 0) {
            final long now = System.nanoTime();
            if (now >= settings.getDeadlineAfter()) {
                observer.onError(GrpcStatuses.toStatus(deadlineExpiredStatus(method)));
                return () -> {};
            }
            callOptions = this.callOptions.withDeadlineAfter(settings.getDeadlineAfter() - now, TimeUnit.NANOSECONDS);
        } else if (defaultReadTimeoutMillis > 0) {
            callOptions = this.callOptions.withDeadlineAfter(defaultReadTimeoutMillis, TimeUnit.MILLISECONDS);
        }

        if (!shutdown) {
            return makeServerStreamCall(method, request, callOptions, settings, observer);
        } else {
            observer.onError(cancelResultDueToShutdown().toStatus());
            return () -> {};
        }
    }
    
    private <ReqT, RespT> CompletableFuture<Result<RespT>> makeUnaryCall(
            MethodDescriptor<ReqT, RespT> method,
            ReqT request,
            CallOptions callOptions,
            GrpcRequestSettings settings,
            CompletableFuture<Result<RespT>> promise) {
        EndpointInfo preferredEndpoint = settings.getPreferredEndpoint();
        GrpcChannel channel = channelPool.getChannel(endpointPool.getEndpoint(
                preferredEndpoint != null ? preferredEndpoint.getEndpoint() : null));
        ClientCall<ReqT, RespT> call = channel.channel.newCall(method, callOptions);
        if (logger.isDebugEnabled()) {
            logger.debug("Sending request to {}, method `{}', request: `{}'", channel.getEndpoint(), method,
                    request);
        }
        sendOneRequest(call, request, settings, new UnaryStreamToFuture<>(promise, settings.getTrailersHandler()));
        return promise.thenApply(result -> {
            checkEndpointResponse(result, channel.getEndpoint());
            return result;
        });
    }

    private <ReqT, RespT> StreamControl makeServerStreamCall(
            MethodDescriptor<ReqT, RespT> method,
            ReqT request,
            CallOptions callOptions,
            GrpcRequestSettings settings,
            StreamObserver<RespT> observer) {
        EndpointInfo preferredEndpoint = settings.getPreferredEndpoint();
        GrpcChannel channel = channelPool.getChannel(endpointPool.getEndpoint(
                preferredEndpoint != null ? preferredEndpoint.getEndpoint() : null));
        ClientCall<ReqT, RespT> call = channel.channel.newCall(method, callOptions);
        sendOneRequest(
                call,
                request,
                settings,
                new ServerStreamToObserver<>(
                        observer,
                        call,
                        (status) -> {
                            checkEndpointResponse(status, channel.getEndpoint());
                        }
                )
        );
        return () -> {
            call.cancel("Cancelled on user request", new CancellationException());
        };
    }

    private <RespT> Result<RespT> cancelResultDueToShutdown() {
        Issue issue = Issue.of("Request was not sent: transport is shutting down", Issue.Severity.ERROR);
        return Result.fail(StatusCode.CLIENT_CANCELLED, issue);
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

    @Override
    public void close() {
        shutdown = true;
        operationTray.close();
        periodicDiscoveryTask.stop();
        channelPool.shutdown(WAIT_FOR_CLOSING_MS);
    }

    protected static Channel interceptChannel(ManagedChannel realChannel, ChannelSettings channelSettings) {
        if (channelSettings.getDatabase() == null) {
            return realChannel;
        }

        Metadata extraHeaders = new Metadata();
        extraHeaders.put(YdbHeaders.DATABASE, channelSettings.getDatabase());
        extraHeaders.put(YdbHeaders.BUILD_INFO, channelSettings.getVersion());
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

    private static <T> Result<T> deadlineExpiredResult(MethodDescriptor<?, T> method) {
        String message = "deadline expired before calling method " + method.getFullMethodName();
        return Result.fail(StatusCode.CLIENT_DEADLINE_EXPIRED, Issue.of(message, Issue.Severity.ERROR));
    }

    private static Status deadlineExpiredStatus(MethodDescriptor<?, ?> method) {
        String message = "deadline expired before calling method " + method.getFullMethodName();
        return Status.DEADLINE_EXCEEDED.withDescription(message);
    }

    @Override
    public OperationTray getOperationTray() {
        return operationTray;
    }

    @Override
    public String getDatabase() {
        return database;
    }

    /**
     * PERIODIC DISCOVERY TASK
     */
    private final class PeriodicDiscoveryTask implements TimerTask {
        private volatile boolean stopped = false;
        private Timeout scheduledHandle = null;

        void stop() {
            logger.debug("stopping PeriodicDiscoveryTask");
            stopped = true;
            if (scheduledHandle != null) {
                scheduledHandle.cancel();
                scheduledHandle = null;
            }
        }

        void start() {
            CompletableFuture<EndpointPool.EndpointUpdateResultData> firstRunFuture = runDiscovery(true);
            if (firstRunFuture == null) {
                // TODO: Retry?
                throw new RuntimeException("Couldn't perform discovery on GrpcTransport start");
            }
            // Waiting for first discovery result...
            EndpointPool.EndpointUpdateResultData firstRunData = firstRunFuture.join();
            if (!firstRunData.discoveryStatus.isSuccess()) {
                throw new RuntimeException("Couldn't perform discovery on GrpcTransport start with status: "
                        + firstRunData.discoveryStatus);
            }
        }

        @Override
        public void run(Timeout timeout) {
            int pessimizationRatio = endpointPool.getPessimizationRatio();
            if (pessimizationRatio > DISCOVERY_PESSIMIZATION_THRESHOLD) {
                logger.info("launching discovery due to pessimization threshold is exceeded: {} is more than {}",
                        pessimizationRatio, DISCOVERY_PESSIMIZATION_THRESHOLD);
                runDiscovery(false);
            } else if (endpointPool.getTimeSinceLastUpdate().getSeconds() > DISCOVERY_PERIOD_NORMAL_SECONDS) {
                logger.debug("launching discovery in normal mode");
                runDiscovery(false);
            } else {
                scheduleNextDiscovery();
                logger.trace("no need to run discovery yet");
            }
        }

        private CompletableFuture<EndpointPool.EndpointUpdateResultData> runDiscovery(boolean firstRun) {
            if (stopped) {
                return null;
            }

            EndpointPool.EndpointUpdateResult updateResult = endpointPool.updateAsync();
            assert !firstRun || updateResult.discoveryWasPerformed;
            if (!updateResult.discoveryWasPerformed) {
                logger.debug("discovery was not performed: already in progress");
                scheduleNextDiscovery();
                return null;
            }

            logger.debug("discovery was requested (firstRun = {}), waiting for result...", firstRun);
            return updateResult.data
                    .thenApply(updateResultData -> {
                        if (updateResultData.discoveryStatus.isSuccess()) {
                            logger.debug("discovery was successfully performed");
                            if (channelPool != null) {
                                channelPool.removeChannels(updateResultData.removed, WAIT_FOR_CLOSING_MS);
                                logger.debug("channelPool.removeChannels executed successfully");
                            }
                        } else {
                            logger.warn("couldn't perform discovery with status: {}", updateResultData.discoveryStatus);
                        }
                        scheduleNextDiscovery();
                        return updateResultData;
                    })
                    .exceptionally(e -> {
                        logger.warn("couldn't perform discovery with exception: {}", e.toString());
                        scheduleNextDiscovery();
                        return null;
                    });
        }

        void scheduleNextDiscovery() {
            scheduledHandle = Async.runAfter(this, DISCOVERY_PERIOD_MIN_SECONDS, TimeUnit.SECONDS);
        }
    }
}
