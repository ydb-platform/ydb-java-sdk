package tech.ydb.topic.impl;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;

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

    @Override
    public String toString() {
        return "Stream[" + id + "]";
    }

    protected abstract W updateTokenMessage(String token);
    protected abstract void handleMessage(R message);

    protected void start() {
        logger.info("{} is about to start", this);
        stream.start((R msg) -> {
            handleMessage(msg);
        }).whenComplete((st, th) -> {
            Status status = st != null ? st : Status.of(StatusCode.INTERNAL_ERROR, th);
            logger.info("{} finished with status {}", this, st);
            streamStatus.complete(status);
        });
    }

    public void fail(Status error) {
        logger.warn("{} stopped with problem {}", this, error);
        if (streamStatus.complete(error)) {
            stream.close();
        }
    }

    public void stop() {
        logger.info("{} stop", this);
        if (!streamStatus.isDone()) {
            stream.close();
        }
    }

    public void send(W request) {
        if (streamStatus.isDone()) {
            logger.trace("{} is already closed. This message is NOT sent:\n{}", this, request);
            return;
        }

        String currentToken = stream.authToken();
        if (!Objects.equals(token, currentToken)) {
            token = currentToken;
            logger.info("{} sends new token", id);
            stream.sendNext(updateTokenMessage(token));
        }

        stream.sendNext(request);
    }
}
