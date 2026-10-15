package tech.ydb.topic.impl;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import com.google.protobuf.Message;

import tech.ydb.core.Status;

public interface TopicStream<R extends Message, W extends Message> {
    CompletableFuture<Status> start(Consumer<R> messageHandler);

    default String getFailureReason(Status status) {
        return null;
    }

    void send(W request);
    void close();
}
