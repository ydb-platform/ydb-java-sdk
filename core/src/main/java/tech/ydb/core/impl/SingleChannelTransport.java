package tech.ydb.core.impl;

import java.util.concurrent.ScheduledExecutorService;

import com.google.common.base.Strings;
import io.grpc.CallOptions;
import io.grpc.Status;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tech.ydb.core.grpc.GrpcRequestSettings;
import tech.ydb.core.grpc.GrpcTransportBuilder;
import tech.ydb.core.impl.auth.AuthCallOptions;
import tech.ydb.core.impl.pool.EndpointRecord;
import tech.ydb.core.impl.pool.GrpcChannel;
import tech.ydb.core.impl.pool.ManagedChannelFactory;

/**
 *
 * @author Aleksandr Gorshenin
 */
public class SingleChannelTransport extends BaseGrpcTrasnsport {
    private static final Logger logger = LoggerFactory.getLogger(SingleChannelTransport.class);

    private final AuthCallOptions callOptionsProvider;
    private final GrpcChannel channel;
    private final String database;
    private final ScheduledExecutorService scheduler;

    public SingleChannelTransport(GrpcTransportBuilder builder) {
        ManagedChannelFactory channelFactory = ManagedChannelFactory.fromBuilder(builder);
        EndpointRecord endpoint = YdbTransportImpl.getDiscoverytEndpoint(builder);

        logger.info("creating signle channel transport with endpoint {}", endpoint);

        this.database = Strings.nullToEmpty(builder.getDatabase());
        this.channel = new GrpcChannel(endpoint, channelFactory, false);
        this.callOptionsProvider = new AuthCallOptions(this,
                endpoint,
                channelFactory,
                builder.getAuthProvider(),
                builder.getCallExecutor(),
                builder.getReadTimeoutMillis()
        );
        this.scheduler = YdbSchedulerFactory.createScheduler();
    }

    @Override
    public ScheduledExecutorService scheduler() {
        return scheduler;
    }

    @Override
    public String getDatabase() {
        return database;
    }

    @Override
    public void close() {
        channel.shutdown();
        callOptionsProvider.close();

        YdbSchedulerFactory.shutdownScheduler(scheduler);
    }

    @Override
    CallOptions getCallOptions() {
        return callOptionsProvider.getCallOptions();
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
