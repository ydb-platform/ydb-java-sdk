package tech.ydb.test.integration.utils;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledExecutorService;

import io.grpc.CallOptions;
import io.grpc.MethodDescriptor;

import tech.ydb.core.Result;
import tech.ydb.core.grpc.GrpcRequestSettings;
import tech.ydb.core.grpc.GrpcTransport;
import tech.ydb.core.rpc.OutStreamObserver;
import tech.ydb.core.rpc.StreamControl;
import tech.ydb.core.rpc.StreamObserver;

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
    public ScheduledExecutorService scheduler() {
        return checked().scheduler();
    }

    @Override
    public <ReqT, RespT> CompletableFuture<Result<RespT>> unaryCall(
            MethodDescriptor<ReqT, RespT> method, GrpcRequestSettings settings, ReqT request) {
        return checked().unaryCall(method, settings, request);
    }

    @Override
    public <ReqT, RespT> StreamControl serverStreamCall(
            MethodDescriptor<ReqT, RespT> method, GrpcRequestSettings settings,
            ReqT request, StreamObserver<RespT> observer) {
        return checked().serverStreamCall(method, settings, request, observer);
    }

    @Override
    public <ReqT, RespT> OutStreamObserver<ReqT> bidirectionalStreamCall(
            MethodDescriptor<ReqT, RespT> method,
            StreamObserver<RespT> observer,
            GrpcRequestSettings settings) {
        return checked().bidirectionalStreamCall(method, observer, settings);
    }

    @Override
    public String getDatabase() {
        return checked().getDatabase();
    }

    @Override
    public CallOptions getCallOptions() {
        return checked().getCallOptions();
    }

    @Override
    public void close() {
        // Usually origin transport must be closed by its owner
    }
}
