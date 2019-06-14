package tech.ydb.table.rpc;

import java.util.concurrent.CompletableFuture;

import tech.ydb.core.Result;
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
     */
    CompletableFuture<Result<MakeDirectoryResponse>> makeDirectory(MakeDirectoryRequest request, long deadlineAfter);

    /**
     * Remove directory.
     */
    CompletableFuture<Result<RemoveDirectoryResponse>> removeDirectory(RemoveDirectoryRequest request, long deadlineAfter);

    /**
     * Returns information about given directory and objects inside it.
     */
    CompletableFuture<Result<ListDirectoryResponse>> describeDirectory(ListDirectoryRequest request, long deadlineAfter);

    /**
     * Returns information about object with given path.
     */
    CompletableFuture<Result<DescribePathResponse>> describePath(DescribePathRequest request, long deadlineAfter);

}
