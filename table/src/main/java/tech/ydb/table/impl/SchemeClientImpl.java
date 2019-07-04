package tech.ydb.table.impl;

import java.util.Iterator;
import java.util.concurrent.CompletableFuture;

import com.google.common.base.Splitter;
import tech.ydb.core.Result;
import tech.ydb.core.Status;
import tech.ydb.scheme.SchemeOperationProtos;
import tech.ydb.scheme.SchemeOperationProtos.DescribePathRequest;
import tech.ydb.scheme.SchemeOperationProtos.ListDirectoryRequest;
import tech.ydb.scheme.SchemeOperationProtos.MakeDirectoryRequest;
import tech.ydb.scheme.SchemeOperationProtos.RemoveDirectoryRequest;
import tech.ydb.table.SchemeClient;
import tech.ydb.table.description.DescribePathResult;
import tech.ydb.table.description.ListDirectoryResult;
import tech.ydb.table.rpc.SchemeRpc;

import static java.util.concurrent.CompletableFuture.completedFuture;


/**
 * @author Sergey Polovko
 */
final class SchemeClientImpl implements SchemeClient {

    private final SchemeRpc schemeRpc;

    SchemeClientImpl(SchemeClientBuilderImpl builder) {
        this.schemeRpc = builder.schemeRpc;
    }

    @Override
    public CompletableFuture<Status> makeDirectory(String path) {
        return mkdir(path);
    }

    @Override
    public CompletableFuture<Status> makeDirectories(String path) {
        if (path.lastIndexOf('/') < 1) {
            return mkdir(path);
        }

        Iterator<String> it = Splitter.on('/')
            .omitEmptyStrings()
            .split(path)
            .iterator();

        CompletableFuture<Status> future = new CompletableFuture<>();
        mkdirs("", it, future);
        return future;
    }

    private void mkdirs(String prefix, Iterator<String> it, CompletableFuture<Status> promise) {
        if (!it.hasNext()) {
            promise.complete(Status.SUCCESS);
            return;
        }
        String path = prefix + '/' + it.next();
        mkdir(path).whenComplete((s, e) -> {
            if (e != null) {
                promise.completeExceptionally(e);
            } else if (!s.isSuccess() && !prefix.isEmpty()) { // ignore non success status for root node
                promise.complete(s);
            } else {
                mkdirs(path, it, promise);
            }
        });
    }

    private CompletableFuture<Status> mkdir(String path) {
        MakeDirectoryRequest request = MakeDirectoryRequest.newBuilder()
            .setPath(path)
            .build();
        final long deadlineAfter = 0;
        return schemeRpc.makeDirectory(request, deadlineAfter)
            .thenCompose(response -> {
                if (!response.isSuccess()) {
                    return completedFuture(response.toStatus());
                }
                return schemeRpc.getOperationTray()
                    .waitStatus(response.expect("makeDirectory()").getOperation(), deadlineAfter);
            });
    }

    @Override
    public CompletableFuture<Status> removeDirectory(String path) {
        RemoveDirectoryRequest request = RemoveDirectoryRequest.newBuilder()
            .setPath(path)
            .build();
        final long deadlineAfter = 0;
        return schemeRpc.removeDirectory(request, deadlineAfter)
            .thenCompose(response -> {
                if (!response.isSuccess()) {
                    return completedFuture(response.toStatus());
                }
                return schemeRpc.getOperationTray()
                    .waitStatus(response.expect("removeDirectory()").getOperation(), deadlineAfter);
            });
    }

    @Override
    public CompletableFuture<Result<DescribePathResult>> describePath(String path) {
        DescribePathRequest request = DescribePathRequest.newBuilder()
            .setPath(path)
            .build();
        final long deadlineAfter = 0;
        return schemeRpc.describePath(request, deadlineAfter)
            .thenCompose(response -> {
                if (!response.isSuccess()) {
                    return completedFuture(response.cast());
                }
                return schemeRpc.getOperationTray().waitResult(
                    response.expect("describePath()").getOperation(),
                    SchemeOperationProtos.DescribePathResult.class,
                    result -> new DescribePathResult(result.getSelf()),
                    deadlineAfter);
            });
    }

    @Override
    public CompletableFuture<Result<ListDirectoryResult>> listDirectory(String path) {
        ListDirectoryRequest request = ListDirectoryRequest.newBuilder()
            .setPath(path)
            .build();
        final long deadlineAfter = 0;
        return schemeRpc.describeDirectory(request, deadlineAfter)
            .thenCompose(response -> {
                if (!response.isSuccess()) {
                    return completedFuture(response.cast());
                }
                return schemeRpc.getOperationTray().waitResult(
                    response.expect("describeDirectory()").getOperation(),
                    SchemeOperationProtos.ListDirectoryResult.class,
                    result -> new ListDirectoryResult(result.getSelf(), result.getChildrenList()),
                    deadlineAfter);
            });
    }

    @Override
    public void close() {
        schemeRpc.close();
    }
}
