package tech.ydb.table;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;

import tech.ydb.core.Result;


/**
 * @author Sergey Polovko
 */
public interface SessionSupplier {

    CompletableFuture<Result<Session>> getOrCreateSession(Duration timeout);

}
