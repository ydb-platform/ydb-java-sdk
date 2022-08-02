package tech.ydb.core.grpc.impl;

import com.google.protobuf.Message;
import io.grpc.Channel;
import io.grpc.Status;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Function;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.ydb.OperationProtos;
import tech.ydb.core.Operations;
import tech.ydb.core.Result;
import tech.ydb.core.auth.AuthProvider;
import tech.ydb.core.grpc.GrpcRequestSettings;
import tech.ydb.core.rpc.OperationTray;

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
            ChannelSettings channelSettings) {
        super(authProvider, executor, readTimeoutMillis);
        this.database = channelSettings.getDatabase();
        this.channel = new GrpcChannel(endpoint, channelSettings);
    }

    @Override
    public String getEndpointByNodeId(int nodeId) {
        return channel.getEndpoint();
    }

    @Override
    public OperationTray getOperationTray() {
        // not supported
        return new OperationTray() {
            @Override
            public CompletableFuture<tech.ydb.core.Status> waitStatus(OperationProtos.Operation operation, GrpcRequestSettings settings) {
                return CompletableFuture.completedFuture(Operations.status(operation));
            }

            @Override
            public <M extends Message, R> CompletableFuture<Result<R>> waitResult(
                    OperationProtos.Operation operation, Class<M> resultClass, Function<M, R> mapper, GrpcRequestSettings settings) {
                try {
                    tech.ydb.core.Status status = Operations.status(operation);
                    if (status.isSuccess()) {
                        M resultMessage = Operations.unpackResult(operation, resultClass);
                        return CompletableFuture.completedFuture(Result.success(mapper.apply(resultMessage), status.getIssues()));
                    }
                    return CompletableFuture.completedFuture(Result.fail(status));
                } catch (Exception ex) {
                    logger.warn("wait result problem", ex);
                    return CompletableFuture.completedFuture(Result.error(ex));
                }
            }

            @Override
            public void close() {
            }
        };
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
    protected CheckableChannel getChannel(GrpcRequestSettings settings) {
        return new CheckableChannel() {
            @Override
            public Channel grpcChannel() { return channel.getGrpcChannel(); }
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
