package tech.ydb.core.grpc.impl;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import com.google.common.base.Strings;
import com.google.common.net.HostAndPort;
import io.grpc.CallOptions;
import io.grpc.Channel;
import io.grpc.MethodDescriptor;
import io.grpc.Status;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tech.ydb.core.Issue;
import tech.ydb.core.Result;
import tech.ydb.core.StatusCode;
import tech.ydb.core.grpc.BalancingSettings;
import tech.ydb.core.grpc.EndpointInfo;
import tech.ydb.core.grpc.GrpcRequestSettings;
import tech.ydb.core.grpc.GrpcTransportBuilder;
import tech.ydb.core.rpc.StreamControl;
import tech.ydb.core.rpc.StreamObserver;
import tech.ydb.discovery.DiscoveryProtos;

/**
 * @author Nikolay Perfilov
 */
public class YdbTransportImpl extends BaseGrpcTrasnsport {
    private static final int DEFAULT_PORT = 2135;

    private static final Result<?> SHUTDOWN_RESULT =  Result.fail(tech.ydb.core.Status.of(
            StatusCode.CLIENT_CANCELLED, null,
            Issue.of("Request was not sent: transport is shutting down", Issue.Severity.ERROR)
    ));

    private static final Result<?> NOT_READY =  Result.fail(tech.ydb.core.Status.of(
            StatusCode.CLIENT_INTERNAL_ERROR, null,
            Issue.of("Request was not sent: transport is not ready", Issue.Severity.ERROR)
    ));

    private static final Logger logger = LoggerFactory.getLogger(YdbTransportImpl.class);

    private final GrpcDiscoveryRpc discoveryRpc;
    private final AuthCallOptions callOptionsProvider;
    private final String database;
    private final EndpointPool endpointPool;
    private final GrpcChannelPool channelPool;
    private final YdbDiscoveryHandler discoveryHandler;
    private final PeriodicDiscoveryTask periodicDiscoveryTask;
    private volatile boolean shutdown = false;

    public YdbTransportImpl(GrpcTransportBuilder builder) {
        super(builder.getReadTimeoutMillis());
        ChannelFactory channelFactory = ChannelFactory.fromBuilder(builder);
        BalancingSettings balancingSettings = getBalancingSettings(builder);
        EndpointRecord discoveryEndpoint = getDiscoverytEndpoint(builder);

        logger.info("creating YDB transport with {}", balancingSettings);

        this.database = Strings.nullToEmpty(builder.getDatabase());
        this.callOptionsProvider = new AuthCallOptions(this,
                discoveryEndpoint,
                channelFactory,
                builder.getAuthProvider(),
                builder.getCallExecutor()
        );
        this.discoveryRpc = new GrpcDiscoveryRpc(this, discoveryEndpoint, channelFactory);

        this.channelPool = new GrpcChannelPool(channelFactory);
        this.endpointPool = new EndpointPool(balancingSettings);
        this.discoveryHandler = new YdbDiscoveryHandler();
        this.periodicDiscoveryTask = new PeriodicDiscoveryTask(discoveryRpc, discoveryHandler);
    }

    public void init() {
        periodicDiscoveryTask.start();
    }

    private static EndpointRecord getDiscoverytEndpoint(GrpcTransportBuilder builder) {
        URI endpointURI = null;
        try {
            if (builder.getEndpoint() != null) {
                endpointURI = new URI(null, builder.getEndpoint(), null, null, null);
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

        return new EndpointRecord(endpointURI.getHost(), endpointURI.getPort(), 0);
    }

    private static BalancingSettings getBalancingSettings(GrpcTransportBuilder builder) {
        BalancingSettings balancingSettings = builder.getBalancingSettings();
        if (balancingSettings != null) {
            return balancingSettings;
        }

        String localDc = builder.getLocalDc();
        if (localDc != null) {
            return BalancingSettings.fromLocation(builder.getLocalDc());
        }

        return new BalancingSettings();
    }

    @Override
    public CallOptions getCallOptions() {
        return callOptionsProvider.getCallOptions();
    }

    @Override
    public String getEndpointByNodeId(int nodeId) {
        return endpointPool.getEndpointByNodeId(nodeId);
    }

    @Override
    public <ReqT, RespT> CompletableFuture<Result<RespT>> unaryCall(
            MethodDescriptor<ReqT, RespT> method,
            GrpcRequestSettings settings,
            ReqT request) {
        if (shutdown) {
            return CompletableFuture.completedFuture(SHUTDOWN_RESULT.map(null));
        }

        return super.unaryCall(method, settings, request);
    }

    @Override
    public <ReqT, RespT> StreamControl serverStreamCall(
            MethodDescriptor<ReqT, RespT> method,
            GrpcRequestSettings settings,
            ReqT request,
            StreamObserver<RespT> observer) {
        if (shutdown) {
            observer.onError(SHUTDOWN_RESULT.getStatus());
            return () -> { };
        }

        return super.serverStreamCall(method, settings, request, observer);
    }

    @Override
    public void close() {
        shutdown = true;
        periodicDiscoveryTask.stop();
        channelPool.shutdown();
        callOptionsProvider.close();
    }

    @Override
    public String getDatabase() {
        return database;
    }

    @Override
    protected CheckableChannel getChannel(GrpcRequestSettings settings) {
        return new YdbChannel(settings);
    }

    private class YdbChannel implements CheckableChannel {
        private final GrpcChannel channel;

        YdbChannel(GrpcRequestSettings settings) {
            EndpointInfo preferredEndpoint = settings.getPreferredEndpoint();
            EndpointRecord endpoint = endpointPool.getEndpoint(
                    preferredEndpoint != null ? preferredEndpoint.getEndpoint() : null);
            this.channel = channelPool.getChannel(endpoint);
        }

        @Override
        public Channel grpcChannel() {
            return channel.getReadyChannel();
        }

        @Override
        public String endpoint() {
            return channel.getEndpoint();
        }

        @Override
        public void updateGrpcStatus(Status status) {
            if (!status.isOk()) {
                endpointPool.pessimizeEndpoint(channel.getEndpoint());
            }
        }
    }

    private class YdbDiscoveryHandler implements PeriodicDiscoveryTask.DiscoveryHandler {
        @Override
        public boolean useMinDiscoveryPeriod() {
            return endpointPool.needToRunDiscovery();
        }

        @Override
        public void handleDiscoveryResult(DiscoveryProtos.ListEndpointsResult result) {
            List<EndpointRecord> removed = endpointPool.setNewState(
                    result.getSelfLocation(), result.getEndpointsList()
            );
            channelPool.removeChannels(removed);
        }
    }
}
