package tech.ydb.core.grpc.impl;

import java.time.Duration;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import tech.ydb.core.auth.AuthProvider;
import tech.ydb.core.grpc.GrpcRequestSettings;

import io.grpc.Channel;
import io.grpc.Status;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Aleksandr Gorshenin
 */
public class SingleChannelTransport extends BaseGrpcTrasnsport {
    private final static Logger logger = LoggerFactory.getLogger(SingleChannelTransport.class);
    private final String database;
    private final GrpcChannel channel;
    
    public SingleChannelTransport(
            AuthProvider authProvider,
            Executor executor,
            long readTimeoutMillis,
            EndpointRecord endpoint,
            ChannelFactory channelFactory) {
        super(authProvider, executor, readTimeoutMillis);
        this.database = channelFactory.getDatabase();
        this.channel = new GrpcChannel(endpoint, channelFactory, true);
    }

    @Override
    public String getEndpointByNodeId(int nodeId) {
        return channel.getEndpoint();
    }

    @Override
    public String getDatabase() {
        return database;
    }

    @Override
    public void close() {
        channel.shutdown();
    }

    @Override
    public boolean waitUntilReady(Duration timeout) {
        try {
            channel.getReadyFuture().get(timeout.toMillis(), TimeUnit.MILLISECONDS);
            return true;
        } catch (ExecutionException | TimeoutException ex) {
            logger.warn("single channel transport wait ready problem", ex);
            return false;
        } catch (InterruptedException ex) {
            logger.warn("single channel transport wait ready interrupted", ex);
            Thread.currentThread().interrupt();
            return false;
        }
    }

    @Override
    protected CheckableChannel getChannel(GrpcRequestSettings settings) {
        return new CheckableChannel() {
            @Override
            public Channel grpcChannel() { return channel.getReadyChannel(); }
            @Override
            public String endpoint() { return channel.getEndpoint(); }
            @Override
            public void updateGrpcStatus(Status status) {
                if (!status.isOk()) {
                    logger.warn("grpc error {}[{}] on single channel {}",
                            status.getCode(),
                            status.getDescription(),
                            channel.getEndpoint());
                }
            }
        };
    };
}
