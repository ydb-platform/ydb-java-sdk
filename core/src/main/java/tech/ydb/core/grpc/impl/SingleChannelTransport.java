package tech.ydb.core.grpc.impl;

import io.grpc.MethodDescriptor;
import java.util.concurrent.CompletableFuture;
import tech.ydb.core.Result;
import tech.ydb.core.grpc.GrpcRequestSettings;
import tech.ydb.core.grpc.GrpcTransport;
import tech.ydb.core.rpc.OperationTray;
import tech.ydb.core.rpc.StreamControl;
import tech.ydb.core.rpc.StreamObserver;

/**
 *
 * @author Aleksandr Gorshenin
 */
public class SingleChannelTransport implements GrpcTransport {

    @Override
    public String getEndpointByNodeId(int nodeId) {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    @Override
    public <ReqT, RespT> CompletableFuture<Result<RespT>> unaryCall(MethodDescriptor<ReqT, RespT> method, ReqT request, GrpcRequestSettings settings) {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    @Override
    public <ReqT, RespT> StreamControl serverStreamCall(MethodDescriptor<ReqT, RespT> method, ReqT request, StreamObserver<RespT> observer, GrpcRequestSettings settings) {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    @Override
    public OperationTray getOperationTray() {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    @Override
    public String getDatabase() {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    @Override
    public void close() {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }
    
}
