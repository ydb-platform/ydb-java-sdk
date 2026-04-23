package tech.ydb.topic.impl;


import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import com.google.protobuf.Message;
import org.slf4j.Logger;

import tech.ydb.core.Status;

public class TopicStreamFail<R extends Message, W extends Message> implements TopicStream<R, W> {
    private final Logger logger;
    private final String debugId;

    private final Status status;

    public TopicStreamFail(Logger logger, String debugId, Status status) {
        this.logger = logger;
        this.debugId = debugId;
        this.status = status;
    }

    @Override
    public CompletableFuture<Status> start(W initReq, Consumer<R> messageHandler) {
        return CompletableFuture.completedFuture(status);
    }

    @Override
    public void send(W req) {
        logger.warn("[{}] is failed stream with status {}. Next message with type {} was NOT sent", debugId, status,
                req.getDescriptorForType().getName());
    }

    @Override
    public void close() {
        logger.warn("[{}] is failed stream with status {}. It doesn't need to close", debugId, status);
    }
}
