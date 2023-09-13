package tech.ydb.topic.read;

import java.util.concurrent.CompletableFuture;

public interface DeferredCommitter {
    void add(Message message);
    CompletableFuture<Void> commit();
}
