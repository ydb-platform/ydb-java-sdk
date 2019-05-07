package ru.yandex.ydb.table.rpc;

import java.util.concurrent.CompletableFuture;

import ru.yandex.ydb.core.Result;
import ru.yandex.ydb.core.rpc.Rpc;
import ru.yandex.ydb.scheme.SchemeOperationProtos.DescribePathRequest;
import ru.yandex.ydb.scheme.SchemeOperationProtos.DescribePathResponse;
import ru.yandex.ydb.scheme.SchemeOperationProtos.ListDirectoryRequest;
import ru.yandex.ydb.scheme.SchemeOperationProtos.ListDirectoryResponse;
import ru.yandex.ydb.scheme.SchemeOperationProtos.MakeDirectoryRequest;
import ru.yandex.ydb.scheme.SchemeOperationProtos.MakeDirectoryResponse;
import ru.yandex.ydb.scheme.SchemeOperationProtos.RemoveDirectoryRequest;
import ru.yandex.ydb.scheme.SchemeOperationProtos.RemoveDirectoryResponse;


/**
 * @author Sergey Polovko
 */
public interface SchemeRpc extends Rpc {

    /**
     * Make directory.
     */
    CompletableFuture<Result<MakeDirectoryResponse>> makeDirectory(MakeDirectoryRequest request);

    /**
     * Remove directory.
     */
    CompletableFuture<Result<RemoveDirectoryResponse>> removeDirectory(RemoveDirectoryRequest request);

    /**
     * Returns information about given directory and objects inside it.
     */
    CompletableFuture<Result<ListDirectoryResponse>> describeDirectory(ListDirectoryRequest request);

    /**
     * Returns information about object with given path.
     */
    CompletableFuture<Result<DescribePathResponse>> describePath(DescribePathRequest request);

}
