package tech.ydb.scheme.impl;

import java.util.concurrent.CompletableFuture;

import tech.ydb.core.Result;
import tech.ydb.core.Status;
import tech.ydb.core.grpc.GrpcRequestSettings;
import tech.ydb.proto.scheme.SchemeOperationProtos.DescribePathRequest;
import tech.ydb.proto.scheme.SchemeOperationProtos.DescribePathResult;
import tech.ydb.proto.scheme.SchemeOperationProtos.ListDirectoryRequest;
import tech.ydb.proto.scheme.SchemeOperationProtos.ListDirectoryResult;
import tech.ydb.proto.scheme.SchemeOperationProtos.MakeDirectoryRequest;
import tech.ydb.proto.scheme.SchemeOperationProtos.RemoveDirectoryRequest;


/**
 * @author Sergey Polovko
 */
public interface SchemeRpc extends AutoCloseable {

    String getDatabase();

    @Override
    void close();

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
