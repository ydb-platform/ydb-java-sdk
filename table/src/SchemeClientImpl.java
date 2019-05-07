package ru.yandex.ydb.table;

import java.util.concurrent.CompletableFuture;

import ru.yandex.ydb.core.Result;
import ru.yandex.ydb.core.Status;
import ru.yandex.ydb.scheme.SchemeOperationProtos;
import ru.yandex.ydb.scheme.SchemeOperationProtos.DescribePathRequest;
import ru.yandex.ydb.scheme.SchemeOperationProtos.ListDirectoryRequest;
import ru.yandex.ydb.scheme.SchemeOperationProtos.MakeDirectoryRequest;
import ru.yandex.ydb.scheme.SchemeOperationProtos.RemoveDirectoryRequest;
import ru.yandex.ydb.table.description.DescribePathResult;
import ru.yandex.ydb.table.description.ListDirectoryResult;
import ru.yandex.ydb.table.rpc.SchemeRpc;


/**
 * @author Sergey Polovko
 */
final class SchemeClientImpl implements SchemeClient {

    private final SchemeRpc schemeRpc;
    private final OperationsTray operationsTray;

    SchemeClientImpl(SchemeRpc schemeRpc, OperationsTray operationsTray) {
        this.schemeRpc = schemeRpc;
        this.operationsTray = operationsTray;
    }

    @Override
    public CompletableFuture<Status> makeDirectory(String path) {
        MakeDirectoryRequest request = MakeDirectoryRequest.newBuilder()
            .setPath(path)
            .build();
        return schemeRpc.makeDirectory(request)
            .thenCompose(response -> {
                if (!response.isSuccess()) {
                    return CompletableFuture.completedFuture(response.toStatus());
                }
                return operationsTray.waitStatus(response.expect("makeDirectory()").getOperation());
            });
    }

    @Override
    public CompletableFuture<Status> removeDirectory(String path) {
        RemoveDirectoryRequest request = RemoveDirectoryRequest.newBuilder()
            .setPath(path)
            .build();
        return schemeRpc.removeDirectory(request)
            .thenCompose(response -> {
                if (!response.isSuccess()) {
                    return CompletableFuture.completedFuture(response.toStatus());
                }
                return operationsTray.waitStatus(response.expect("removeDirectory()").getOperation());
            });
    }

    @Override
    public CompletableFuture<Result<DescribePathResult>> describePath(String path) {
        DescribePathRequest request = DescribePathRequest.newBuilder()
            .setPath(path)
            .build();
        return schemeRpc.describePath(request)
            .thenCompose(response -> {
                if (!response.isSuccess()) {
                    return CompletableFuture.completedFuture(response.cast());
                }
                return operationsTray.waitResult(
                    response.expect("describePath()").getOperation(),
                    SchemeOperationProtos.DescribePathResult.class,
                    result -> new DescribePathResult(result.getSelf()));
            });
    }

    @Override
    public CompletableFuture<Result<ListDirectoryResult>> listDirectory(String path) {
        ListDirectoryRequest request = ListDirectoryRequest.newBuilder()
            .setPath(path)
            .build();
        return schemeRpc.describeDirectory(request)
            .thenCompose(response -> {
                if (!response.isSuccess()) {
                    return CompletableFuture.completedFuture(response.cast());
                }
                return operationsTray.waitResult(
                    response.expect("describeDirectory()").getOperation(),
                    SchemeOperationProtos.ListDirectoryResult.class,
                    result -> new ListDirectoryResult(result.getSelf(), result.getChildrenList()));
            });
    }
}
