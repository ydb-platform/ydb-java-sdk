package tech.ydb.core.impl;

import java.util.Collections;
import java.util.concurrent.ScheduledExecutorService;

import com.google.common.base.Strings;
import io.grpc.Status;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tech.ydb.core.grpc.GrpcRequestSettings;
import tech.ydb.core.grpc.GrpcTransportBuilder;
import tech.ydb.core.impl.auth.AuthCallOptions;
import tech.ydb.core.impl.operation.OperationManager;
import tech.ydb.core.impl.pool.EndpointRecord;
import tech.ydb.core.impl.pool.GrpcChannel;
import tech.ydb.core.impl.pool.ManagedChannelFactory;

/**
 * @author Aleksandr Gorshenin
 */
public class SingleChannelTransport extends BaseGrpcTransport {
    private static final Logger logger = LoggerFactory.getLogger(SingleChannelTransport.class);

    private final AuthCallOptions callOptions;
    private final GrpcChannel channel;
    private final String database;
    private final ScheduledExecutorService scheduler;
    private final OperationManager operationManager;

    public SingleChannelTransport(GrpcTransportBuilder builder) {
        ManagedChannelFactory channelFactory = ManagedChannelFactory.fromBuilder(builder);
        EndpointRecord endpoint = YdbTransportImpl.getDiscoveryEndpoint(builder);

        logger.info("creating signle channel transport with endpoint {}", endpoint);

        this.database = Strings.nullToEmpty(builder.getDatabase());
        this.channel = new GrpcChannel(endpoint, channelFactory, true);
        this.operationManager = new OperationManager(this);

        this.callOptions = new AuthCallOptions(this,
                Collections.singletonList(endpoint),
                channelFactory,
                builder.getAuthProvider(),
                builder.getReadTimeoutMillis(),
                builder.getCallExecutor()
        );
        this.scheduler = YdbSchedulerFactory.createScheduler();
    }

    @Override
    public ScheduledExecutorService getScheduler() {
        return scheduler;
    }

    @Override
    public OperationManager getOperationManager() {
        return operationManager;
    }

    @Override
    public String getDatabase() {
        return database;
    }

    @Override
    public void close() {
        super.close();

        channel.shutdown();
        callOptions.close();

        YdbSchedulerFactory.shutdownScheduler(scheduler);
    }

    @Override
    public AuthCallOptions getAuthCallOptions() {
        return callOptions;
    }

    @Override
    GrpcChannel getChannel(GrpcRequestSettings settings) {
        return channel;
    }

    @Override
    void updateChannelStatus(GrpcChannel channel, Status status) {
        if (!status.isOk()) {
            logger.warn("grpc error {}[{}] on single channel {}",
                    status.getCode(),
                    status.getDescription(),
                    channel.getEndpoint());
        }
    }
}
