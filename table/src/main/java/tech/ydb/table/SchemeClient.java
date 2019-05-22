package tech.ydb.table;

import java.util.concurrent.CompletableFuture;

import javax.annotation.WillClose;

import tech.ydb.core.Result;
import tech.ydb.core.Status;
import tech.ydb.table.description.DescribePathResult;
import tech.ydb.table.description.ListDirectoryResult;
import tech.ydb.table.impl.SchemeClientBuilderImpl;
import tech.ydb.table.rpc.SchemeRpc;


/**
 * @author Sergey Polovko
 */
public interface SchemeClient extends AutoCloseable {

    static Builder newClient(@WillClose SchemeRpc schemeRpc) {
        return new SchemeClientBuilderImpl(schemeRpc);
    }

    CompletableFuture<Status> makeDirectory(String path);

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
