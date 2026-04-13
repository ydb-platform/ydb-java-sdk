package tech.ydb.topic.impl;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import org.slf4j.Logger;

import tech.ydb.core.Status;
import tech.ydb.core.StatusCode;
import tech.ydb.core.grpc.GrpcReadWriteStream;

public abstract class TopicStream<R, W> {
    private final Logger logger;
    private final GrpcReadWriteStream<R, W> stream;
    private final String id;
    private final CompletableFuture<Status> streamStatus = new CompletableFuture<>();
    private volatile String token;

    public TopicStream(Logger logger, String id, GrpcReadWriteStream<R, W> stream) {
        this.logger = logger;
        this.id = id;
        this.stream = stream;
        this.token = stream.authToken();
    }

    protected abstract W updateTokenMessage(String token);
    protected abstract Status parseMessageStatus(R message);

    public CompletableFuture<Status> start(W initReq, Consumer<R> messageHandler) {
        this.logger.debug("[{}] is about to start", id);
        this.stream.start((R msg) -> {
            Status messageStatus = parseMessageStatus(msg);
            if (messageStatus.isSuccess()) {
                messageHandler.accept(msg);
            } else {
                logger.warn("[{}] stopped by getting status {}", this, messageStatus);
                if (streamStatus.complete(messageStatus)) {
                    stream.close();
                }
            }
        }).whenComplete((st, th) -> {
            Status status = st != null ? st : Status.of(StatusCode.INTERNAL_ERROR, th);
            logger.debug("{} finished with status {}", id, st);
            streamStatus.complete(status);
        });

        if (!streamStatus.isDone()) {
            stream.sendNext(initReq);
        }

        return streamStatus;
    }

    public void close() {
        logger.info("[{}] closed by app", id);
        if (!streamStatus.isDone()) {
            stream.close();
        }
    }

    public void send(W request) {
        if (streamStatus != null) {
            logger.warn("[{}] is already closed. This message is NOT sent:\n{}", id, request);
            return;
        }

        String currentToken = stream.authToken();
        if (!Objects.equals(token, currentToken)) {
            token = currentToken;
            logger.info("[{}] sends new token", id);
            stream.sendNext(updateTokenMessage(token));
        }

        stream.sendNext(request);
    }
}
