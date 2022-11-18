package tech.ydb.scheme;

import java.util.concurrent.CompletableFuture;

import javax.annotation.WillNotClose;

import tech.ydb.core.Result;
import tech.ydb.core.Status;
import tech.ydb.core.grpc.GrpcTransport;
import tech.ydb.scheme.description.DescribePathResult;
import tech.ydb.scheme.description.ListDirectoryResult;
import tech.ydb.scheme.impl.GrpcSchemeRpc;
import tech.ydb.scheme.impl.SchemeClientImpl;


/**
 * @author Sergey Polovko
 */
public interface SchemeClient extends AutoCloseable {

    static Builder newClient(@WillNotClose GrpcTransport transport) {
        return SchemeClientImpl.newClient(GrpcSchemeRpc.useTransport(transport));
    }

    /**
     * Create single directory.
     *
     * Parent directories must be already present.
     * @param path  path to directory
     * @return operation status
     */
    CompletableFuture<Status> makeDirectory(String path);

    /**
     * Create directory and all its parent directories if they are not present.
     *
     * @param path  path to directory
     * @return operation status
     */
    CompletableFuture<Status> makeDirectories(String path);

    CompletableFuture<Status> removeDirectory(String path);

    CompletableFuture<Result<DescribePathResult>> describePath(String path);

    CompletableFuture<Result<ListDirectoryResult>> listDirectory(String path);

    @Override
    void close();

    /**
     * BUILDER
     */
    interface Builder {

        SchemeClient build();
    }
}
