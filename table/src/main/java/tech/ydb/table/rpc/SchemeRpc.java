package tech.ydb.table.rpc;

import java.util.concurrent.CompletableFuture;

import tech.ydb.core.Result;
import tech.ydb.core.grpc.GrpcRequestSettings;
import tech.ydb.core.rpc.Rpc;
import tech.ydb.scheme.SchemeOperationProtos.DescribePathRequest;
import tech.ydb.scheme.SchemeOperationProtos.DescribePathResponse;
import tech.ydb.scheme.SchemeOperationProtos.ListDirectoryRequest;
import tech.ydb.scheme.SchemeOperationProtos.ListDirectoryResponse;
import tech.ydb.scheme.SchemeOperationProtos.MakeDirectoryRequest;
import tech.ydb.scheme.SchemeOperationProtos.MakeDirectoryResponse;
import tech.ydb.scheme.SchemeOperationProtos.RemoveDirectoryRequest;
import tech.ydb.scheme.SchemeOperationProtos.RemoveDirectoryResponse;


/**
 * @author Sergey Polovko
 */
public interface SchemeRpc extends Rpc {

    /**
     * Make directory.
     * @param request request proto
     * @param settings rpc call settings
     * @return completable future with result of operation
     */
    CompletableFuture<Result<MakeDirectoryResponse>> makeDirectory(MakeDirectoryRequest request,
                                                                   GrpcRequestSettings settings);

    /**
     * Remove directory.
     * @param request request proto
     * @param settings rpc call settings
     * @return completable future with result of operation
     */
    CompletableFuture<Result<RemoveDirectoryResponse>> removeDirectory(RemoveDirectoryRequest request,
                                                                       GrpcRequestSettings settings);

    /**
     * Returns information about given directory and objects inside it.
     * @param request request proto
     * @param settings rpc call settings
     * @return completable future with result of operation
     */
    CompletableFuture<Result<ListDirectoryResponse>> describeDirectory(ListDirectoryRequest request,
                                                                       GrpcRequestSettings settings);

    /**
     * Returns information about object with given path.
     * @param request request proto
     * @param settings rpc call settings
     * @return completable future with result of operation
     */
    CompletableFuture<Result<DescribePathResponse>> describePath(DescribePathRequest request,
                                                                 GrpcRequestSettings settings);

}
