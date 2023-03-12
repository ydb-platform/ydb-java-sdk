package tech.ydb.test.integration.utils;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledExecutorService;

import io.grpc.MethodDescriptor;

import tech.ydb.core.Result;
import tech.ydb.core.grpc.GrpcReadStream;
import tech.ydb.core.grpc.GrpcReadWriteStream;
import tech.ydb.core.grpc.GrpcRequestSettings;
import tech.ydb.core.grpc.GrpcTransport;

/**
 *
 * @author Aleksandr Gorshenin
 */
public abstract class ProxyGrpcTransport implements GrpcTransport {

    protected abstract GrpcTransport origin();

    private GrpcTransport checked() {
        GrpcTransport check = origin();
        if (check == null) {
            throw new NullPointerException("Can't proxy method of null");
        }
        return check;
    }

    @Override
    public ScheduledExecutorService getScheduler() {
        return checked().getScheduler();
    }

    @Override
    public <ReqT, RespT> CompletableFuture<Result<RespT>> unaryCall(
            MethodDescriptor<ReqT, RespT> method, GrpcRequestSettings settings, ReqT request) {
        return checked().unaryCall(method, settings, request);
    }

    @Override
    public <ReqT, RespT> GrpcReadStream<RespT> readStreamCall(
            MethodDescriptor<ReqT, RespT> method, GrpcRequestSettings settings, ReqT request) {
        return checked().readStreamCall(method, settings, request);
    }

    @Override
    public <ReqT, RespT> GrpcReadWriteStream<RespT, ReqT> readWriteStreamCall(
            MethodDescriptor<ReqT, RespT> method, GrpcRequestSettings settings) {
        return checked().readWriteStreamCall(method, settings);
    }

    @Override
    public String getDatabase() {
        return checked().getDatabase();
    }

    @Override
    public void close() {
        // Usally origin transport must be closed by its owner
    }
}
