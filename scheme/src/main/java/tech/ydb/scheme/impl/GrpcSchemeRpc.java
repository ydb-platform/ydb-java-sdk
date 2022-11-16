package tech.ydb.scheme.impl;

import java.util.concurrent.CompletableFuture;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import javax.annotation.WillClose;
import javax.annotation.WillNotClose;

import tech.ydb.core.Operations;
import tech.ydb.core.Result;
import tech.ydb.core.Status;
import tech.ydb.core.grpc.GrpcRequestSettings;
import tech.ydb.core.grpc.GrpcTransport;
import tech.ydb.scheme.SchemeOperationProtos.DescribePathRequest;
import tech.ydb.scheme.SchemeOperationProtos.DescribePathResponse;
import tech.ydb.scheme.SchemeOperationProtos.DescribePathResult;
import tech.ydb.scheme.SchemeOperationProtos.ListDirectoryRequest;
import tech.ydb.scheme.SchemeOperationProtos.ListDirectoryResponse;
import tech.ydb.scheme.SchemeOperationProtos.ListDirectoryResult;
import tech.ydb.scheme.SchemeOperationProtos.MakeDirectoryRequest;
import tech.ydb.scheme.SchemeOperationProtos.MakeDirectoryResponse;
import tech.ydb.scheme.SchemeOperationProtos.RemoveDirectoryRequest;
import tech.ydb.scheme.SchemeOperationProtos.RemoveDirectoryResponse;
import tech.ydb.scheme.SchemeRpc;
import tech.ydb.scheme.v1.SchemeServiceGrpc;


/**
 * @author Sergey Polovko
 */
@ParametersAreNonnullByDefault
public final class GrpcSchemeRpc implements SchemeRpc {

    private final GrpcTransport transport;
    private final boolean transportOwned;

    private GrpcSchemeRpc(GrpcTransport transport, boolean transportOwned) {
        this.transport = transport;
        this.transportOwned = transportOwned;
    }

    @Nullable
    public static GrpcSchemeRpc useTransport(@WillNotClose GrpcTransport transport) {
        return new GrpcSchemeRpc(transport, false);
    }

    @Nullable
    public static GrpcSchemeRpc ownTransport(@WillClose GrpcTransport transport) {
        return new GrpcSchemeRpc(transport, true);
    }

    @Override
    public CompletableFuture<Status> makeDirectory(MakeDirectoryRequest request, GrpcRequestSettings settings) {
        return transport
                .unaryCall(SchemeServiceGrpc.getMakeDirectoryMethod(), settings, request)
                .thenApply(Operations.statusUnwrapper(MakeDirectoryResponse::getOperation));

    }

    @Override
    public CompletableFuture<Status> removeDirectory(RemoveDirectoryRequest request, GrpcRequestSettings settings) {
        return transport
                .unaryCall(SchemeServiceGrpc.getRemoveDirectoryMethod(), settings, request)
                .thenApply(Operations.statusUnwrapper(RemoveDirectoryResponse::getOperation));
    }

    @Override
    public CompletableFuture<Result<ListDirectoryResult>> describeDirectory(ListDirectoryRequest request,
                                                                              GrpcRequestSettings settings) {
        return transport
                .unaryCall(SchemeServiceGrpc.getListDirectoryMethod(), settings, request)
                .thenApply(Operations.resultUnwrapper(ListDirectoryResponse::getOperation, ListDirectoryResult.class));
    }

    @Override
    public CompletableFuture<Result<DescribePathResult>> describePath(DescribePathRequest request,
                                                                        GrpcRequestSettings settings) {
        return transport
                .unaryCall(SchemeServiceGrpc.getDescribePathMethod(), settings, request)
                .thenApply(Operations.resultUnwrapper(DescribePathResponse::getOperation, DescribePathResult.class));
    }

    @Override
    public String getDatabase() {
        return transport.getDatabase();
    }

    @Override
    public void close() {
        if (transportOwned) {
            transport.close();
        }
    }
}
