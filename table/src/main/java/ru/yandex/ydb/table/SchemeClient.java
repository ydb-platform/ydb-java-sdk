package ru.yandex.ydb.table;

import java.util.concurrent.CompletableFuture;

import ru.yandex.ydb.core.Result;
import ru.yandex.ydb.core.Status;
import ru.yandex.ydb.table.description.DescribePathResult;
import ru.yandex.ydb.table.description.ListDirectoryResult;


/**
 * @author Sergey Polovko
 */
public interface SchemeClient {

    CompletableFuture<Status> makeDirectory(String path);

    CompletableFuture<Status> removeDirectory(String path);

    CompletableFuture<Result<DescribePathResult>> describePath(String path);

    CompletableFuture<Result<ListDirectoryResult>> listDirectory(String path);

}
