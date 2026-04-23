package tech.ydb.topic.impl;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import com.google.protobuf.Message;

import tech.ydb.core.Status;

public interface TopicStream<R extends Message, W extends Message> {
    CompletableFuture<Status> start(W initReq, Consumer<R> messageHandler);
    void send(W request);

    void close();
}
