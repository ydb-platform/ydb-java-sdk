package tech.ydb.table.rpc.grpc;

import java.util.concurrent.CompletableFuture;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import javax.annotation.WillClose;
import javax.annotation.WillNotClose;

import tech.ydb.core.Result;
import tech.ydb.core.grpc.GrpcTransport;
import tech.ydb.core.rpc.OperationTray;
import tech.ydb.core.rpc.RpcTransport;
import tech.ydb.scheme.v1.SchemeServiceGrpc;
import tech.ydb.table.rpc.SchemeRpc;

import static tech.ydb.scheme.SchemeOperationProtos.DescribePathRequest;
import static tech.ydb.scheme.SchemeOperationProtos.DescribePathResponse;
import static tech.ydb.scheme.SchemeOperationProtos.ListDirectoryRequest;
import static tech.ydb.scheme.SchemeOperationProtos.ListDirectoryResponse;
import static tech.ydb.scheme.SchemeOperationProtos.MakeDirectoryRequest;
import static tech.ydb.scheme.SchemeOperationProtos.MakeDirectoryResponse;
import static tech.ydb.scheme.SchemeOperationProtos.RemoveDirectoryRequest;
import static tech.ydb.scheme.SchemeOperationProtos.RemoveDirectoryResponse;


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
    public static GrpcSchemeRpc useTransport(@WillNotClose RpcTransport transport) {
        if (transport instanceof GrpcTransport) {
            return new GrpcSchemeRpc((GrpcTransport) transport, false);
        }
        return null;
    }

    @Nullable
    public static GrpcSchemeRpc ownTransport(@WillClose RpcTransport transport) {
        if (transport instanceof GrpcTransport) {
            return new GrpcSchemeRpc((GrpcTransport) transport, true);
        }
        return null;
    }

    @Override
    public CompletableFuture<Result<MakeDirectoryResponse>> makeDirectory(MakeDirectoryRequest request) {
        return transport.unaryCall(SchemeServiceGrpc.METHOD_MAKE_DIRECTORY, request);
    }

    @Override
    public CompletableFuture<Result<RemoveDirectoryResponse>> removeDirectory(RemoveDirectoryRequest request) {
        return transport.unaryCall(SchemeServiceGrpc.METHOD_REMOVE_DIRECTORY, request);
    }

    @Override
    public CompletableFuture<Result<ListDirectoryResponse>> describeDirectory(ListDirectoryRequest request) {
        return transport.unaryCall(SchemeServiceGrpc.METHOD_LIST_DIRECTORY, request);
    }

    @Override
    public CompletableFuture<Result<DescribePathResponse>> describePath(DescribePathRequest request) {
        return transport.unaryCall(SchemeServiceGrpc.METHOD_DESCRIBE_PATH, request);
    }

    @Override
    public OperationTray getOperationTray() {
        return transport.getOperationTray();
    }

    @Override
    public void close() {
        if (transportOwned) {
            transport.close();
        }
    }
}
