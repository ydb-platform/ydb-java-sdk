package tech.ydb.test.integration.utils;

import java.util.concurrent.CompletableFuture;

import io.grpc.MethodDescriptor;

import tech.ydb.core.Result;
import tech.ydb.core.grpc.GrpcRequestSettings;
import tech.ydb.core.grpc.GrpcTransport;
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
    public String getEndpointByNodeId(int nodeId) {
        return checked().getEndpointByNodeId(nodeId);
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
    public String getDatabase() {
        return checked().getDatabase();
    }

    @Override
    public void close() {
        // Usally origin transport must be closed by its owner
    }
}
