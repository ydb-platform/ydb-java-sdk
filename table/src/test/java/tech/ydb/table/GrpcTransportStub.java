package tech.ydb.table;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledExecutorService;

import io.grpc.MethodDescriptor;

import tech.ydb.core.Result;
import tech.ydb.core.Status;
import tech.ydb.core.StatusCode;
import tech.ydb.core.grpc.GrpcReadStream;
import tech.ydb.core.grpc.GrpcReadWriteStream;
import tech.ydb.core.grpc.GrpcRequestSettings;
import tech.ydb.core.grpc.GrpcTransport;
import tech.ydb.core.impl.call.EmptyStream;

/**
 *
 * @author Aleksandr Gorshenin
 */
public class GrpcTransportStub implements GrpcTransport {
    private final ScheduledExecutorService scheduler;
    private final String database;

    public GrpcTransportStub(ScheduledExecutorService scheduler, String database) {
        this.scheduler = scheduler;
        this.database = database;
    }

    @Override
    public <ReqT, RespT> CompletableFuture<Result<RespT>> unaryCall(
            MethodDescriptor<ReqT, RespT> method, GrpcRequestSettings settings, ReqT request) {
        return CompletableFuture.completedFuture(Result.fail(Status.of(StatusCode.CLIENT_CALL_UNIMPLEMENTED)));
    }

    @Override
    public <ReqT, RespT> GrpcReadStream<RespT> readStreamCall(MethodDescriptor<ReqT, RespT> method, GrpcRequestSettings settings, ReqT request) {
        return new EmptyStream<>(Status.of(StatusCode.CLIENT_CALL_UNIMPLEMENTED));
    }

    @Override
    public <ReqT, RespT> GrpcReadWriteStream<RespT, ReqT> readWriteStreamCall(MethodDescriptor<ReqT, RespT> method, GrpcRequestSettings settings) {
        return new EmptyStream<>(Status.of(StatusCode.CLIENT_CALL_UNIMPLEMENTED));
    }

    @Override
    public String getDatabase() {
        return database;
    }

    @Override
    public ScheduledExecutorService getScheduler() {
        return scheduler;
    }

    @Override
    public void close() {
    }
}
