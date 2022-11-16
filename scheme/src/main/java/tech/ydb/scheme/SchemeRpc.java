package tech.ydb.scheme;

import java.util.concurrent.CompletableFuture;

import tech.ydb.core.Result;
import tech.ydb.core.Status;
import tech.ydb.core.grpc.GrpcRequestSettings;
import tech.ydb.core.rpc.Rpc;
import tech.ydb.scheme.SchemeOperationProtos.DescribePathRequest;
import tech.ydb.scheme.SchemeOperationProtos.DescribePathResult;
import tech.ydb.scheme.SchemeOperationProtos.ListDirectoryRequest;
import tech.ydb.scheme.SchemeOperationProtos.ListDirectoryResult;
import tech.ydb.scheme.SchemeOperationProtos.MakeDirectoryRequest;
import tech.ydb.scheme.SchemeOperationProtos.RemoveDirectoryRequest;


/**
 * @author Sergey Polovko
 */
public interface SchemeRpc extends Rpc {

    /**
     * Make directory.
     * @param request request proto
     * @param settings rpc call settings
     * @return completable future with status of operation
     */
    CompletableFuture<Status> makeDirectory(MakeDirectoryRequest request, GrpcRequestSettings settings);

    /**
     * Remove directory.
     * @param request request proto
     * @param settings rpc call settings
     * @return completable future with status of operation
     */
    CompletableFuture<Status> removeDirectory(RemoveDirectoryRequest request, GrpcRequestSettings settings);

    /**
     * Returns information about given directory and objects inside it.
     * @param request request proto
     * @param settings rpc call settings
     * @return completable future with result of operation
     */
    CompletableFuture<Result<ListDirectoryResult>> describeDirectory(ListDirectoryRequest request,
            GrpcRequestSettings settings);

    /**
     * Returns information about object with given path.
     * @param request request proto
     * @param settings rpc call settings
     * @return completable future with result of operation
     */
    CompletableFuture<Result<DescribePathResult>> describePath(DescribePathRequest request,
            GrpcRequestSettings settings);

}
