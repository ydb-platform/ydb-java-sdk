package tech.ydb.topic.impl;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import com.google.protobuf.Message;
import com.google.protobuf.TextFormat;
import org.slf4j.Logger;

import tech.ydb.core.Status;
import tech.ydb.core.StatusCode;
import tech.ydb.core.grpc.GrpcReadWriteStream;

public abstract class TopicStream<R extends Message, W extends Message> {
    private final Logger logger;
    private final String debugId;
    private final GrpcReadWriteStream<R, W> stream;
    private final CompletableFuture<Status> streamStatus = new CompletableFuture<>();
    private volatile String token;

    public TopicStream(Logger logger, String debugId, GrpcReadWriteStream<R, W> stream) {
        this.logger = logger;
        this.debugId = debugId;
        this.stream = stream;
        this.token = stream.authToken();
    }

    protected abstract W updateTokenMessage(String token);
    protected abstract Status parseMessageStatus(R message);

    public CompletableFuture<Status> start(W initReq, Consumer<R> messageHandler) {
        this.logger.debug("[{}] is about to start", debugId);
        this.stream.start((R msg) -> {
            Status messageStatus = parseMessageStatus(msg);
            if (messageStatus.isSuccess()) {
                messageHandler.accept(msg);
            } else {
                logger.warn("[{}] stopped by getting status {}", debugId, messageStatus);
                if (streamStatus.complete(messageStatus)) {
                    stream.close();
                }
            }
        }).whenComplete((st, th) -> {
            Status status = st != null ? st : Status.of(StatusCode.INTERNAL_ERROR, th);
            logger.debug("[{}] finished with status {}", debugId, st);
            streamStatus.complete(status);
        });

        if (!streamStatus.isDone()) {
            stream.sendNext(initReq);
        }

        return streamStatus;
    }

    public void close() {
        logger.debug("[{}] closed by app", debugId);
        if (!streamStatus.isDone()) {
            stream.close();
        }
    }

    public void send(W req) {
        if (streamStatus.isDone()) {
            logger.warn("[{}] is already closed. This message is NOT sent:\n{}", debugId, TextFormat.shortDebugString(req));
            return;
        }

        String currentToken = stream.authToken();
        if (!Objects.equals(token, currentToken)) {
            token = currentToken;
            logger.info("{} sends new token", this);
            stream.sendNext(updateTokenMessage(token));
        }

        stream.sendNext(req);
    }
}
