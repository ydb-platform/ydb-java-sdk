package tech.ydb.table.impl;

import java.util.Iterator;
import java.util.concurrent.CompletableFuture;

import tech.ydb.core.Result;
import tech.ydb.core.Status;
import tech.ydb.core.grpc.GrpcRequestSettings;
import tech.ydb.scheme.SchemeOperationProtos;
import tech.ydb.table.SchemeClient;
import tech.ydb.table.description.DescribePathResult;
import tech.ydb.table.description.ListDirectoryResult;
import tech.ydb.table.rpc.SchemeRpc;

import com.google.common.base.Splitter;

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

        String database = schemeRpc.getDatabase();
        if (!database.isEmpty() && path.startsWith(database)) {
            path = path.substring(database.length());
        }

        Iterator<String> it = Splitter.on('/')
            .omitEmptyStrings()
            .split(path)
            .iterator();

        CompletableFuture<Status> future = new CompletableFuture<>();
        mkdirs(database, it, future);
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
        SchemeOperationProtos.MakeDirectoryRequest request = SchemeOperationProtos.MakeDirectoryRequest
                .newBuilder()
                .setPath(path)
                .build();
        final GrpcRequestSettings grpcRequestSettings = GrpcRequestSettings.newBuilder().build();
        return schemeRpc
                .makeDirectory(request, grpcRequestSettings);
    }

    @Override
    public CompletableFuture<Status> removeDirectory(String path) {
        SchemeOperationProtos.RemoveDirectoryRequest request = SchemeOperationProtos.RemoveDirectoryRequest
                .newBuilder()
                .setPath(path)
                .build();
        final GrpcRequestSettings grpcRequestSettings = GrpcRequestSettings.newBuilder().build();
        return schemeRpc.removeDirectory(request, grpcRequestSettings);
    }

    @Override
    public CompletableFuture<Result<DescribePathResult>> describePath(String path) {
        SchemeOperationProtos.DescribePathRequest request = SchemeOperationProtos.DescribePathRequest
                .newBuilder()
                .setPath(path)
                .build();
        final GrpcRequestSettings grpcRequestSettings = GrpcRequestSettings.newBuilder().build();
        return schemeRpc
                .describePath(request, grpcRequestSettings)
                .thenApply(result -> result.map(DescribePathResult::new));
    }

    @Override
    public CompletableFuture<Result<ListDirectoryResult>> listDirectory(String path) {
        SchemeOperationProtos.ListDirectoryRequest request = SchemeOperationProtos.ListDirectoryRequest
                .newBuilder()
                .setPath(path)
                .build();
        final GrpcRequestSettings grpcRequestSettings = GrpcRequestSettings.newBuilder().build();
        return schemeRpc
                .describeDirectory(request, grpcRequestSettings)
                .thenApply(result -> result.map(ListDirectoryResult::new));
    }

    @Override
    public void close() {
        schemeRpc.close();
    }
}
